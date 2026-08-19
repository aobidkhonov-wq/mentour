package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResGroupAnalytics {

    private long groupId;
    private String groupName;
    private String courseStatus;
    private long studentCount;
    private double avgAttendanceRate;
    private double avgMarkingRate;
    private Double avgGrade;
    private long revenueGenerated;

}
