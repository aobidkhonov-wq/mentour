package uz.tune.mentourBiz.rest.domain.userManagement.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.DiscountType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

/**
 * A price break granted to one student. Optional — a student without a row here simply pays the full
 * billing-plan price. While the discount is in force, every billing-plan charge for that student is
 * reduced by {@link #discountOn(long)} before it hits their som balance.
 *
 * <p>A student may hold one discount of each {@link DiscountType} at a time — a fixed som amount, a
 * percentage, or both together (see {@link #totalDiscountOn(Collection, long)}).
 *
 * <p>The discount is a concession from the school to the family and is deliberately invisible to
 * teacher payroll: the charge row keeps the discounted part in {@code FinanceTransaction.discountAmount},
 * and payroll adds it back so the teacher is always paid on the full, undiscounted price.
 *
 * <p>A discount either runs for a fixed number of months ({@code durationMonths} = 3 → valid until
 * {@code startDate + 3 months}) or forever ({@code durationMonths} null, {@code endDate} null).
 */
@Table(name = "student_discounts")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentDiscount extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType type;

    // FIXED: som taken off each charge (e.g. 100000). Null for PERCENT.
    @Column(name = "amount")
    private Long amount;

    // PERCENT: 1..100, the share taken off each charge (e.g. 10). Null for FIXED.
    @Column(name = "percent")
    private Integer percent;

    // First day the discount applies. A future date means it is stored but not yet in force.
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate = LocalDate.now();

    // How many months it runs for. Null = permanent.
    @Column(name = "duration_months")
    private Integer durationMonths;

    // Exclusive end: the first day the discount no longer applies (startDate + durationMonths).
    // Null = permanent. Stored rather than recomputed so an already granted window never shifts when
    // the plan or the clock changes.
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** No end date — the discount runs until somebody switches it off. */
    public boolean isPermanent() {
        return endDate == null;
    }

    /** Whether the discount is switched on and {@code date} falls inside its window. */
    public boolean appliesOn(LocalDate date) {
        if (!Boolean.TRUE.equals(isActive) || date == null) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        return endDate == null || date.isBefore(endDate);
    }

    /**
     * The som taken off {@code price} by this one discount. Never negative and never more than the
     * price itself, so a 100 000 fixed discount against a 50 000 per-lesson charge just makes that
     * lesson free instead of paying the student.
     */
    public long discountOn(long price) {
        if (price <= 0) return 0L;
        long off = type == DiscountType.PERCENT
                ? Math.round(price * (percent != null ? percent : 0) / 100.0)
                : (amount != null ? amount : 0L);
        return Math.max(0L, Math.min(off, price));
    }

    /**
     * The som taken off {@code price} by all of a student's discounts together. A student may hold one
     * discount of each type at a time, and both then apply: the percentage is always worked out on the
     * full price and the fixed amount comes off as well, so 10% plus 100 000 off a 500 000 fee leaves
     * the student paying 350 000. The total is capped at the price, never turning into a payout.
     */
    public static long totalDiscountOn(Collection<StudentDiscount> discounts, long price) {
        if (price <= 0 || discounts == null || discounts.isEmpty()) return 0L;

        long total = 0L;
        for (StudentDiscount discount : discounts) {
            if (discount != null) total += discount.discountOn(price);
        }
        return Math.max(0L, Math.min(total, price));
    }
}
