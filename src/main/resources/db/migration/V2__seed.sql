-- Default settings row (the app always reads ID = 1).
INSERT INTO APP_CONFIG (ID, CURRENCY, VAT_PERCENT, BRAND_RGB, LOGO_FILE)
VALUES (1, 'ALL', 20, '11,71,121', NULL);

-- Initial administrator. Username: admin   Password: admin123
-- Change this password right after the first login (Profile > Change password).
-- Skip or delete this row if you import the users from SQL Server instead.
INSERT INTO APP_USER (FIRST_NAME, LAST_NAME, EMAIL, USERNAME, PASSWORD, USER_ROLE, LAST_PASSWORD_CHANGE_AT)
VALUES ('System', 'Administrator', NULL, 'admin',
        '{bcrypt}$2a$10$RV1PLwrV9fnWMpd/UGtDPeSmtl1/DV1jVf7jmPYZGy87Pmvkxsflq',
        1, SYSTIMESTAMP);
