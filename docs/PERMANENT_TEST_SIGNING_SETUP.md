# Permanent Testing Signing Setup

This runbook creates one permanent testing identity for Girvi Khata. The private keystore must never be committed to Git, uploaded to issues, shared in chat, or stored in the public repository.

## Why this is required

Android only permits an installed app to be updated by another APK signed with the same certificate. A random or regenerated key breaks update continuity. The CI workflow therefore fails closed when the permanent identity is unavailable or does not match the pinned certificate fingerprint.

## 1. Create the keystore offline

Run this on a trusted computer with Java installed. Choose strong unique passwords and preserve them in a password manager.

```bash
mkdir -p girvi-private-signing
cd girvi-private-signing

keytool -genkeypair \
  -keystore girvi-testing.keystore \
  -storetype PKCS12 \
  -storepass 'REPLACE_WITH_STRONG_STORE_PASSWORD' \
  -keypass 'REPLACE_WITH_STRONG_KEY_PASSWORD' \
  -alias girvi-testing \
  -keyalg RSA \
  -keysize 3072 \
  -validity 10000 \
  -dname 'CN=Girvi Khata Testing, O=Girvi Khata, C=IN'
```

Do not reuse a personal password. Do not place the real passwords in shell history on a shared computer.

## 2. Calculate the certificate fingerprint

```bash
keytool -exportcert \
  -keystore girvi-testing.keystore \
  -storepass 'REPLACE_WITH_STRONG_STORE_PASSWORD' \
  -alias girvi-testing \
| sha256sum
```

Copy only the 64-character SHA-256 value. This fingerprint is not the private key.

## 3. Encode the keystore for GitHub Actions

Linux:

```bash
base64 -w 0 girvi-testing.keystore > girvi-testing.keystore.base64
```

macOS:

```bash
base64 < girvi-testing.keystore | tr -d '\n' > girvi-testing.keystore.base64
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('girvi-testing.keystore')) |
  Set-Content -NoNewline girvi-testing.keystore.base64
```

## 4. Add repository Actions secrets

In GitHub open repository Settings, then Secrets and variables, Actions, and create these repository secrets:

| Secret | Value |
|---|---|
| `GIRVI_TEST_KEYSTORE_BASE64` | Complete single-line content of `girvi-testing.keystore.base64` |
| `GIRVI_TEST_STORE_PASSWORD` | Keystore store password |
| `GIRVI_TEST_KEY_PASSWORD` | Key password |
| `GIRVI_TEST_KEY_ALIAS` | `girvi-testing` |
| `GIRVI_TEST_CERT_SHA256` | Lowercase 64-character certificate fingerprint |

Never add these values to source files, workflow YAML, Gradle files, documentation, comments, commits, or pull-request descriptions.

## 5. Preserve offline recovery copies

Keep at least two encrypted offline copies of `girvi-testing.keystore` in separate trusted locations. Record the alias and passwords in a password manager. Losing the key permanently prevents future APKs from updating existing testing installations.

## 6. Verify CI

After all five secrets are present, rerun the Alpha 24 Android Build workflow. A valid run must show:

- Unit tests passed.
- Android compilation passed.
- Signing keystore certificate matched `GIRVI_TEST_CERT_SHA256`.
- Debug testing APK built.
- Final APK certificate matched the same fingerprint.
- `girvi-khata-testing-apk` artifact uploaded.

## 7. Release evidence required before owner testing

Record the following without exposing credentials:

- Git commit SHA.
- Workflow run ID.
- Package name: `com.girvikhata.app.testing`.
- Version code: `24`.
- Version name: `0.24.0-testing`.
- Certificate SHA-256 fingerprint.
- APK SHA-256 checksum.
- Artifact ZIP SHA-256 checksum.

The owner-approved Alpha 21 baseline and `baseline/alpha21-owner-approved` rollback reference must remain unchanged until Alpha 24 completes physical device testing and explicit approval.
