package uz.tune.mentourBiz.rest.payload.res.shop;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.shopManagement.ShopItem;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.UUID;

@Getter
@Setter
public class ResShopItem {
    private UUID uuid;
    private ResAttachment attachment;
    private String name;
    private String description;
    private long price;
    private long quantity;
    private ResSchoolInfo schoolInfo;

    public ResShopItem(ShopItem shopItem) {
        this.uuid = shopItem.getUuid();
        this.name = shopItem.getName();
        this.description = shopItem.getDescription();
        if(shopItem.getAttachment() != null) {
            this.attachment = new ResAttachment(shopItem.getAttachment());
        }
        this.price = shopItem.getPrice();
        this.quantity = shopItem.getQuantity();
        this.schoolInfo = new ResSchoolInfo(shopItem.getSchool());
    }
}
