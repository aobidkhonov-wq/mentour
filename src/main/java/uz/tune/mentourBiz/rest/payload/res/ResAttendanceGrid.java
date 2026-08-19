package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResAttendanceGrid {
    private List<AttendanceGridHeader> headers;
    private List<StudentAttendanceRow> rows;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;

    @Data
    @AllArgsConstructor
    public static class AttendanceGridHeader {
        private LocalDate date;
        private String lessonName;
        private UUID lessonUuid;
    }

    @Data
    @AllArgsConstructor
    public static class StudentAttendanceRow {
        private UUID studentUuid;
        private String fullName;
        private String userName;
        private ResAttachment attachment;
        private List<AttendanceCell> cells;
    }

    @Data
    @AllArgsConstructor
    public static class AttendanceCell {
        private UUID lessonUuid;
        private AttendanceStatus status;
        private Boolean isMarked;
        private String note;
    }
}