package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import uz.tune.mentourBiz.rest.domain.SubscriptionPlan;
import uz.tune.mentourBiz.rest.enums.Currency;

import java.util.UUID;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ResSubscriptionPlan {
    private UUID uuid;
    private String planName;
    private Long price;
    private Currency currency;
    private Integer maxStudents;

    public ResSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.uuid = subscriptionPlan.getUuid();
        this.planName = subscriptionPlan.getName();
        this.price = subscriptionPlan.getPrice();
        this.currency = subscriptionPlan.getCurrency();
        this.maxStudents = subscriptionPlan.getMaxStudents();
    }
}
