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

## 1. Configure MySQL
Edit `src/main/resources/drs/server/server.properties` and set `db.user` / `db.password`
(default `root` / `root`). The server **creates the database and all tables itself** on first start
and loads the DRS-Initial (Assignment 2) data. Optionally you can load it manually first:

```bash
mysql -u root -p < database/drs_enhanced.sql
```

## 2. Run in NetBeans
1. File > Open Project > select the `DRS-Enhanced` folder (Maven project).
2. Right-click project > Build with Dependencies (downloads JavaFX, MySQL Connector/J, JUnit).
3. Right-click project > Run Maven > **Run DRS Server** - wait for `listening on port 5050`.
4. Click the green **Run Project** button (or Run Maven > **Run DRS Client**). Start more clients the same way.

## 3. Run from the command line
```bash
mvn clean package          # compiles, runs tests, builds target/*-server.jar and *-client.jar
mvn exec:java              # server
mvn javafx:run             # client (new terminal)
java -jar target/DRS-Enhanced-1.0-server.jar
java -jar target/DRS-Enhanced-1.0-client.jar
```
Pre-built jars are also in `dist/` (`run-server.sh|bat`, `run-client.sh|bat`; the client script needs `JAVAFX_HOME` set to a JavaFX 21 SDK).

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
  dist/                              pre-built executable jars and run scripts
  docs/                              UML diagrams, GitHub task plan
  src/main/java/drs/common/...       shared model, protocol, signatures, validation
  src/main/java/drs/server/...       multi-threaded server, services, security, DAOs
  src/main/java/drs/client/...       JavaFX app, controllers, client service, key store
  src/main/resources/drs/client/view FXML views + styles.css
  src/main/resources/drs/server      server.properties + seed CSV (A2 data)
  src/test/java/...                  JUnit 5 unit and integration tests
```
