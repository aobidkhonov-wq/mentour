package uz.tune.mentourBiz.rest.payload.res.parent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.level.ResLevel;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResChildOverview {
    private UUID studentUuid;
    private String fullName;
    private UserStatus status;
    private ResAttachment studentPhoto;
    private String parentName;
    private Long studentBalance;
    private String groupName;
    private ResLevel level;
    private Long coins;
    private Integer score;
    private Integer ranking;
    private Boolean isAtRisk;
    private ResChildSchool school;
    private ResChildTeacher teacher;
    private int attendancePercentage;
    private int averageScorePercentage;
    private long homeworkCount;
    private long completedHomeworkCount;
    private LocalDate nextPaymentDate;
    private Long paymentAmount;
}
