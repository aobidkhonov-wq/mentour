package uz.tune.mentourBiz.rest.payload.res.payments;

import lombok.Getter;
import lombok.Setter;

import uz.tune.mentourBiz.rest.domain.transaction.payment.PaymentOrder;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ResPaymentOrder {

    private UUID uuid;
    private UUID studentUuid;
    private String studentFullName;
    private UUID courseUuid;
    private String courseName;
    private Integer lessonsToCharge;
    private Long pricePerLesson; // In tiyin/cents
    private Long totalAmount;    // In tiyin/cents
    private TransactionStatus status;
    private String ofdUrl;
    private Instant createdAt;
    private Instant paidAt;

    public ResPaymentOrder(PaymentOrder order) {
        this.uuid = order.getUuid();
        this.lessonsToCharge = order.getLessonsToCharge();
        this.pricePerLesson = order.getPricePerLesson();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.studentUuid = order.getStudent().getUser().getUuid();
        this.studentFullName = order.getStudent().getUser().getFirstName() + " " + order.getStudent().getUser().getLastName();
        this.courseUuid = order.getCourse().getUuid();
        this.courseName = order.getCourse().getName();
        this.ofdUrl = order.getOfdUrl();
    }
}
