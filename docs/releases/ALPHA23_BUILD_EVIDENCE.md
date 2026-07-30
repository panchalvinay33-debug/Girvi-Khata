# Alpha 23 Build Evidence

Date: 2026-07-30

## Source and CI

- Exact source commit: `07a1950a6baf9a6e21971102647e04bd2db6b500`
- Version code/name: `23` / `0.23.0-testing`
- Testing package: `com.girvikhata.app.testing`
- Android workflow: `30549958870` — SUCCESS
- Security Guard: `30549959181` — SUCCESS
- APK artifact ID: `8762478179`
- Artifact ZIP size: `19,580,301 bytes`
- Artifact digest: `sha256:0b4d5f67eb434eb40ed97469809a34a7774b1b2bacaf05163a7cfcfe0403f298`

## Verified by CI

- Unit tests passed.
- Android/Compose compilation passed.
- Stable testing signing passed.
- APK artifact upload passed.

## Local verification status

The artifact was downloaded, but the local container runtime returned an infrastructure error before APK extraction, archive test, byte size and APK SHA-256 could be completed. No APK checksum is claimed in this document. The artifact must not be promoted as the owner testing candidate until those checks are completed.

## Governance

- Alpha 22 exact testing candidate remains `candidate/alpha22-testing`.
- Alpha 23 is isolated in draft PR #3.
- Owner-approved Alpha 21 `main` and permanent rollback branch remain untouched.
- Relational cutover remains blocked.
