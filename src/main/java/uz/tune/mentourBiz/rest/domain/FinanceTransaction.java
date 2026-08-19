package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.util.UUID;

@Entity
@Table(name = "finance_transactions")
@Getter
@Setter
public class FinanceTransaction extends BaseEntity {

    // Stable public id the front-end passes to the delete endpoint; the numeric id is never exposed.
    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    private Student student;

    // The group this transaction is attributed to, when it originates from a group-scoped billing
    // action (LESSON_PACK per-lesson charge, MONTHLY charge/renewal, or a per-lesson refund). Null for
    // wallet-level movements that are not tied to a single group (manual top-ups, adjustments, initial
    // charges). Used by teacher payroll to split revenue per group.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    private Long amount;

    // The part of the full price the student was let off by their StudentDiscount, always positive.
    // {@code amount} is what actually moved on the balance, so the undiscounted price of a charge is
    // {@code -amount + discountAmount}. Teacher payroll adds this back so a discount granted by the
    // school never comes out of the teacher's cut. 0 for everything that carries no discount.
    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount = 0L;

    @Enumerated(EnumType.STRING)
    private FinanceEnums.FinanceTransactionType type;

    @Enumerated(EnumType.STRING)
    private FinanceEnums.PaymentMethod method;

    // Who registered this movement. On a soft-delete it is overwritten with the user who deleted it,
    // so the record carries the last person who acted on it.
    @ManyToOne(fetch = FetchType.LAZY)
    private User acceptedBy;

    private String note;

    // Soft-delete flag. Never hard-deleted: the row is kept for audit and its balance effect is reversed
    // when the flag is set. Deleted rows still surface in the charge/payment history list (flagged), but
    // every revenue sum, payroll replay and status check filters them out with `deleted = false`.
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
