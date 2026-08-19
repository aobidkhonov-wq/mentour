package uz.tune.mentourBiz.rest.payload.req.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqTransactionChangeStatus {

    private String orderId; // IT'S OUR TRANSACTION ID

    private String status;

    private Long amount;

    private String cardType;

    private String ofdUrl;

    private String description;

    private Long orderCreatedTime;

    private String maskedCardNumber;
}
