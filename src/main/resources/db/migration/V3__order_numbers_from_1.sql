-- Order numbers now count up from 00000001 (shown with 8 digits).
-- V1 started the sequence at 10000000; this restarts it at 1.
-- The app skips any number that is already used, so codes imported from the
-- old system (which were random 8-digit numbers) never clash with new ones.
DROP SEQUENCE ORDER_CODE_SEQ;
CREATE SEQUENCE ORDER_CODE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;
