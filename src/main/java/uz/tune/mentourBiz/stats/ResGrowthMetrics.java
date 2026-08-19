package uz.tune.mentourBiz.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ResGrowthMetrics {
    private GrowthDetail students;
    private GrowthDetail teachers;
    private GrowthDetail classes;

    @Data
    @AllArgsConstructor
    public static class GrowthDetail {
        private long currentMonthCount;   // New items this month
        private long previousMonthCount;  // New items last month
        private double percentageChange;  // The calculated %
    }
}