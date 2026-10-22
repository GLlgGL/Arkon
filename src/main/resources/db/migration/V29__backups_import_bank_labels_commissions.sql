-- V29: automatic backups, bank statement import, sales commissions.

-- Automatic backups
ALTER TABLE APP_CONFIG ADD (
    BK_AUTO         NUMBER(1) DEFAULT 0 NOT NULL,
    BK_DIR          VARCHAR2(500),
    BK_EVERY_HOURS  NUMBER(4) DEFAULT 0 NOT NULL,       -- 0 = once a day at BK_AT
    BK_AT           VARCHAR2(5) DEFAULT '02:00' NOT NULL,
    BK_FULL         NUMBER(1) DEFAULT 1 NOT NULL,       -- 1 = data + invoices + logo, 0 = data only
    BK_REPLACE      NUMBER(1) DEFAULT 0 NOT NULL,       -- 1 = one file, replaced each time
    BK_KEEP_COUNT   NUMBER(4) DEFAULT 14 NOT NULL,      -- keep the last N (0 = no limit)
    BK_KEEP_DAYS    NUMBER(4) DEFAULT 0 NOT NULL,       -- delete older than N days (0 = no limit)
    BK_LAST_RUN     TIMESTAMP,
    BK_LAST_OK      NUMBER(1),
    BK_LAST_MSG     VARCHAR2(500)
);

-- Bank statement import: the statement line a movement was matched to or created from (so it isn't imported twice)
ALTER TABLE MONEY_MOVEMENT ADD (BANK_REF VARCHAR2(64));
CREATE INDEX IX_MOVEMENT_BANK_REF ON MONEY_MOVEMENT (BANK_REF);

-- Sales commissions: per user, a % of sales without VAT or of profit
ALTER TABLE APP_USER ADD (COMMISSION_PCT NUMBER(5,2), COMMISSION_BASE VARCHAR2(8));
