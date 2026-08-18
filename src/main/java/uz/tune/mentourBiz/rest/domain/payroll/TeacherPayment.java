package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.finance.SchoolExpense;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Money actually handed to a teacher, in one go.
 *
 * <p>A payment is made against the teacher's balance rather than against a particular month: an admin
 * says "I gave Aziz 3 000 000", not "I paid off his March payslip". Which payslips that settles is
 * worked out afterwards and recorded in {@link TeacherPaymentAllocation} — oldest open month first, so a
 * remainder carried over from a previous month is always cleared before the current one.
 *
 * <p>The payment never exceeds the balance. Paying an advance therefore requires the month to have been
 * approved first: approval is what puts the money on the ledger for it to be drawn from.
 */
@Entity
@Table(name = "teacher_payments")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherPayment extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.TeacherPaymentType type = PayrollEnums.TeacherPaymentType.ADVANCE;

    // Always positive. Its effect on the balance is negative; the sign lives on the ledger entry.
    @Column(name = "amount", nullable = false)
    private Long amount = 0L;

    @Column(name = "method")
    @Enumerated(EnumType.STRING)
    private FinanceEnums.PaymentMethod method;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "note", length = 1000)
    private String note;

    // The school's cash-flow row for this payment, created alongside it so a salary shows up in the
    // expense report next to the rent.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private SchoolExpense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeacherPaymentAllocation> allocations = new ArrayList<>();

    // Soft-delete. A reversed payment puts its amount back on the balance and releases the payslips it
    // had settled; the row itself stays for audit.
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;
}
