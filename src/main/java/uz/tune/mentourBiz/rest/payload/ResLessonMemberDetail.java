package uz.tune.mentourBiz.rest.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ResLessonMemberDetail {
    private UUID studentUuid;
    private UUID userUuid;
    private String fullName;
    private ResAttachment profilePhoto;
    private Integer avgUnitProgress;
    private AttendanceStatus attendanceStatus;
    private Boolean isMarked;
}