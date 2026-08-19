package uz.tune.mentourBiz.rest.payload.studentRes.res.lesson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResMarkCell {
    private UUID unitUuid;
    private LocalDate date;
    private Integer score;
    private AttendanceStatus attendance;
    private UUID scheduleUUID;
}