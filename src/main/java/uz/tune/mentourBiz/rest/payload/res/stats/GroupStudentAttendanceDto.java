package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupStudentAttendanceDto {
    private UUID studentUuid;
    private String firstName;
    private String lastName;
    private Long present;
    private Long absent;
    private Long late;
    private Long notMarked;
    private Long total;
    private Double rate;
}
