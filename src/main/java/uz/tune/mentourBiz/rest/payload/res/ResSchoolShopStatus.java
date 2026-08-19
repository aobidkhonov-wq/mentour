package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

@Data
@AllArgsConstructor
public class ResSchoolShopStatus {
    private ResSchoolInfo resSchoolInfo;
    private Integer studentCount;
    private String status;
}