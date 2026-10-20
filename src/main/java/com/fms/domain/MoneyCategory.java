package com.fms.domain;

/**
 * Kinds of money movement. Each knows its direction and whether it can be
 * entered by hand (the others come from payments, transfers or reconciling).
 * Texts are in messages_*.properties (cash.cat.*).
 */
public enum MoneyCategory {
    // money in
    SALE_PAYMENT(true, false), PURCHASE_REFUND(true, false),
    CAPITAL(true, true), LOAN_IN(true, true), OTHER_INCOME(true, true),
    TRANSFER_IN(true, false), ADJUSTMENT_IN(true, false),
    // money out
    PURCHASE_PAYMENT(false, false), SALE_REFUND(false, false),
    RENT(false, true), SALARIES(false, true), UTILITIES(false, true), TRANSPORT(false, true),
    TAXES(false, true), BANK_FEES(false, true), OWNER_WITHDRAWAL(false, true), LOAN_REPAYMENT(false, true),
    OTHER_EXPENSE(false, true),
    TRANSFER_OUT(false, false), ADJUSTMENT_OUT(false, false);

    private final boolean in;
    private final boolean manual;

    MoneyCategory(boolean in, boolean manual) {
        this.in = in;
        this.manual = manual;
    }

    public boolean isIn() { return in; }
    public boolean isManual() { return manual; }
    public String getDirection() { return in ? "IN" : "OUT"; }
}
