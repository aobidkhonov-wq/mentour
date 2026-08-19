package uz.tune.mentourBiz.rest.payload.studentRes.res.lesson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResStudentMarksRow {
    private UUID unitUuid;
    private UUID studentUuid;
    private String studentFullName;
    private String userName;
    private ResAttachment attachment;
    private List<ResMarkCell> marks;
    private Integer overallScore;
    private Integer overallAttendanceScore;
}