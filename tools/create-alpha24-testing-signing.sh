#!/usr/bin/env bash
set -euo pipefail

umask 077

OUTPUT_DIR="${1:-girvi-private-signing}"
KEYSTORE="$OUTPUT_DIR/girvi-testing.keystore"
BASE64_FILE="$OUTPUT_DIR/girvi-testing.keystore.base64"
FINGERPRINT_FILE="$OUTPUT_DIR/girvi-testing.cert.sha256"
ALIAS="girvi-testing"

command -v keytool >/dev/null 2>&1 || {
  echo "Java keytool is required. Install a JDK and retry." >&2
  exit 1
}
command -v base64 >/dev/null 2>&1 || {
  echo "base64 command is required." >&2
  exit 1
}
command -v sha256sum >/dev/null 2>&1 || {
  echo "sha256sum command is required." >&2
  exit 1
}

if [[ -e "$KEYSTORE" || -e "$BASE64_FILE" || -e "$FINGERPRINT_FILE" ]]; then
  echo "Refusing to overwrite existing signing material in $OUTPUT_DIR" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
chmod 700 "$OUTPUT_DIR"

read -r -s -p "Create a strong keystore password: " STORE_PASSWORD
echo
read -r -s -p "Repeat keystore password: " STORE_PASSWORD_CONFIRM
echo
[[ "$STORE_PASSWORD" == "$STORE_PASSWORD_CONFIRM" ]] || {
  echo "Passwords do not match." >&2
  exit 1
}
[[ ${#STORE_PASSWORD} -ge 16 ]] || {
  echo "Use at least 16 characters." >&2
  exit 1
}

keytool -genkeypair \
  -keystore "$KEYSTORE" \
  -storetype PKCS12 \
  -storepass "$STORE_PASSWORD" \
  -keypass "$STORE_PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 3072 \
  -validity 10000 \
  -dname "CN=Girvi Khata Testing, O=Girvi Khata, C=IN"

keytool -exportcert \
  -keystore "$KEYSTORE" \
  -storepass "$STORE_PASSWORD" \
  -alias "$ALIAS" \
| sha256sum | awk '{print tolower($1)}' > "$FINGERPRINT_FILE"

base64 -w 0 "$KEYSTORE" > "$BASE64_FILE"
chmod 600 "$KEYSTORE" "$BASE64_FILE" "$FINGERPRINT_FILE"

unset STORE_PASSWORD STORE_PASSWORD_CONFIRM

cat <<EOF

Signing material created in: $OUTPUT_DIR

Add these GitHub Actions repository secrets manually:
- GIRVI_TEST_KEYSTORE_BASE64 = complete single line from $BASE64_FILE
- GIRVI_TEST_STORE_PASSWORD = the password you entered
- GIRVI_TEST_KEY_PASSWORD = the same password
- GIRVI_TEST_KEY_ALIAS = $ALIAS
- GIRVI_TEST_CERT_SHA256 = value from $FINGERPRINT_FILE

Never commit, upload, email, or paste the keystore/password into chat or an issue.
Keep two encrypted offline copies of the keystore.
EOF
