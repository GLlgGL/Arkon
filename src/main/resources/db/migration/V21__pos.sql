-- V21: Quick sale (POS), optional module (off by default).
ALTER TABLE APP_CONFIG ADD (
    MOD_POS      NUMBER(1) DEFAULT 0 NOT NULL,
    POS_COMPANY  VARCHAR2(200),     -- the company selling (empty = the first company)
    POS_CLIENT   VARCHAR2(200)      -- the default client for walk-in sales (empty = "Klient pakice")
);
