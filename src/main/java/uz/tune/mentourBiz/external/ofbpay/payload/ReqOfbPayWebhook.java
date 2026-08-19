package uz.tune.mentourBiz.external.ofbpay.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqOfbPayWebhook {

    private String method;

    private String orderId;

    private String transactionId;

    private Long amount;

    private String hashKey;

    // present only for the "accept" method: "success" or "failed"
    private String status;

    // present only for the "accept" method: unix timestamp
    private Long checkTime;
}
