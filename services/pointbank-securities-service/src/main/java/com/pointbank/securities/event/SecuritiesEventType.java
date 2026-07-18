package com.pointbank.securities.event;

public final class SecuritiesEventType {
    public static final String CASH_ACCOUNT_CREATE_REQUESTED = "CASH_ACCOUNT_CREATE_REQUESTED";
    public static final String CASH_ACCOUNT_CREATED = "CASH_ACCOUNT_CREATED";
    public static final String BUY_ORDER_REQUESTED = "BUY_ORDER_REQUESTED";
    public static final String BUY_FUNDS_DEBITED = "BUY_FUNDS_DEBITED";
    public static final String BUY_FUNDS_FAILED = "BUY_FUNDS_FAILED";
    public static final String SELL_ORDER_REQUESTED = "SELL_ORDER_REQUESTED";
    public static final String SELL_FUNDS_CREDITED = "SELL_FUNDS_CREDITED";
    public static final String SELL_FUNDS_FAILED = "SELL_FUNDS_FAILED";

    private SecuritiesEventType() {
    }
}
