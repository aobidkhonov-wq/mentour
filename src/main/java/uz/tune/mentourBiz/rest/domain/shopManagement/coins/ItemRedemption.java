package uz.tune.mentourBiz.rest.domain.shopManagement.coins;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.shopManagement.ShopItem;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.ItemRedemptionStatus;


import java.util.UUID;

@Table(name = "item_redemptions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemRedemption extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ItemRedemptionStatus status;

    @Column(name = "count")
    private Integer count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_Id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "shop_Item_Id")
    private ShopItem shopItem;


}
