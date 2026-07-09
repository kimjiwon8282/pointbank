USE banking_db;

-- Banking Service keeps external API ownership.
-- Money source data moved to ledger_db and is owned by Ledger Service.
-- Future banking-owned data can live here, e.g. user settings, transfer limits,
-- favorite accounts, and education API logs.
