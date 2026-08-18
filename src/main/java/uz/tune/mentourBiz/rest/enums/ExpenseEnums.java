package uz.tune.mentourBiz.rest.enums;

/** Enumerations behind the school expense ledger — every som that leaves the school. */
public class ExpenseEnums {

    /**
     * What the money was spent on. TEACHER_SALARY is the only category the system books on its own
     * (every teacher payment writes one); the rest are entered by hand and exist so a school can see
     * where its money goes without inventing a category per row.
     */
    public enum ExpenseCategory {
        TEACHER_SALARY,
        STAFF_SALARY,
        RENT,
        UTILITIES,
        MARKETING,
        SUPPLIES,
        EQUIPMENT,
        TAX,
        MAINTENANCE,
        OTHER
    }
}
