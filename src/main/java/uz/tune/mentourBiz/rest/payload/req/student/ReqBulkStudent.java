package uz.tune.mentourBiz.rest.payload.req.student;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqBulkStudent {
    private String firstName;
    private String lastName;
    private String username;
    private String phoneNumber;
    private UUID groupId;
}
