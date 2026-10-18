package com.marketplace.entity;

/**
 * Fulfilment status of a single order line. Each vendor moves only its own items forward:
 * PLACED -> CONFIRMED -> SHIPPED -> DELIVERED, and may cancel before shipping.
 */
public enum ItemStatus {
    PLACED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(ItemStatus next) {
        return switch (this) {
            case PLACED -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
