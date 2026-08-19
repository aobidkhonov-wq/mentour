package uz.tune.mentourBiz.rest.payload.studentRes.res.school;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.UUID;

@Getter
@Setter
public class ResSchoolDetails {
    private UUID uuid;
    private String name;
    private SchoolStatus status;
    private ResAttachment logo;

    public ResSchoolDetails(School school) {
        this.uuid = school.getUuid();
        this.name = school.getName();
        this.status = school.getStatus();
        if (school.getLogo() != null) {
            this.logo = new ResAttachment(school.getLogo());
        }
    }
}