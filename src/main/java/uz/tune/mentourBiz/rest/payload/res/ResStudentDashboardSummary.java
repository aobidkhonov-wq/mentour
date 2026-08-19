package uz.tune.mentourBiz.rest.payload.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResStudentDashboardSummary {
    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;
    private long newThisMonth;
    private long leftThisMonth;
    private double parentConnectionRate;
}