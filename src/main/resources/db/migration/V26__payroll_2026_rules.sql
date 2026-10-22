-- V26: payroll as in the official monthly list (Listëpagesa 2026).
-- Per employee: tax code (001 full time, 002 part time) and whether the yearly declaration was filed
-- (it gives the tax allowance). Per payslip line: the values of columns 11), 19) and 20).
ALTER TABLE EMPLOYEE ADD (TAX_CODE VARCHAR2(3) DEFAULT '001' NOT NULL, DECLARED NUMBER(1) DEFAULT 1 NOT NULL);
ALTER TABLE PAYROLL_LINE ADD (CONTRIB_BASE NUMBER(16,2), TAX_ALLOWANCE NUMBER(16,2), TAX_BASE NUMBER(16,2),
                              TAX_CODE VARCHAR2(3), DECLARED NUMBER(1));
-- 2026 rates as used by the official list: social 9.5% employee / 15% employer (part time 9.38% / 13.92%),
-- health 1.7% + 1.7% on the full gross, contribution ceiling 186,416 ALL, no minimum base.
UPDATE APP_CONFIG SET PR_EMP_SOCIAL = 9.5, PR_ER_SOCIAL = 15, PR_EMP_HEALTH = 1.7, PR_ER_HEALTH = 1.7,
                      PR_MAX_BASE = 186416, PR_MIN_BASE = 0, PR_TAX_ON_NET = 0;
