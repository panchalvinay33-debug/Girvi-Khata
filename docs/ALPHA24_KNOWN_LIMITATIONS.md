# Alpha 24 known limitations and release gates

## Signing

The testing APK cannot be produced until issue #6 provisions one permanent signing identity. Random or regenerated signing is forbidden.

## Cross-store restore atomicity

Business records and the portable master catalog are stored in separate encrypted stores. Alpha 24 verifies each store after restore and creates a pre-restore encrypted safety backup, but a process termination in the narrow interval between the business snapshot commit and master-catalog save can leave the two stores from different restore generations.

Release treatment:

- keep relational source-of-truth cutover blocked;
- perform restore testing from a fresh Alpha 21 backup during issue #7;
- after restore, verify business counts and the complete master catalog;
- kill the process and relaunch before approval;
- retain the pre-restore `.gkb` safety backup until owner approval;
- do not describe portable restore as fully cross-store atomic in Alpha 24.

A future milestone should introduce a shared restore-generation marker or one coordinated encrypted bundle transaction covering both business records and masters.

## Physical-device evidence

Automated unit tests and Android compilation do not replace owner phone validation. PR #5 remains draft until issue #7 is complete and explicit owner approval is recorded.

## Relational cutover

The encrypted snapshot remains authoritative. Relational cutover requires at least 25 successful coordinated writes, no unresolved write intent, no recovery-required state, matched dual-read fingerprints, and explicit owner approval.
