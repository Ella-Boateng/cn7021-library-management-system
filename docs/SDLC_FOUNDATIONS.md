# SDLC foundations and learning

## Understanding the life cycle

The SDLC links activities and their outputs so that development follows an explicit process rather than a sequence of isolated coding tasks. IBM describes seven stages from planning to maintenance and notes that the way they are organised depends on the chosen process model (Jackson, Holdsworth and Kosinski, n.d.). In this project, the requirements explain what the library must do, the design explains how it is organised, and tests provide evidence of its behaviour.

## Choosing an approach for this project

A sequential waterfall process offers defined stages when requirements are stable, while iterative approaches allow repeated refinement (Jackson, Holdsworth and Kosinski, n.d.). The library project uses a small, documented scope and phased deliverables with feedback from testing. It is not presented as a Scrum project because no sprints, product owner or Scrum events were used. This choice keeps the process proportionate to a single-workstation application.

## Connecting requirements to evidence

NASA (2022, sections 3.1 and 3.12) treats life-cycle planning as spanning the whole software effort and requires traceability among relevant software work products. The library report applies that principle by linking each functional requirement to an acceptance criterion and evidence. For example, FR06 requires a return to restore availability once; tests T16 and T17 check both the successful return and rejection of a second return.

## Lessons demonstrated by the implementation

Writing down the borrowing policy makes the limit of three loans and the 14-day period explicit. Keeping LibraryService separate from LibraryFrame makes those rules testable without clicking the interface. The failed-save test demonstrates why a change should not become visible until storage succeeds. The due-date boundary test demonstrates why testing only a successful borrowing example would leave important behaviour unchecked.

## Deployment and maintenance within the project scope

The application is deployed locally through an executable Java archive and a launcher. GitHub distributes source code and evidence; it does not host a running web application. Maintenance is supported by a recovery procedure and regression tests. Long-term use by real library staff remains outside the recorded evaluation. This distinction separates an implemented academic prototype from a production service.

# References

Arakkal, D. (n.d.) Using the Library. Lancaster University Ghana. Available at:

[https://lancaster.edu.gh/library/using-the-library/](https://lancaster.edu.gh/library/using-the-library/)

CN7021 (n.d.) WEEK 10 Topic Overview. Advanced Software Engineering, Experiment III, p. 6. Course handout.

Jackson, G., Holdsworth, J. and Kosinski, M. (n.d.) What is the software development life cycle (SDLC)? IBM. Available at:

[https://www.ibm.com/think/topics/sdlc](https://www.ibm.com/think/topics/sdlc)

NASA (2022) NASA Software Engineering Requirements. NPR 7150.2D, chapter 3, sections 3.1 and 3.12. Effective 8 March 2022. Available at:

[NASA Software Engineering Requirements Chapter 3](https://nodis3.gsfc.nasa.gov/displayDir.cfm?Internal_ID=N_PR_7150_002D_&page_name=Chapter3)

Oracle (n.d.a) Clock. Java Platform Standard Edition 17 API Specification. Available at:

[https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Clock.html](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Clock.html)

Oracle (n.d.b) Files. Java Platform Standard Edition 17 API Specification. Available at:

[https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html)

Oracle (n.d.c) javax.swing. Java Platform Standard Edition 17 API Specification. Available at:

[https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/package-summary.html](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/package-summary.html)

Washizaki, H. (ed.) (2026) Guide to the Software Engineering Body of Knowledge. Version 4.0a, released August 2026. IEEE Computer Society. Chapters 1, 3, 5 and 7. Available at:

[https://ieeecs-media.computer.org/media/education/swebok/swebok-v4.pdf](https://ieeecs-media.computer.org/media/education/swebok/swebok-v4.pdf)

All online sources accessed 20 September 2026. The course handout supplies the assignment context; the SDLC discussion is supported by IBM, NASA and SWEBOK, while the Oracle references support Java implementation details.
