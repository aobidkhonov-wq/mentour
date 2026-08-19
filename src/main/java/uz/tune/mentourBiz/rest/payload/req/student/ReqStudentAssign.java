package uz.tune.mentourBiz.rest.payload.req.student;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqStudentAssign {
    private List<UUID> studentUuids;
    private UUID groupUuids;
    private String note;
}
