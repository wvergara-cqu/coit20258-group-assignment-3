package drs.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import drs.common.model.Department;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import drs.common.model.Responder;
import drs.common.model.Role;
import drs.common.model.Severity;
import drs.server.db.ReportDAO;
import drs.server.db.ResourceDAO;
import drs.server.db.UserDAO;
import drs.server.security.PasswordHasher;

/**
 * Loads the data collected with DRS-Initial (Assignment 2) into MySQL the first
 * time the server starts: the eight departments, sixteen responders and the
 * disaster reports captured during A2 testing, plus the seed user accounts.
 * Source files are CSV resources under /drs/server/seed/.
 */
public class DataLoader {

    private static final Logger LOG = Logger.getLogger(DataLoader.class.getName());
    private static final String SEED = "/drs/server/seed/";

    private final UserDAO users;
    private final ResourceDAO resources;
    private final ReportDAO reports;

    public DataLoader(UserDAO users, ResourceDAO resources, ReportDAO reports) {
        this.users = users;
        this.resources = resources;
        this.reports = reports;
    }

    /** Loads every seed file whose target table is still empty. */
    public void loadIfEmpty() throws SQLException {
        if (users.count() == 0) {
            int n = 0;
            for (String[] row : readCsv("users.csv")) {
                String salt = PasswordHasher.newSalt();
                users.insert(row[0], row[2], Role.valueOf(row[3]), PasswordHasher.hash(row[1], salt), salt);
                n++;
            }
            LOG.info("Loaded " + n + " user accounts.");
        }
        if (resources.countDepartments() == 0) {
            Map<String, Department> byName = new HashMap<>();
            for (String[] row : readCsv("departments.csv")) {
                Department d = new Department(0, row[0], row[1], row[2], row[3]);
                resources.insertDepartment(d);
                byName.put(d.getName(), d);
            }
            int n = 0;
            for (String[] row : readCsv("responders.csv")) {
                Responder r = new Responder(0, row[0], row[1], row[2], byName.get(row[3]));
                r.setUsername(row.length > 4 && !row[4].isBlank() ? row[4] : null);
                resources.insertResponder(r);
                n++;
            }
            LOG.info("Loaded " + byName.size() + " departments and " + n + " responders from DRS-Initial.");

            int count = 0;
            for (String[] row : readCsv("a2_reports.csv")) {
                DisasterReport r = new DisasterReport(0, DisasterType.valueOf(row[0]), Severity.valueOf(row[1]),
                        row[2], row[3], row[4], row[5]);
                r.setStatus(DisasterStatus.valueOf(row[6]));
                if (!row[7].isBlank()) {
                    r.setPriority(Integer.parseInt(row[7]));
                }
                Timestamp created = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                        .minusHours(Long.parseLong(row[8])));
                reports.insert(r, null, null, created);
                count++;
            }
            LOG.info("Imported " + count + " disaster reports collected with DRS-Initial (Assignment 2).");
        }
    }

    /**
     * Reads a seed CSV (header skipped). Fields are separated by commas; a field
     * may be wrapped in double quotes to contain commas.
     */
    static List<String[]> readCsv(String file) {
        InputStream in = DataLoader.class.getResourceAsStream(SEED + file);
        if (in == null) {
            throw new IllegalStateException("Seed file missing: " + file);
        }
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = br.readLine();     // header
            while ((line = br.readLine()) != null) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    rows.add(parseLine(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }

    static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                fields.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        fields.add(cur.toString().trim());
        return fields.toArray(new String[0]);
    }
}
