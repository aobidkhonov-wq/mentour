package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

@Data
@AllArgsConstructor
public class ResSchoolPaymentStatus {
    private ResSchoolInfo resSchoolInfo;
    private boolean isPaymentActive;
    private long packageCount;
    private String issue;
}