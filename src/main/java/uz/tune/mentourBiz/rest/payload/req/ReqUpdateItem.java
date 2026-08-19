package uz.tune.mentourBiz.rest.payload.req;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.ShopItemType;

import java.util.UUID;

@Getter
@Setter
public class ReqUpdateItem {
    private UUID logoId;
    private String name;
    private String description;
    private long price;
    private ShopItemType type;
    private long quantity;
    private UUID schoolId;
    private Boolean isActive;
}
