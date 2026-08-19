package uz.tune.mentourBiz.rest.payload.req.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqPayoutDetails {
    private String recipientFullName;
    private String recipientAccount;
    private String recipientPinfl;
    private String bankMfo;
}