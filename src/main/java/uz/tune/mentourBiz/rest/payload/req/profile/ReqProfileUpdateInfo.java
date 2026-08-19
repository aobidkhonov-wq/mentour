package uz.tune.mentourBiz.rest.payload.req.profile;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqProfileUpdateInfo {
    private String firstName;
    private String lastName;
    private UUID imageId;
}