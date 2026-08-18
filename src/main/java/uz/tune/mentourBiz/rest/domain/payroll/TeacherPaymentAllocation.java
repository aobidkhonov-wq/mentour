package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.UUID;

/**
 * How much of one payment went to settling one payslip.
 *
 * <p>Payments are made against a running balance and payslips are monthly, so the two do not line up:
 * a single 3 000 000 payment may clear the 800 000 still open from July and leave 2 200 000 against
 * August. Without this split neither payslip could tell whether it had been paid.
 */
@Entity
@Table(name = "teacher_payment_allocations")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherPaymentAllocation extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private TeacherPayment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id")
    private TeacherPayslip payslip;

    // Positive; the allocations of one payment always add up to its amount.
    @Column(name = "amount", nullable = false)
    private Long amount = 0L;
}
