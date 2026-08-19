package uz.tune.mentourBiz.rest.admin.req.upd;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.SchoolBookStatus;

import java.util.UUID;

@Data
public class ReqUpdateBook {
    private String name;
    private SchoolBookStatus status;
    private UUID levelUuid;
    private Boolean isGlobal;
}
