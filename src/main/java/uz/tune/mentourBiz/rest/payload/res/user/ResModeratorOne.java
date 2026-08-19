package uz.tune.mentourBiz.rest.payload.res.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Moderator;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResModeratorOne {

    private UUID uuid;
    private String username;
    private String fullName;
    private UserRole role = UserRole.MODERATOR;
    private ResSchoolInfo school;

    public ResModeratorOne(Moderator moderator) {
        this.uuid = moderator.getUser().getUuid();
        this.username = moderator.getUser().getUsername();
        this.fullName = moderator.getUser().getFirstName() + " " + moderator.getUser().getLastName();
        this.school = new ResSchoolInfo(moderator.getSchool());
    }
}