package uz.tune.mentourBiz.rest.payload.res.shop.shopping;


import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.shopManagement.ShopItem;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.ItemRedemptionStatus;
import uz.tune.mentourBiz.rest.payload.res.shop.ResShopItem;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentOne;

@Getter
@Setter
public class ResItemRdmpOne {
    private ResShopItem shopItem;
    private ResStudentOne studentOne;
    private ItemRedemptionStatus status;
    private Integer count;

    public ResItemRdmpOne(Student student, ShopItem shopItem, ItemRedemptionStatus status, Integer count) {
        this.shopItem = new ResShopItem(shopItem);
        this.studentOne = new ResStudentOne(student);
        this.status = status;
        this.count = count;
    }
}
