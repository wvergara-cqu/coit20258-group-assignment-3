# GitHub team plan - DRS-Enhanced

**Repository:** https://github.com/wvergara-cqu/coit20258-group-assignment-3

**Unit:** COIT20258 Software Engineering - Assessment 3 (tutor Dr. Meiru Che added as collaborator)

## Team and responsibilities
Work is split into three equal streams; each member owns one tier of the system plus a share of testing
and documentation.

| Member | Student ID | Stream | Responsibilities |
|---|---|---|---|
| Wilson Vergara | 12302173 | Data tier, build and requirements | Repository setup and A2 baseline import; database schema; shared domain model; Maven build and executable jars; unit tests; configuration and README; report: introduction and requirement specification; final packaging and submission |
| Judyta Byjos | 12279582 | Business logic and security | Data access layer; business services including both enhanced features; security (RBAC, hashing, encryption, signatures) and shared protocol; client service facade; integration testing, bug reporting and fixes; report: privacy and security design |
| Soabh Sattar Narejo | 12319572 | Server, client GUI and design documentation | Multi-threaded server; JavaFX controllers, FXML views and styling; remaining client classes; UML diagrams (use case, class, sequence, ERD, architecture); report: design specification, test plan and evidence, NetBeans run instructions with screenshots |

## Work plan

| Phase | Work | Owner |
|---|---|---|
| 1. Setup | Create repository, add team, import selected DRS-Initial (A2) as the baseline | Wilson |
| 2. Data tier | Database schema, domain model, A2 data loader | Wilson |
| | Connection pool and DAOs | Judyta |
| 3. Business tier | Multi-threaded server and dispatcher | Soabh |
| | Services, Enhanced Feature 1 (Public Warnings), Enhanced Feature 2 (Field Task Tracking), security, protocol | Judyta |
| 4. Presentation tier | Controllers, FXML views, client app | Soabh |
| | Client service facade | Judyta |
| 5. Build and test | Maven build, executable jars, unit tests | Wilson |
| | Integration testing on Windows/NetBeans, bug fixes | Judyta |
| | GUI test cases (GT-01 to GT-09) and test evidence | Soabh |
| 6. Documentation | Requirements, README, configuration | Wilson |
| | UML diagrams, design section, test plan, run instructions | Soabh |
| | Security section, bug issues on GitHub | Judyta |
| 7. Submission | Final build, packaging, submission | Wilson |

## Workflow
- Work is committed to `main` in small, single-purpose commits under each member's own account.
- Bugs found in testing are recorded as GitHub Issues and closed by the commit that fixes them.
- Every member runs the full test suite (`mvn test`) before pushing.

## Bugs found and resolved

| Bug | Found / fixed by | Description | Resolution |
|---|---|---|---|
| BUG-01 (High) | Judyta Byjos | Public warnings expired as soon as they were issued when MySQL ran in local time (UTC+10), because timestamps were stored in UTC but compared with MySQL `NOW()` in local time. | JDBC option `forceConnectionTimeZoneToSession=true` so the session uses UTC; all 80 tests pass. |
| BUG-02 (Low) | Judyta Byjos | Header summary counters only refreshed on tab change. | Summary refreshes after every successful action. |

## Evidence for the report
Screenshots of the commit history, Insights > Contributors, and the closed BUG-01 / BUG-02 issues.
