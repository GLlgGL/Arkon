package com.fms.domain;

import java.util.Arrays;

/**
 * The three kinds of order. In the original these were three separate tables
 * (Purchases, Sales, Offers) and three copies of every page.
 */
public enum OrderType {

    PURCHASE("purchases", "Purchase", "Purchases", "Merchant", "Company"),
    SALE("sales", "Sale", "Sales", "Company", "Client"),
    OFFER("offers", "Offer", "Offers", "Company", "Client");

    private final String path;
    private final String label;
    private final String pluralLabel;
    private final String providerLabel;
    private final String buyerLabel;

    OrderType(String path, String label, String pluralLabel, String providerLabel, String buyerLabel) {
        this.path = path;
        this.label = label;
        this.pluralLabel = pluralLabel;
        this.providerLabel = providerLabel;
        this.buyerLabel = buyerLabel;
    }

    /** URL segment, e.g. "sales" in /sales/123. */
    public String getPath() { return path; }
    public String getLabel() { return label; }
    public String getPluralLabel() { return pluralLabel; }
    public String getProviderLabel() { return providerLabel; }
    public String getBuyerLabel() { return buyerLabel; }

    /** Sales and offers use sale price, client discounts and stock checks; purchases don't. */
    public boolean isOutgoing() { return this != PURCHASE; }

    public static OrderType fromPath(String path) {
        return Arrays.stream(values())
                .filter(t -> t.path.equalsIgnoreCase(path))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Unknown order type: " + path));
    }
}
