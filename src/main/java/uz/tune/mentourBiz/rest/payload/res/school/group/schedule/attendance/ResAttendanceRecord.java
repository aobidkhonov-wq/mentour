package uz.tune.mentourBiz.rest.payload.res.school.group.schedule.attendance;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;

import java.util.UUID;

@Getter
@Setter
public class ResAttendanceRecord {
    private Long id;
    private UUID studentId;
    private String studentFullName;
    private UUID lessonUuid;
    private AttendanceStatus status;
    private String note;
    private UUID markedByUserId;
    private String markedByFullName;

    public ResAttendanceRecord(AttendanceRecord record) {
        this.id = record.getId();
        this.status = record.getStatus();
        this.note = record.getNote();
        if (record.getStudent() != null) {
            this.studentId = record.getStudent().getUser().getUuid();
            this.studentFullName = record.getStudent().getUser().getFirstName() + " " + record.getStudent().getUser().getLastName();
        }
        if (record.getLesson() != null) {
            this.lessonUuid = record.getLesson().getUuid();
        }
        if (record.getMarkedBy() != null) {
            this.markedByUserId = record.getMarkedBy().getUuid();
            this.markedByFullName = record.getMarkedBy().getFirstName() + " " + record.getMarkedBy().getLastName();
        }
    }
}