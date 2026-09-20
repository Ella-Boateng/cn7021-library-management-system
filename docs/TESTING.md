# Test strategy and results

## Implementation environment

The program is written in Java and uses Swing, java.time and java.nio.file from the standard library. The build uses javac --release 17 and creates an executable JAR. It was compiled and tested with Java 22 on macOS arm64. Java 17 bytecode compatibility is requested by the compiler; execution on other Java versions and operating systems has not been tested.

## Core lending algorithm

- Find the selected book and member.
- Reject requests with no available copy, three existing active loans or the same title already on loan.
- Create a new loan ID and calculate the due date from the injected clock.
- Create and validate a candidate snapshot, then save it.
- Replace the active snapshot only after saving succeeds; refresh the visible tables.

## Verification strategy

SWEBOK distinguishes unit, integration and system testing, and discusses boundary-value techniques (Washizaki, 2026, chapter 5, sections 2 and 3). Here, service tests check lending rules; file tests check reloads and failures; interface checks exercise buttons and dialogs. A fixed clock checks due-date boundaries reproducibly (Oracle, n.d.a). The tests use temporary records separate from working data.

| Evidence | Passed | Failed | What was checked |
| --- | --- | --- | --- |
| automated-tests.txt | 32 | 0 | Rules, dates, validation and storage |
| ui-smoke.txt | 2 | 0 | Three tab renders and book and member search |
| ui-workflow.txt | 9 | 0 | Cancel, invalid form, add, register, issue, return, history, backup and reload |

Recorded execution: 19 September 2026. The evidence files contain the actual test output; the service run includes its timestamp and runtime. All 43 recorded checks passed. These results establish the observed behaviour within the tested scope; production deployment requires further qualification.

## Reproducing the results

Double-click Run Tests.command for the 32 service and storage tests. To repeat the graphical checks, run sh test-ui.sh from the project folder in a desktop session. The graphical tests briefly open their own windows and use temporary records.

The table shows representative observed results. The complete 32 service tests are listed in docs/TESTING.md and implemented in tests/library/LibraryTests.java.

| Test | Input or condition | Expected and observed result |
| --- | --- | --- |
| T04 | Copies 0, -1 or 1000 | Rejected; no invalid book created. |
| T10 | Register the same email with different case | Duplicate rejected. |
| T11 | Issue one available copy on 2026-09-19 | Due 2026-10-03; availability falls by one. |
| T12 | Issue a title with zero copies available | Request rejected; original loan retained. |
| T15 | Member already has three active loans | Fourth loan rejected; stock unchanged. |
| T16–T17 | Return a loan, then return it again | First restores stock; second is rejected. |
| T19 | Check status on due date and next day | On loan on due date; Overdue next day. |
| T21–T22 | Save, reload, return and borrow again | State and history retained; unique ID continues. |
| T24–T25 | Bad header or encoded record | Load rejected without rewriting the original. |
| T28 | Storage throws during a return | In-memory state and availability unchanged. |
| UI05–UI08 | Operate add, register, issue and return forms | The complete lending workflow succeeds. |
| UI10–UI11 | Save backup and reload UI changes | Reloaded state equals the working state. |

## Verification boundaries

The automated suite does not assert column sorting, keyboard navigation, backup overwrite cancellation or contention between two application processes. Platform acceptance testing should cover these behaviours in the intended deployment environment.

## Limits of the evidence

Testing covers a small local dataset. There is no performance benchmark, accessibility audit, network-drive test, power-failure simulation or real-user acceptance sign-off. The failed-save test simulates a storage exception; it does not prove survival of a machine crash during a disk write.

## Complete recorded service results

```text
Built dist/library-management.jar
Library Management System automated test evidence
Execution time: 2026-09-19T23:47:10.949132Z
Java runtime: 22
OS: Mac OS X aarch64
Business date fixed at 2026-09-19 for reproducible date assertions.
T01 | PASS | New store starts empty
T02 | PASS | Add book trims fields and assigns ID
T03 | PASS | Reject blank title and author
T04 | PASS | Reject invalid copy counts
T05 | PASS | Reject duplicate book regardless of case
T06 | PASS | Search title author and ID without case sensitivity
T07 | PASS | Add copies updates availability with bounds
T08 | PASS | Register member and normalise email
T09 | PASS | Reject blank name and malformed email
T10 | PASS | Reject duplicate email regardless of case
T11 | PASS | Issue loan sets due date and reduces availability
T12 | PASS | Reject lending unavailable book
T13 | PASS | Reject unknown book and member
T14 | PASS | Reject a second active loan of the same title
T15 | PASS | Enforce three active loans per member
T16 | PASS | Return restores stock and preserves history
T17 | PASS | Reject repeated return and unknown loan
T18 | PASS | Returned title can be borrowed again with a new ID
T19 | PASS | Overdue starts the day after due date
T20 | PASS | Returned loan stays returned after due date
T21 | PASS | Persist and reload books members and active loans
T22 | PASS | Reload return history and continue unique IDs
T23 | PASS | Unicode punctuation and accents round trip
T24 | PASS | Reject corrupt header without rewriting file
T25 | PASS | Reject invalid encoded record
T26 | PASS | Reject orphan loan reference
T27 | PASS | Reject duplicate persisted IDs
T28 | PASS | Failed save leaves service state unchanged
T29 | PASS | Reject control characters and oversized text
T30 | PASS | Snapshot lists cannot be mutated by callers
T31 | PASS | Reject return when system date is before issue date
T32 | PASS | Backup copy reloads to the identical state
RESULT: 32 passed; 0 failed; 32 total.
```

## UI evidence

```text
UI01 | PASS | All three tabs render with realistic fictional records
UI02 | PASS | Book and member searches filter visible tables by text and ID
2026-09-19 23:48:38.312 java[71068:5559456] +[IMKClient subclass]: chose IMKClient_Modern
2026-09-19 23:48:38.312 java[71068:5559456] +[IMKInputSession subclass]: chose IMKInputSession_Modern
PASS | UI03 Cancel leaves catalogue unchanged
PASS | UI04 Blank book form is rejected
PASS | UI05 Book form saves a new book
PASS | UI06 Member form saves registration
PASS | UI07 Issue dialog creates a loan
PASS | UI08 Return confirmation restores availability
PASS | UI09 History filter shows returned loan
PASS | UI10 Backup button creates a restorable copy
PASS | UI11 UI changes survive reloading the store
RESULT: 9 UI workflow checks passed; 0 failed.
```
