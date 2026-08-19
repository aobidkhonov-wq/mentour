package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.Region;
import uz.tune.mentourBiz.rest.enums.Lang;

import java.util.UUID;

@Data
public class ResRegion {
    private UUID uuid;
    private String name;
    private String country;
    private String phoneCode;
    private String currency;
    private Lang lang;

    public ResRegion(Region region) {
        if (region != null) {
            this.uuid = region.getUuid();
            this.name = region.getName();
            this.country = region.getCountry();
            this.phoneCode = region.getPhoneCode();
            this.currency = region.getCurrency();
            this.lang = region.getLang();
        }
    }
}