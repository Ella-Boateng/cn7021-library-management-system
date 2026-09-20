# Maintenance and evaluation

## Maintenance plan

SWEBOK distinguishes corrective, adaptive, preventive, additive and perfective maintenance (Washizaki, 2026, chapter 7, section 1.6). For this application, corrective work fixes a reproducible defect and adds a regression test; adaptive work checks Java or operating-system changes; preventive work addresses latent faults; perfective work improves existing performance or maintainability. New reservations or renewal features would be additive maintenance. Backup and recovery are addressed separately as operational procedures.

## Backup and recovery

Use Back up records after a working session and before upgrades. Keep backups in a different folder or storage device. To restore, close every library window, retain a copy of the current data/library.db, and place the chosen backup at that exact path. Restart and compare book, member and loan counts. Test a restored copy before replacing important records. A project ZIP is a code-delivery archive, not an ongoing backup of live records.

## Risks and future changes

The main risks are incorrect computer dates, loss of a local file, changes to lending policy and growth beyond a single workstation. The current controls are date checks, explicit backups, centrally defined loan rules and clear deployment limits. Changing the 14-day policy requires a data migration design because existing due dates are validated against that rule. Multiuser access would require database transactions and authentication.

## Evaluation

The implemented scope demonstrates the SDLC through traceable requirements, a layered design, a working Java application and repeatable verification. The 43 recorded checks passed. Maintenance is supported by documented recovery procedures, regression tests and a plan for future changes. The resulting application fulfils the defined catalogue, membership and lending requirements within the stated single-user scope.
