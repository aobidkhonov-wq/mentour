package uz.tune.mentourBiz.config;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import org.springframework.stereotype.Component;
import uz.tune.mentourBiz.config.security.AppUserDetails;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.Objects;

@Component("Validation")
public class Validation {

    public boolean checkRole(UserRole role) {

        AppUserDetails userDetails = GlobalVar.getUserDetails();

        if (Objects.isNull(userDetails)) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        return userDetails.getRole().equals(role);
    }

}
