package uz.tune.mentourBiz.rest.payload.req.transaction;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqPaymentOrderCreate {
    private UUID studentUuid;
    private UUID courseUuid;
    private Integer lessonsToCharge;
    private Long pricePerLesson;
}