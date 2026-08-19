package uz.tune.mentourBiz.external.payload.res;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.external.payload.enums.OrderState;

@Getter
@Setter
public class ExtResOrderCreateResult {
    private String orderId;
    private String transactionId;
    private OrderState status;
    private String payLink;
    private String description;
    private Long orderCreatedTime;
}
