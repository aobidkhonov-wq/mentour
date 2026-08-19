package uz.tune.mentourBiz.rest.payload.req.user;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqUserUpdateDetails {
    private String username;
    private String firstName;
    private String lastName;
    private UUID schoolId;
    private UUID organizationUuid;
    private String password;
    private UserStatus status;
    private String phoneNumber;
    private String address;
    private List<UUID> groupUuids;
    // Nullable on purpose: null / true keep the student's billing plan, an explicit false
    // clears it when reactivating. A primitive boolean would default to false and silently
    // wipe billing plans on unrelated updates (e.g. a name change).
    private Boolean withBillingPlan;
}