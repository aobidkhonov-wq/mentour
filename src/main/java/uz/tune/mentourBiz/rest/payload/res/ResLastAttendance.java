package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResLastAttendance {
    private String lessonName;
    private Instant lessonDate;
    private AttendanceStatus status;
    private Boolean isMarked;
}