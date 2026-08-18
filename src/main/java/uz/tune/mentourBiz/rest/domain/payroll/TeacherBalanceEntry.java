package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.util.UUID;

/**
 * One movement on a teacher's balance. The balance itself is nothing but the sum of these rows.
 *
 * <p>It is deliberately not a column on {@code Teacher}: a stored figure can drift out of step with the
 * payslips and payments behind it, and when it does there is nothing to reconcile it against. Summing a
 * ledger is slower and always right.
 *
 * <p>The ledger runs continuously and is never zeroed at a month boundary. A teacher who was underpaid
 * in July walks into August with that remainder still owed, which is exactly what carrying the balance
 * over means.
 *
 * <p>{@code amount} is signed: an ACCRUAL is positive, a PAYMENT negative, a REVERSAL the negative of
 * the accrual it undoes.
 */
@Entity
@Table(name = "teacher_balance_entries")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherBalanceEntry extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(name = "entry_type")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.BalanceEntryType entryType;

    @Column(name = "amount", nullable = false)
    private Long amount = 0L;

    // The payslip whose approval created this entry. Set on ACCRUAL and REVERSAL, null on PAYMENT —
    // a payment is made against the balance as a whole, and which payslips it settled is recorded in
    // TeacherPaymentAllocation instead.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id")
    private TeacherPayslip payslip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private TeacherPayment payment;

    // Shown in the balance history, e.g. "Payslip approved — 2026-08".
    @Column(name = "title")
    private String title;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    // Null when payroll produced the entry rather than an admin acting directly.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
}
