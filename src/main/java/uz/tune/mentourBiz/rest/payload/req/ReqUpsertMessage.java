package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.Lang;

@Data
public class ReqUpsertMessage {
    private String key;
    private Lang lang;
    private String template;
}