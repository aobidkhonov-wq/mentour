package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherGroupRetentionDto {
    private Long groupId;
    private String groupName;
    private Long enrolled;
    private Long retained;
    private Long dropped;
    private Double retentionRate;
}
