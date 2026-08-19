package uz.tune.mentourBiz.rest.payload.req.user;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqUserCreate {
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private UserRole role;
    private String phoneNumber;
    private String address;
    private UUID schoolId;
    private UUID organizationUuid;
    private String aboutMe;
    private List<UUID> groupUuids;
}