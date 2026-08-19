package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One teacher's pay for one month, frozen at the moment it was generated.
 *
 * <p>The live calculation in {@code TeacherPayrollService} always reflects today's data, which makes it
 * useless as a record of what someone was actually paid: editing an old transaction would silently
 * change a salary that has already been handed over. Generating a payslip snapshots those numbers into
 * rows that never move again once the payslip leaves DRAFT.
 */
@Table(
        name = "teacher_payslips",
        uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "period_year", "period_month"})
)
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherPayslip extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    // The plan the teacher was on when the payslip was generated, kept for the record even if they
    // move to a different plan later.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_plan_id")
    private SalaryPlan salaryPlan;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.PayslipStatus status = PayrollEnums.PayslipStatus.DRAFT;

    @Column(name = "total_earnings")
    private Long totalEarnings = 0L;

    @Column(name = "total_deductions")
    private Long totalDeductions = 0L;

    // totalEarnings - totalDeductions, stored so reports never have to re-derive it.
    @Column(name = "net_pay")
    private Long netPay = 0L;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private FinanceEnums.PaymentMethod paymentMethod;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_id")
    private User paidBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(mappedBy = "payslip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeacherPayslipLine> lines = new ArrayList<>();
}
