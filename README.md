# Library Management System

CN7021 Advanced Software Engineering, Experiment III

A single-user Java desktop application for catalogue management, member registration, lending, returns and loan history.

## Requirements

Java Development Kit (JDK) 17 or later and a graphical desktop session. Verified with Java 22 on macOS arm64. The application uses the Java standard library and runs offline without external dependencies or a database server.

## Run

Download this repository using Code > Download ZIP and extract it, or clone the repository, before running the application. On macOS, double-click `Start Library.command`. The launcher compiles the source and starts the application. Keep the Terminal window open while it runs.

Alternatively, from the project directory:

```sh
sh build.sh
java -jar dist/library-management.jar data
```

## Test

```sh
sh test.sh
sh test-ui.sh
```

The first command runs the 32 service and storage tests. The second runs graphical checks in a desktop session. Tests use temporary records rather than the working data folder. Recorded results and screenshots are in `evidence/`.

## Project contents

| Location | Contents |
| --- | --- |
| docs/SDLC_FOUNDATIONS.md | SDLC learning and references |
| docs/INFORMATION_GATHERING.md | Published library policy and requirements decisions |
| docs/MILESTONES.md | Dated deliverables and release evidence |
| docs/SRS.md | Requirements, business rules and use cases |
| docs/DESIGN.md | Architecture, data model and diagram sources |
| docs/TESTING.md | Verification strategy, coverage and recorded results |
| docs/USER_GUIDE.md | Application operation |
| docs/MAINTENANCE.md | Backup, recovery and maintenance plan |
| src/library/ | Java source |
| tests/library/ | Reproducible tests |
| dist/library-management.jar | Executable Java archive |
| evidence/ | Test logs and screenshots |
| data/ | Working records created by the application |

## Lending rules

Loans last 14 calendar days. Each member may hold three active loans, with at most one loan per title. A title may have 1 to 999 copies. Availability is derived from active loans. Duplicate title/author pairs and duplicate member emails are rejected without case sensitivity. Completed loans remain in history.

## Storage and recovery

`data/library.db` is a versioned UTF-8 text snapshot. Successful changes are saved automatically. The application uses a file lock to prevent simultaneous normal instances sharing the same data directory.

Use **Back up records** to save a separate copy. To restore, close the application, preserve the current working file, replace `data/library.db` with a valid backup and restart. See `docs/MAINTENANCE.md` for the recovery procedure.

## Scope

The system is intended for a trusted local user. It does not provide authentication, encryption, reservations, renewals, fines or multiuser transactions. Technical assumptions and verification limits are documented in the report.
