package uz.tune.mentourBiz.rest.enums;

import java.util.Arrays;

public enum TransactionStatus {
    PREPARE,
    PENDING,
    SUCCESS,
    FAILED,
    REQ_ERROR,
    SCHOOL_PAY_ACCOUNT_NOT_FOUND,
    UNKNOWN,
    PAID_OUT,
    CANCELED,
    PROCESSING_PAYOUT,
    RES_ERROR,
    NO_PAYOUT_DETAILS;

    public static TransactionStatus getByName(String value) {
        return Arrays.stream(TransactionStatus.values())
                .filter(status -> status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
