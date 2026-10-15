# DRS-Enhanced - Disaster Response System (COIT20258 Assessment 3)

Three-tier, MVC Java application:

| Tier | Technology | Package |
|---|---|---|
| Presentation | JavaFX 21 + FXML (Scene Builder) client | `drs.client` + `src/main/resources/drs/client/view` |
| Business logic | Multi-threaded TCP server (fixed thread pool) | `drs.server`, `drs.server.service`, `drs.server.security` |
| Data | MySQL 8 via JDBC + connection pool | `drs.server.db` |
| Shared | Serializable domain model, protocol, signatures | `drs.common` |

Enhanced features (new in A3):
1. **Public Warnings & Evacuation Advisories** (UC-6) - Advice / Watch and Act / Emergency Warning, pre-filled from the incident, signed, auto-expiring, live public board.
2. **Field Task Tracking** (UC-5) - each dispatched responder gets a task that moves through Dispatched > Acknowledged > En Route > On Scene > Completed (or Declined) with signed, time-stamped history.

## Requirements
- JDK 21 or later, Maven 3.8+ (bundled with NetBeans 21+)
- MySQL Server 8.x running on localhost:3306

## 1. Configure MySQL (do this before running anything)
1. Make sure MySQL Server 8 is installed and running on your machine.
2. Open `src/main/resources/drs/server/server.properties` and set the values to match **your** MySQL
   installation:

   ```properties
   db.host=localhost      # MySQL host
   db.port=3306           # MySQL port (3306 is the default; change it if yours differs, e.g. 3356)
   db.user=root           # your MySQL username
   db.password=root   # your MySQL password  <-- change this
   server.port=5050       # port the DRS server listens on (clients connect to this)
   ```

   The account must be allowed to create databases (e.g. `root`).
   **Do not change** `security.aesKey`: it decrypts the reporter data in the seed data and SQL script.

   This file is used by NetBeans, `mvn exec:java`, `mvn test` (the integration tests read it too) and
   is packaged into the server jar.
3. Save the file. If you later build the jars (Option B below), rebuild with `mvn clean package` after
   any change to this file, because the settings are packaged inside the server jar.

The server **creates the `drs_enhanced` database and all tables itself** on first start and loads the
DRS-Initial (Assignment 2) data, so no manual database setup is needed. Optionally you can load it
manually first:

```bash
mysql -u root -p < database/drs_enhanced.sql
```

If the server stops with `Cannot connect to MySQL ...`, MySQL is not running, or `db.port`, `db.user`
or `db.password` do not match your MySQL installation. Check your login with
`mysql -u root -p -P <port>`; it must work before the server can connect.

## 2. Run in NetBeans
Complete **Section 1** first (MySQL running and `server.properties` updated).

1. File > Open Project > select the `DRS-Enhanced` folder (Maven project).
2. Right-click project > Build with Dependencies (downloads JavaFX, MySQL Connector/J, JUnit).
3. Right-click project > Run Maven > **Run DRS Server** - wait for `listening on port 5050`.
4. Click the green **Run Project** button (or Run Maven > **Run DRS Client**). Start more clients the same way.

## 3. Run from the command line
Complete **Section 1** first (MySQL running and `server.properties` updated). Use **one** of the two options below; in both, start the server first and leave it running.

### Option A - run with Maven (during development)
Runs straight from the compiled classes; no jars are built.
```bash
mvn compile exec:java      # terminal 1: server - wait for "listening on port 5050"
mvn javafx:run             # terminal 2: client (repeat in more terminals for more clients)
```

### Option B - run the packaged jars (submission / no IDE)
```bash
mvn clean package                                # once: compiles, runs tests, builds the jars
java -jar target/DRS-Enhanced-1.0-server.jar     # terminal 1: server
java -jar target/DRS-Enhanced-1.0-client.jar     # terminal 2: client
```
Both jars are self-contained (MySQL driver and JavaFX included), so only Java 21 and MySQL are needed.
Ignore the plain `DRS-Enhanced-1.0.jar`; it does not bundle its libraries.

The client jar contains the JavaFX native libraries for the OS it was built on. To build a client jar
that also runs on Windows and Linux, package with the `all-platforms` profile:
```bash
mvn clean package -Pall-platforms
```

Copies of the two jars are in `dist/` and are run the same way:
```bash
java -jar dist/DRS-Enhanced-1.0-server.jar
java -jar dist/DRS-Enhanced-1.0-client.jar
```

#### Changing the MySQL settings for the jar without rebuilding
The settings inside the jar come from `server.properties` at build time. To override them without
rebuilding, use **either** of these:

1. **A `drs-server.properties` file** in the folder you run the command from. For
   `java -jar dist/DRS-Enhanced-1.0-server.jar` run from the project folder, that means the project
   root (next to `pom.xml`), **not** inside `dist/`. Example:

   ```properties
   db.host=localhost
   db.port=3306
   db.user=root
   db.password=root
   ```

2. **Command-line options**, which override everything else:

   ```bash
   java -Ddb.port=3356 -Ddb.password=password -jar dist/DRS-Enhanced-1.0-server.jar
   ```

`drs-server.properties` only affects the server when it is run from that folder. It is not used by
NetBeans or `mvn test`; for those, edit `src/main/resources/drs/server/server.properties` (Section 1).

## 4. Test
```bash
mvn test
```
59 unit tests always run. 21 end-to-end integration tests (`drs.client.ServerIntegrationTest`) start a real
server on a random port against a temporary `drs_enhanced_test` schema; they are skipped automatically if
MySQL is not reachable with the configured account.

## Seed accounts
| Username | Password | Role |
|---|---|---|
| admin | Admin@2026 | Administrator (everything + Audit Log) |
| controller | Control@2026 | Response Controller |
| controller2 | Control@2026 | Response Controller |
| jsmith, mgarcia, jlee, abrown, mjohnson, cwhite | Field@2026 | Field Responder |
| citizen | Citizen@2026 | Public Citizen |

Citizens can also self-register from the login screen. RSA signing keys are created per user on first
login in `~/.drs-enhanced/keys`.

## Project layout
```
DRS-Enhanced/
  pom.xml  nbactions.xml  README.md
  database/drs_enhanced.sql          create + populate script
  dist/                              copies of the packaged server and client jars
  docs/                              UML diagrams, GitHub task plan
  src/main/java/drs/common/...       shared model, protocol, signatures, validation
  src/main/java/drs/server/...       multi-threaded server, services, security, DAOs
  src/main/java/drs/client/...       JavaFX app, controllers, client service, key store
  src/main/resources/drs/client/view FXML views + styles.css
  src/main/resources/drs/server      server.properties + seed CSV (A2 data)
  src/test/java/...                  JUnit 5 unit and integration tests
```
