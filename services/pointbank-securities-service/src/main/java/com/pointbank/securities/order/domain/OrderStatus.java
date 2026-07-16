package com.pointbank.securities.order.domain;

public enum OrderStatus {
    REQUESTED,
    FUNDS_COMPLETED,
    COMPLETED,
    FAILED,
    MANUAL_REVIEW,
    CANCELED,
    REVERSED
}
