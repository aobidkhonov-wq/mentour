package uz.tune.mentourBiz.rest.enums;

public class FinanceEnums {
    public enum FinanceStatus {
        PAID,
        PARTIAL,
        UNPAID,
        NEW
    }

    public enum FinanceTransactionType {
        CHARGE,
        PAYMENT,
        ADJUSTMENT
    }

    public enum PaymentMethod {
        CASH, CARD, SELLO, BONUS, OCTO, SYSTEM_ADJUSTMENT
    }

    public enum FinanceCurrency {
        UZS, USD
    }
}