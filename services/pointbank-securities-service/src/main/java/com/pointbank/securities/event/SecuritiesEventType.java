package com.pointbank.securities.event;

public final class SecuritiesEventType {
    public static final String CASH_ACCOUNT_CREATE_REQUESTED = "CASH_ACCOUNT_CREATE_REQUESTED";
    public static final String CASH_ACCOUNT_CREATED = "CASH_ACCOUNT_CREATED";
    public static final String BUY_ORDER_REQUESTED = "BUY_ORDER_REQUESTED";

    private SecuritiesEventType() {
    }
}
