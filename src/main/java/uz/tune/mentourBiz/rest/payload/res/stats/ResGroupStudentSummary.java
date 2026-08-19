package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResGroupStudentSummary {

    private String studentUuid;
    private String firstName;
    private String lastName;
    private double attendanceRate;
    private Double avgGrade;
    private long totalPaid;

}
