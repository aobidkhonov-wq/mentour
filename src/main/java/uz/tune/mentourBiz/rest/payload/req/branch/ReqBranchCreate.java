package uz.tune.mentourBiz.rest.payload.req.branch;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqBranchCreate {
    private String name;
    private String address;
    private UUID schoolId;

}