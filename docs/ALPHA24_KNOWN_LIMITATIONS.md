# Alpha 24 known limitations and release gates

## Signing

The testing APK cannot be produced until issue #6 provisions one permanent signing identity. Random or regenerated signing is forbidden.

## Cross-store restore recovery

Alpha 24 now coordinates business records and the portable master catalog through one restore generation protocol:

- target business data and target masters are staged together in an app-private encrypted bundle;
- metadata-only generation intent is persisted before either authoritative store changes;
- the business snapshot activates through the verified write coordinator and relational proof;
- portable masters activate only after business proof is complete;
- startup deterministically completes an unambiguous interrupted generation;
- unknown, mismatched or corrupted generations block normal business writes;
- legacy backups preserve the current master catalog as the explicit generation target;
- staged data and generation metadata are removed only after both target fingerprints verify.

The previous known gap where process death between business and master saves could silently expose mixed generations is therefore closed in code. Release approval still requires issue #7 physical-device interruption testing, including process kill and relaunch during restore, verification of both stores, and retention of the pre-restore `.gkb` safety backup until owner approval.

## Physical-device evidence

Automated unit tests and Android compilation do not replace owner phone validation. PR #5 remains draft until issue #7 is complete and explicit owner approval is recorded.

## Relational cutover

The encrypted snapshot remains authoritative. Relational cutover requires at least 25 successful coordinated writes, no unresolved write intent, no recovery-required state, matched dual-read fingerprints, and explicit owner approval.
