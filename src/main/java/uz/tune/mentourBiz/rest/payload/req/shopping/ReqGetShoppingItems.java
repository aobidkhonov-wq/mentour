package uz.tune.mentourBiz.rest.payload.req.shopping;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.ShopItemType;

import java.util.UUID;

@Getter
@Setter
public class ReqGetShoppingItems {
    private UUID schoolUuid;
    private ShopItemType shopItemType;
}
