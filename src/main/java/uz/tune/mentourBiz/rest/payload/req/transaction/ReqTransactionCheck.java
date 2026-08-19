package uz.tune.mentourBiz.rest.payload.req.transaction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqTransactionCheck {

    private String orderId;

    private Long amount;
}
