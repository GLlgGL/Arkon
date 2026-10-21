-- V20: VAT rate per client and per sale or offer (default: the rate in Settings).

ALTER TABLE CLIENT ADD (VAT_RATE NUMBER(5,2), VAT_NOTE VARCHAR2(300));       -- null = normal rate
ALTER TABLE ORDER_INFO ADD (VAT_RATE NUMBER(5,2), VAT_NOTE VARCHAR2(300));   -- the rate this order uses, if not the normal one
