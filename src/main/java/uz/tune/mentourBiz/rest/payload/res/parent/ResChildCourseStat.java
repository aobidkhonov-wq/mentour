package uz.tune.mentourBiz.rest.payload.res.parent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResChildCourseStat {
    private UUID courseUuid;
    private String courseName;
    private int attendancePercentage;
    private int averageScorePercentage;
}
