-- V24: for 0% VAT sales, which column of the tax office's sales book they go in
-- (EXPORT = Eksporte mallrash, ZERO = Furnizime me 0%, EXEMPT = Shitjet e përjashtuara, NOVAT = Shitjet pa TVSH).
ALTER TABLE CLIENT ADD (VAT_BOOK VARCHAR2(10));
