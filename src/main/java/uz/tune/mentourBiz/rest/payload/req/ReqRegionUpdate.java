package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.Lang;

@Data
public class ReqRegionUpdate {
    private String name;
    private String country;
    private String phoneCode;
    private String currency;
    private Lang lang;
}