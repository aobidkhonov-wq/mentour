package uz.tune.mentourBiz.rest.admin.req.create.adminCreate;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.SchoolBookStatus;

import java.util.UUID;

@Data
public class ReqCreateBook {
    private String name;
    private UUID levelUuid;
    private SchoolBookStatus status;
    private boolean isGlobal;
}