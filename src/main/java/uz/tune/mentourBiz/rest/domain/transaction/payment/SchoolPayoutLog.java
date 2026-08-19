package uz.tune.mentourBiz.rest.domain.transaction.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;

import java.util.UUID;

@Entity
@Table(name = "school_payout_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolPayoutLog extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "transaction_id")
    private String transactionId; // SELLO

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}