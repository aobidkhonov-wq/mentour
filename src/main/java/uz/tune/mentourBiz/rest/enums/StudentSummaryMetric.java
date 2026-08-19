package uz.tune.mentourBiz.rest.enums;

public enum StudentSummaryMetric {
    TOTAL,      // Total active accounts
    ACTIVE,     // Active + has enrollment
    INACTIVE,   // Active + no enrollment (Unassigned)
    NEW,        // Created this month
    LEFT,       // Blocked this month
    PARENTS     // Has parent connection
}