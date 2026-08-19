package uz.tune.mentourBiz.rest.payload.req;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.ShopItemType;
import java.util.UUID;

@Getter
@Setter
public class ReqCreateItem {
    private String name;
    private String description;
    private Long price;
    private Long quantity;
    private ShopItemType type;
    private UUID logoId;
    private UUID schoolId;
}