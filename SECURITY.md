# Security Policy

## Non-negotiable rules

- No developer master password, universal recovery PIN, hidden admin account, or backdoor.
- No real customer data or production backups in GitHub issues, commits, tests, screenshots, or logs.
- No advertising, session replay, screen recording, or behavioral tracking SDK.
- No secrets in source code or Android resources.
- Use minimum Google authorization scopes.
- Encrypt sensitive local data and every cloud backup before upload.
- Never log customer names, phone numbers, addresses, IDs, amounts, photo paths, tokens, keys, or passphrases.
- Production financial entries use audit-preserving reversal, not silent deletion.

## Reporting a vulnerability

Do not create a public issue containing exploit details, credentials, real records, or backup samples. Contact the repository owner privately and provide only sanitized reproduction data.

## Repository note

The repository is currently public. Before adding OAuth configuration, release signing, or production deployment material, repository visibility should be changed to Private and GitHub secret scanning/branch protection should be enabled.
