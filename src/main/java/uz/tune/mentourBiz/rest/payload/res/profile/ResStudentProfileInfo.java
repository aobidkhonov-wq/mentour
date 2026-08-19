package uz.tune.mentourBiz.rest.payload.res.profile;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.UUID;

@Getter
@Setter
public class ResStudentProfileInfo {
    private UUID profileId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String username;
    private Lang language;
    private UserRole role;
    private ResSchoolInfo schoolInfo;

    public ResStudentProfileInfo(User user, School school ) {
        this.profileId = user.getUuid();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.username = user.getUsername();
        this.language = user.getLang();
        this.role = user.getRole();
        this.schoolInfo = new ResSchoolInfo(school);
    }
}