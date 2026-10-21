-- V19: exchange differences on foreign-currency payments.

-- The rate on the payment day (daily rate, or typed by hand) and the resulting
-- difference in the base currency: + gain, - loss for the company.
ALTER TABLE PAYMENT ADD (PAY_RATE NUMBER(14,6), FX_DIFF NUMBER(16,2));

-- Accounts for exchange gains and losses (Albanian plan: 769 and 669).
INSERT INTO ACC_ACCOUNT (CODE, NAME, TYPE, SYSTEM_KEY)
    SELECT '769', 'Fitime nga kursi i këmbimit', 'INCOME', 'FX_GAIN' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM ACC_ACCOUNT WHERE CODE = '769');
INSERT INTO ACC_ACCOUNT (CODE, NAME, TYPE, SYSTEM_KEY)
    SELECT '669', 'Humbje nga kursi i këmbimit', 'EXPENSE', 'FX_LOSS' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM ACC_ACCOUNT WHERE CODE = '669');
-- If someone already added 769 / 669 by hand, use those.
UPDATE ACC_ACCOUNT SET SYSTEM_KEY = 'FX_GAIN' WHERE CODE = '769' AND SYSTEM_KEY IS NULL
    AND NOT EXISTS (SELECT 1 FROM ACC_ACCOUNT WHERE SYSTEM_KEY = 'FX_GAIN');
UPDATE ACC_ACCOUNT SET SYSTEM_KEY = 'FX_LOSS' WHERE CODE = '669' AND SYSTEM_KEY IS NULL
    AND NOT EXISTS (SELECT 1 FROM ACC_ACCOUNT WHERE SYSTEM_KEY = 'FX_LOSS');
