package uz.tune.mentourBiz.rest.payload.req.school;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class ReqClassCreate {
    private String name;
    private UUID schoolId;
}