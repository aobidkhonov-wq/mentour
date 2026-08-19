package uz.tune.mentourBiz.rest.payload.req.mentor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqMentorCreate {
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String aboutMe;
}