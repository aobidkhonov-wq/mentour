package uz.tune.mentourBiz.rest.payload.req.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqSignIn {

    private String username;
    private String password;

}
