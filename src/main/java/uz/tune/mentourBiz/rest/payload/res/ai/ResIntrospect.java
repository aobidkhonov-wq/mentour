package uz.tune.mentourBiz.rest.payload.res.ai;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.UserRole;
import java.util.UUID;
import java.util.List;

@Getter
@Setter
public class ResIntrospect {
    private final UUID uuid;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final UserRole role;
    private final Lang lang;
    private UUID schoolUuid;
    private List<UUID> authorizedSchoolUuids;

    public ResIntrospect(User user, UUID schoolUuid) {
        this.uuid = user.getUuid();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.role = user.getRole();
        this.lang = user.getLang();
        this.schoolUuid = schoolUuid;
    }
}