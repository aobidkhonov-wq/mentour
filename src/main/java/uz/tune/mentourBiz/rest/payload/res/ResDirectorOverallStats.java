package uz.tune.mentourBiz.rest.payload.res;

import lombok.Builder;
import lombok.Data;
import uz.tune.mentourBiz.stats.ResGrowthMetrics;

@Data
@Builder
public class ResDirectorOverallStats {
    // Totals
    private long totalSchools;
    private long totalStudents;
    private long totalTeachers;
    private long totalActiveGroups;

    // Financials (Across all schools)
    private Long totalCollectedRevenueMonth; // This month
    private Long totalOutstandingDebt;      // Students with negative balance
    private Double avgCollectionRate;        // %

    // Performance
    private Double avgOrganizationAttendance; // %
    private Double avgUnitProgress;           // %

    // Activity
    private long pendingShopOrders;          // Orders awaiting approval across all schools
    private long expiringSchoolsCount;       // Schools with < 7 days sub left
//
//    // Growth (Compared to last month)
//    private Double studentGrowthRate;
//    private Double revenueGrowthRate;

    private ResGrowthMetrics growthMetrics;
}