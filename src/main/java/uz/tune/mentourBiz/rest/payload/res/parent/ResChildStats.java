package uz.tune.mentourBiz.rest.payload.res.parent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResChildStats {
    private UUID studentUuid;
    private String fullName;
    private String schoolName;
    private int overallAttendancePercentage;
    private int overallScorePercentage;
    private List<ResChildCourseStat> courses;
}
