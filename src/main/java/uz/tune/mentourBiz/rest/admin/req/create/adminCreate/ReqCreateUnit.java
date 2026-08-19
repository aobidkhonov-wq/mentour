package uz.tune.mentourBiz.rest.admin.req.create.adminCreate;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.UnitStatus;
import uz.tune.mentourBiz.rest.enums.UnitType;

import java.util.UUID;

@Data
public class ReqCreateUnit {
    private UUID bookUuid;
    private String title;
    private String topic;
    private UUID levelUuid;
    private Integer sortOrder;
    private UnitStatus status;
    private UnitType type;
}