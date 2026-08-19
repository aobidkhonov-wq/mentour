package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.UUID;

/**
 * A single line on a payslip — "Regular Lessons 2 640 000", "Tax (10%) 320 000". Amounts are always
 * stored positive; {@code kind} says whether the line adds to or subtracts from the total.
 */
@Table(name = "teacher_payslip_lines")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherPayslipLine extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id")
    private TeacherPayslip payslip;

    @Column(name = "line_kind")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.PayslipLineKind kind;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.PayslipLineCategory category;

    // What the payslip shows for this line, e.g. "Tax (10%)" or "Missing Reports".
    @Column(name = "label")
    private String label;

    @Column(name = "amount")
    private Long amount = 0L;

    // How many times the underlying thing happened — lessons taught, reports missed. Null when the
    // line is a single lump sum.
    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
