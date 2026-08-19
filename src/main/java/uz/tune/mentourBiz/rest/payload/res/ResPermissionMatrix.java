package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResPermissionMatrix {
    private UserPermission permission;
    private String displayName;
    private Map<UserRole, Boolean> roleAccess;
}
