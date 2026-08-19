package uz.tune.mentourBiz.rest.payload.req.school;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;

@Getter
@Setter
public class ReqSchoolSearchByName {
    private SchoolStatus status;
    private String schoolName;
}
