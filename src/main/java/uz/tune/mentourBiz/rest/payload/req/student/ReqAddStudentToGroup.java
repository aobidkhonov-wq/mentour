package uz.tune.mentourBiz.rest.payload.req.student;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqAddStudentToGroup {
    private UUID groupUuid;
    private UUID schoolUuid;
}
