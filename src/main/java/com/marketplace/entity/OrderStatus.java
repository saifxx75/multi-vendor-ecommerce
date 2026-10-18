package com.marketplace.entity;

/**
 * Overall order status. It is derived from the status of the order's items
 * (each item is fulfilled independently by its own vendor).
 */
public enum OrderStatus {
    PLACED,
    COMPLETED,
    CANCELLED
}
