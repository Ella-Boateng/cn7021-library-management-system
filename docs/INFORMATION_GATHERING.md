# Information gathering and requirements evidence

## Method and source

On 20 September 2026, I reviewed Lancaster University Ghana’s published Using the Library guidance to evaluate the initial scenario-based requirements. The page is attributed to Deeraj Arakkal and was published on the university’s website. This is secondary research through document analysis. It supports a hypothetical library case study; it does not constitute an interview, an institutional commission or stakeholder approval.

## Findings and requirements decisions

The guidance permits students to borrow three books for two weeks. It also describes due-date statements, overdue highlighting, renewals and requests or recalls (Arakkal, n.d.). The comparison below distinguishes the published practice from the choices made for this prototype.

| Published practice | Project decision | SRS link |
| --- | --- | --- |
| Three books for two weeks. | Retain a maximum of three active loans and a 14-calendar-day loan period. | FR05 |
| Account statements show loans and due dates; overdue items are highlighted. | Display active loans, due dates and overdue status in the librarian’s interface. Email statements are outside scope. | FR07 |
| Users return borrowed items on time. | Record returns, restore availability and retain loan history. | FR06 |
| Renewals, requests and recalls support shared access. | Record these as possible extensions. Version 1 does not implement them. | Scope boundary |

## Interpretation and limitations

The source corroborates the existing three-loan and 14-day choices; it was consulted during the report review, after initial implementation. It does not establish universal library rules. The prototype’s one-active-loan-per-title rule, unique member email, copy-count ceiling and local storage design remain project assumptions. Return-history retention is also a project design decision, not a claim about the source’s internal system.

The published service is larger than this single-user prototype. Its automated messages, recalls and renewal workflow are not reproduced. Before real deployment, a librarian would need to validate the selected policies, required member details, exception handling and acceptance criteria.

## Source record

Arakkal, D. (n.d.) Using the Library. Lancaster University Ghana. Sections Borrowing and Renewals, Requests and Recalls, and Weekly account statement. Accessed 20 September 2026.

[Lancaster University Ghana library guidance](https://lancaster.edu.gh/library/using-the-library/)
