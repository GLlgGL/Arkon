-- V10: reasons on returns (sales and purchases), and shelves off by default.

ALTER TABLE ORDER_LINE ADD (
    RETURN_REASON  VARCHAR2(30),    -- DAMAGED, DEFECTIVE, WRONG_ITEM, NOT_NEEDED, QUALITY, OTHER
    RETURN_NOTE    VARCHAR2(300)
);

-- Shelves are optional and start switched off. An installation that already
-- has shelves keeps its current setting.
ALTER TABLE APP_CONFIG MODIFY (USE_SHELVES DEFAULT 0);
UPDATE APP_CONFIG SET USE_SHELVES = 0 WHERE NOT EXISTS (SELECT 1 FROM SHELF);
