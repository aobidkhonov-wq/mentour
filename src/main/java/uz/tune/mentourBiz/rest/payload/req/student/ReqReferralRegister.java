package uz.tune.mentourBiz.rest.payload.req.student;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqReferralRegister {
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private UUID referralCode;
}