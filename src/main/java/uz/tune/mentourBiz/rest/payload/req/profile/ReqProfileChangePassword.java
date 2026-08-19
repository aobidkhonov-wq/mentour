package uz.tune.mentourBiz.rest.payload.req.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqProfileChangePassword {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}