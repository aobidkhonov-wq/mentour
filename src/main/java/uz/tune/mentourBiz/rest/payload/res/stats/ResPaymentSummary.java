package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResPaymentSummary {

    private Long totalCollected;
    private Long totalCharged;
    private Long outstanding;
    private Long paymentCount;
    private Long studentsPaid;
    private List<ResPaymentMethodStat> byMethod;

}
