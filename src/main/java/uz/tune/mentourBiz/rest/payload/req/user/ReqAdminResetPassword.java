package uz.tune.mentourBiz.rest.payload.req.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqAdminResetPassword {
    private String newPassword;
}