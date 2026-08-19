package uz.tune.mentourBiz.rest.payload.res.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolAdmin;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResSchoolAdminOne {

    private UUID uuid;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserRole role = UserRole.SCHOOL_ADMIN;
    private ResSchoolInfo school;
    private UserStatus status;
    private java.time.Instant lastActiveAt;
    private String timeSinceLastActive;

    public ResSchoolAdminOne(SchoolAdmin schoolAdmin) {
        User user = schoolAdmin.getUser();
        this.uuid = user.getUuid();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.status = user.getStatus();
        this.lastActiveAt = user.getLastActiveAt();

        if (schoolAdmin.getSchool() != null) {
            this.school = new ResSchoolInfo(schoolAdmin.getSchool());
        }

        if (this.lastActiveAt != null) {
            java.time.Duration d = java.time.Duration.between(this.lastActiveAt, java.time.Instant.now());
            long s = d.getSeconds();
            if (s < 60) this.timeSinceLastActive = s + "s ago";
            else if (s < 3600) this.timeSinceLastActive = (s / 60) + "m ago";
            else if (s < 86400) this.timeSinceLastActive = (s / 3600) + "h ago";
            else this.timeSinceLastActive = (s / 86400) + "d ago";
        } else {
            this.timeSinceLastActive = "never";
        }
    }
}