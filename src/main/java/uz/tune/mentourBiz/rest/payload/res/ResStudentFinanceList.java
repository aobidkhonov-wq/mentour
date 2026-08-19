package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.List;
import java.util.UUID;

@Data
public class ResStudentFinanceList {
    private UUID studentUuid;
    private ResAttachment attachment;
    private List<ResPaymentPackage> resPaymentPackage;
    private String studentName;
    private String courseName;
    private String schoolName;
    private String teacherName;
    private String userName;
    private Long balance;
    private FinanceEnums.FinanceStatus status;
    private UserStatus userStatus;
}