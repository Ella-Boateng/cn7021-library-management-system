# Project planning and requirements analysis

## Problem and proposed system

A small library needs a consistent way to record its catalogue, readers and lending activity. In the proposed scenario, staff use paper records and must manually count copies and check due dates. The application centralises those records, calculates availability from active loans and flags overdue items. The analysis uses a hypothetical small-library scenario.

## Stakeholders and elicitation

The librarian is the direct user. Members are the people whose borrowing is recorded; they do not log into the application. Requirements were derived through scenario analysis of catalogue management, registration, lending and returns. The elicitation method is scenario analysis; field interviews and surveys are outside the scope of this study.

Before deployment in a real library, a workshop with the librarian should confirm the lending period, borrowing limit, information collected, backup responsibility and handling of lost books. A short prototype demonstration should then check that the screen flow fits the librarian’s work.

## Development approach

SDLC activities connect planning, analysis, design, coding, testing, deployment and maintenance (Jackson, Holdsworth and Kosinski, n.d.). This project uses a phased approach with feedback: the requirements define a small lending system; design separates the screens, rules and storage; Java implements those responsibilities; tests check normal and exceptional behaviour. Local installation and maintenance procedures complete the documented cycle. Verification findings can feed back into the design and implementation.

| Phase | Output | Completion evidence |
| --- | --- | --- |
| Requirements | SRS and acceptance criteria | Sections below and docs/SRS.md |
| Design | Architecture and data relationships | Diagrams and docs/DESIGN.md |
| Implementation | Java application and launcher | src, dist and build.sh |
| Testing | Repeatable tests and recorded results | tests and evidence folders |
| Maintenance | Backup, recovery and change plan | docs/MAINTENANCE.md |

NASA (2022, sections 3.1.5 and 3.12) emphasises defined acceptance criteria and links between requirements and verification. Here, FR01 to FR10 are mapped to observable behaviour and test evidence in the SRS. This is a practical use of traceability in a small academic project.

# Software Requirements Specification

The system serves one librarian on one computer. The working records are kept in a local file. Each requirement below describes observable behaviour; the evidence column connects it to a test or an explicit review step. Acceptance criteria describe how a requirement will be judged (Washizaki, 2026, chapter 1, section 4.3).

| ID | Function | Acceptance criterion | Evidence |
| --- | --- | --- | --- |
| FR01 | Catalogue books | Add title, author and 1 to 999 copies; assign a unique book ID. | T02–T05 |
| FR02 | Find books | Search title, author or ID without case sensitivity; show available copies. | T06, UI02 |
| FR03 | Increase stock | Add copies to an existing title without exceeding 999 total. | T07 |
| FR04 | Register members | Store name and a unique email; assign a unique member ID. | T08–T10 |
| FR05 | Issue books | Create a dated loan only when stock and borrowing rules allow it. | T11–T15 |
| FR06 | Return books | Record one return date and restore availability exactly once. | T16–T18 |
| FR07 | Track loans | Show active and returned loans, due dates and overdue status. | T19–T20, UI09 |
| FR08 | Save records | Automatically save successful changes and reload them on startup. | T21–T28 |
| FR09 | Back up records | Let the librarian save a separate restorable copy of the data. | T32, UI10 |
| FR10 | Find members | Filter members by ID, name or email in the Members tab. | UI02 |

## External interfaces

The user works through three tabs: Books, Members, and Loans and returns. Forms collect new records; tables display searchable or sortable results. A file chooser saves backups. The application has no external service, network dependency, barcode scanner or email delivery integration.

## Out of scope

Version 1 does not include passwords, member self service, fines, reservations, renewals, book deletion, catalogue editing, individual copy barcodes or simultaneous users. These exclusions keep the project focused on demonstrating the complete SDLC with a small working system.

# Quality requirements and business rules

| ID | Requirement | Verification or limit |
| --- | --- | --- |
| NFR01 | Run offline with the standard Java library. | Build targets Java 17; tested on Java 22 on macOS arm64. |
| NFR02 | Keep stored relationships and availability consistent. | Validate snapshots, foreign references and stock before saving and after loading. |
| NFR03 | Keep changes recoverable if a save fails. | Replace the whole snapshot atomically; preserve in-memory state on a failed save. |
| NFR04 | Separate presentation, rules and storage. | LibraryFrame, LibraryService and LibraryStore define the boundaries. |
| NFR05 | Explain rejected input without losing entered form values. | Warnings identify the issue; the same form reopens for correction. |
| NFR06 | Prevent two normal application instances sharing one data directory. | Main holds an operating-system file lock until exit. The recorded automated suite does not verify process-lock contention. |

## Business rules

- A loan lasts 14 calendar days. The due date itself is on time; overdue begins the following day.
- A member may hold at most three active loans and only one active loan for a particular title.
- A title may have 1 to 999 copies. Available copies equal total copies minus active loans for that title.
- A return can be recorded only once, on or after the issue date. Returned loans remain in history.
- Book title and author pairs must be unique without case sensitivity. Member emails must also be unique without case sensitivity.
- Names, titles and authors are required, limited to 200 characters, and cannot contain control characters. Email format validation is basic and does not verify ownership.

## Operational assumptions

The library uses a trusted local account and the computer’s calendar date. There is no application authentication or encryption. Base64 is used only to encode text fields safely; it is not a privacy control. Large catalogues, network drives, power-loss durability and multiuser operation have not been qualified.

## Glossary

SRS means Software Requirements Specification. A catalogue entry represents a title and its copy count. An active loan has no return date. A snapshot is the complete set of books, members and loans saved together.

# Use cases and acceptance scenarios

## UC01 Register a book and a member

Actor: librarian. Preconditions: the application is open. The librarian selects Add book, enters title, author and copies, and confirms. The application validates the fields, assigns an ID and saves the book. In Members, the librarian registers a name and email. Successful records appear in their tables. Blank fields, duplicate books, duplicate emails and invalid copy counts produce warnings. Cancel leaves records unchanged.

## UC02 Issue a book

Actor: librarian. Preconditions: at least one member and one available book exist. The librarian selects a book and opens Issue selected book, chooses the member and confirms. The system verifies that both records exist, a copy is available, the member has fewer than three active loans and the member does not already hold the same title. It creates a loan with today’s date and a due date 14 days later, saves it and refreshes availability.

Exception paths: unavailable stock, an unknown record, a duplicate active title or an exceeded borrowing limit rejects the request without creating a loan. A storage failure displays an error and keeps the previous state. Postcondition on success: one new active loan exists and availability decreases by one.

## UC03 Return a book

Actor: librarian. Preconditions: an active loan exists. The librarian selects it in Loans and returns and confirms Return selected loan. The system records today’s date, saves the loan history and recalculates availability. An unknown loan, an already returned loan or a computer date before the issue date is rejected. Postcondition on success: the loan remains in history and one copy becomes available.

## UC04 Find records and inspect lending

The librarian types part of a title, author or ID in Books, or a member name, email or ID in Members. Matching rows update as text is entered. In Loans and returns, Active loans only hides completed loans. Clearing that option displays the retained history. Column headings sort the displayed rows.

## UC05 Back up and recover records

The librarian chooses Back up records, selects a destination and confirms any overwrite. Recovery is an offline procedure: close the application, preserve the current file, replace data/library.db with a known good backup and restart. The application validates restored data before opening the library.
