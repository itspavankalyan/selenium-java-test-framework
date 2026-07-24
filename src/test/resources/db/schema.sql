-- Schema for the local H2 "audit" store used by the DB validation layer.
--
-- This table plays the role of a downstream system of record that a real
-- pipeline might reconcile against an upstream API (e.g. a data warehouse
-- sync, or an internal ledger service). restful-booker is a public practice
-- API and — deliberately, since it's a shared demo service — does not expose
-- its own backing database for direct inspection. Rather than fake the DB
-- validation layer, this framework owns a real local database and performs
-- genuine JDBC reads/writes/assertions against it, keyed by the booking id
-- the API returns.
CREATE TABLE IF NOT EXISTS booking_audit (
    booking_id       INT PRIMARY KEY,
    firstname        VARCHAR(255) NOT NULL,
    lastname         VARCHAR(255) NOT NULL,
    total_price      INT NOT NULL,
    deposit_paid     BOOLEAN NOT NULL,
    checkin_date     VARCHAR(20) NOT NULL,
    checkout_date    VARCHAR(20) NOT NULL,
    additional_needs VARCHAR(255),
    synced_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
