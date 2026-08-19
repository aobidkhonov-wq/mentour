package uz.tune.mentourBiz.external.octo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.SubscriptionPlan;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;

import java.util.UUID;

@Entity
@Table(name = "school_subscription_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscriptionPayment extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = true)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = true)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    private Integer months;

    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    private String octoPaymentUuid;

    private String shopTransactionId;

    @Column(name = "sello_transaction_id")
    private String selloTransactionId;

    @Column(name = "ofb_transaction_id")
    private String ofbTransactionId;

    @Column(name = "uzum_transaction_id")
    private String uzumTransactionId;

    @Column(name = "ofd_url", columnDefinition = "TEXT")
    private String ofdUrl;
}