package uz.tune.mentourBiz.rest.payload.res.parent;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResChildSchool {
    private UUID uuid;
    private String name;
    private String address;
    private String contactInfo;
    private String telegramLink;
    private ResAttachment logo;

    public ResChildSchool(School school) {
        this.uuid = school.getUuid();
        this.name = school.getName();
        this.address = school.getAddress();
        this.contactInfo = school.getContactInfo();
        this.telegramLink = school.getTelegramLink();
        this.logo = (school.getLogo() != null) ? new ResAttachment(school.getLogo()) : null;
    }
}
