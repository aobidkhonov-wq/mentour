package uz.tune.mentourBiz.rest.payload.req.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqResetPassword {
    private String token;
    private String newPassword;
}