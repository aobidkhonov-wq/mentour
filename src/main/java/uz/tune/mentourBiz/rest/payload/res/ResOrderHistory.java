package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.ItemRedemption;
import uz.tune.mentourBiz.rest.enums.ItemRedemptionStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ResOrderHistory {
    private UUID orderUuid;
    private String studentFullName;
    private String productName;
    private ResAttachment itemImage;
    private Long priceAtPurchase;
    private Integer quantity;
    private Long totalSpent;
    private ItemRedemptionStatus status;
    private Instant orderDate;

    public ResOrderHistory(ItemRedemption redemption) {
        this.orderUuid = redemption.getUuid();
        this.studentFullName = redemption.getStudent().getUser().getFirstName() + " " +
                redemption.getStudent().getUser().getLastName();
        this.productName = redemption.getShopItem().getName();

        if (redemption.getShopItem().getAttachment() != null) {
            this.itemImage = new ResAttachment(redemption.getShopItem().getAttachment());
        }
        this.priceAtPurchase = redemption.getShopItem().getPrice();
        this.quantity = redemption.getCount();
        this.totalSpent = this.priceAtPurchase * this.quantity;
        this.status = redemption.getStatus();
        this.orderDate = redemption.getCreatedAt();
    }
}