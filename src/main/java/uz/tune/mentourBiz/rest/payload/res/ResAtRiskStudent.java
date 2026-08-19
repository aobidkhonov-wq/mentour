package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResAtRiskStudent {
    private UUID studentUuid;
    private String fullName;
    private String username;
    private ResAttachment attachment;
    private String groupName;
    private Integer attendanceRate;
    private Integer averageResult;
    private Integer attendanceThreshold;
    private Integer resultsThreshold;
}