package uz.tune.mentourBiz.rest.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ResTeacherRank {
    private UUID teacherUuid;
    private String fullName;
    private Integer studentCount;
    private Double avgPerformance;
    private ResAttachment profilePhoto;
}