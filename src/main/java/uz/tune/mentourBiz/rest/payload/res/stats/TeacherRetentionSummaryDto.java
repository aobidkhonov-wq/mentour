package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRetentionSummaryDto {
    private Long enrolledStudents;
    private Long retainedStudents;
    private Long droppedStudents;
    private Double retentionRate;
    private List<TeacherGroupRetentionDto> byGroup;
}
