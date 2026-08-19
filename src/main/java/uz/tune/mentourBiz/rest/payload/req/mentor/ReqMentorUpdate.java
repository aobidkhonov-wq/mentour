package uz.tune.mentourBiz.rest.payload.req.mentor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqMentorUpdate {

    private String firstName;
    private String lastName;
    private String username;
    private String aboutMe;
}