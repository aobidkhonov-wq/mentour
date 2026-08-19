package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResSchoolSubscriptionPayment {
    private UUID paymentUuid;
    private String schoolName;
    private String organizationName;
    private String planName;
    private Long amount;
    private Integer months;
    private TransactionStatus status;
    private java.time.Instant createdAt;
    private String ofdUrl;

    public ResSchoolSubscriptionPayment(SchoolSubscriptionPayment p) {
        this.paymentUuid = p.getUuid();
        this.schoolName = p.getSchool().getName();
        this.planName = p.getPlan().getName();
        this.amount = p.getTotalAmount();
        this.months = p.getMonths();
        this.status = p.getStatus();
        this.createdAt = p.getCreatedAt();
        this.ofdUrl = p.getOfdUrl();

        if (p.getSchool().getOrganization() != null) {
            this.organizationName = p.getSchool().getOrganization().getName();
        }
    }
}