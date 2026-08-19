package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.Lang;

@Data
public class ReqUpdateParent {
    private String name;
    private String telegramNickname;
    private String phoneNumber;
    private Lang language;
    private Boolean isActive;
}