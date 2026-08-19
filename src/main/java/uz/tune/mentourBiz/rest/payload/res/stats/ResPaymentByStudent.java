package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResPaymentByStudent {

    private UUID studentUuid;
    private String studentName;
    private Long totalCharged;
    private Long totalPaid;
    private Long balance;
    private Instant lastPaymentAt;

}
