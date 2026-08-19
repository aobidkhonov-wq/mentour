package uz.tune.mentourBiz.rest.admin.req.upd;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.UnitStatus;
import uz.tune.mentourBiz.rest.enums.UnitType;

@Data
public class ReqUpdateUnit {
    private String title;
    private String topic;
    private Integer sortOrder;
    private UnitStatus status;
    private UnitType type;
}
