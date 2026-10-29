package com.eema.backend.entity.enums;

public enum RefundStatus {
    NONE, CREATING, QUEUED, PENDING, PROCESSING, REFUNDED, FAILED, CANCELED;

    /** Provider polling cannot reopen a terminal operation. Recovery needs a separate explicit operation. */
    public boolean canProgressTo(RefundStatus target) {
        if (target == this || target == NONE || target == CREATING) return false;
        return switch (this) {
            case CREATING -> true;
            case QUEUED -> target == PENDING || target == PROCESSING || target == REFUNDED
                    || target == FAILED || target == CANCELED;
            case PENDING -> target == PROCESSING || target == REFUNDED || target == FAILED || target == CANCELED;
            case PROCESSING -> target == REFUNDED || target == FAILED;
            case NONE, REFUNDED, FAILED, CANCELED -> false;
        };
    }
}
