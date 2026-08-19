package uz.tune.mentourBiz.rest.payload.res.payments;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResInitiatePayment {
    private String paymentUrl;
}