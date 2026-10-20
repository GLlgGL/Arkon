-- V7: shelves can be switched off. 1 = use shelves, 0 = ignore them everywhere.
ALTER TABLE APP_CONFIG ADD (USE_SHELVES NUMBER(1) DEFAULT 1 NOT NULL);
