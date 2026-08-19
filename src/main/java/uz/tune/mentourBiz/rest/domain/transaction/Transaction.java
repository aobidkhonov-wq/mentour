package uz.tune.mentourBiz.rest.domain.transaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.transaction.payment.PaymentOrder;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "transactions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

    @Column(name = "transaction_id")
    private String transactionId = UUID.randomUUID().toString();

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "lesson_count")
    private Integer lessonCount;

    @Column(name = "course_uuid")
    private UUID courseUUID;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "commission")
    private Long commission;

    @Column(name = "ofd_url")
    private String ofdUrl;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "pay_time")
    private LocalDateTime payTime;

    @Column(name = "description")
    private String description;

    @Column(name = "paid_lesson_count")
    private Integer paidLessonCount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id")
    private PaymentOrder paymentOrder;
}
