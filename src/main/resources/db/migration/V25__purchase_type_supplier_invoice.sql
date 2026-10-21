-- V25: purchase type (for the tax office's purchase book) and the supplier's own invoice number.
-- LOCAL = goods from local suppliers (default), IMPORT = imported goods, INVEST_LOCAL = investment from
-- local suppliers, INVEST_IMPORT = imported investment, FARMER = from local farmers.
ALTER TABLE MERCHANT ADD (PURCHASE_KIND VARCHAR2(15));                                  -- the supplier's default
ALTER TABLE ORDER_INFO ADD (PURCHASE_KIND VARCHAR2(15), SUPPLIER_INVOICE VARCHAR2(60));   -- per purchase
