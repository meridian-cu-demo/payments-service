# payments-service

External transfers and bill payments for Meridian members. Owned by **Member Experience**.

**Mock system:** all state is in-memory (per §3.1 this must be, and is, labeled) — no
database, no persistence, no real money movement. Identity is mocked with an `X-Member-Id`
header; real authentication is tracked against handbook §5.1.

## API

Contract of record: `api-contracts/openapi/payments.yaml` (§2.4 — keep them in sync).

- `POST /transfers` — external transfer, subject to the member daily limit (§5.4)
- `GET /transfers/{transferId}`
- `POST /bill-payments`

## Talks to

- `accounts-service` (balance checks) via `accounts.base-url`
- `notifications-service` (member confirmations, §4.5) via `notifications.base-url`

## Run

```bash
mvn spring-boot:run   # port 8082
```

Requires `banking-commons` 1.2.0 in the local repository (clone at tag `v1.2.0`,
`mvn install` — CI does exactly this).
