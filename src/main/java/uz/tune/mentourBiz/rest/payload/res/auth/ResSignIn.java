package uz.tune.mentourBiz.rest.payload.res.auth;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.model.AuthResponse;

@Getter
@Setter
public class ResSignIn {

    private String accessToken;
    private Long accessTokenExpire;
    private String refreshToken;
    private Long refreshTokenExpire;
    private UserRole role;

    public ResSignIn(AuthResponse authResponse) {
        this.accessToken = authResponse.accessToken();
        this.accessTokenExpire = authResponse.accessTokenExpire();
        this.refreshToken = authResponse.refreshToken();
        this.refreshTokenExpire = authResponse.refreshTokenExpire();
        this.role = authResponse.role();
    }
}
