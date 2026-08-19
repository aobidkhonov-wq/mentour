package uz.tune.mentourBiz.rest.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResCourseGroupDetails {
    private String teacherName;
    private ResAttachment teacherAttachment;
    private Integer courseAvgResult;
    private List<ResCourseStudentDetail> students;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResCourseStudentDetail {
        private UUID studentUuid;
        private String fullName;
        private ResAttachment attachment;
        private Integer attendancePercentage;
        private Integer resultPercentage;
    }
}
