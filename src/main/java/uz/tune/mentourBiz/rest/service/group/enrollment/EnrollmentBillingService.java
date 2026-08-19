package uz.tune.mentourBiz.rest.service.group.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.domain.FinanceTransaction;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.repository.FinanceTransactionRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.student.StudentDiscountService;

/**
 * Charges a student's som balance ({@code currentBalance}) for a {@link BillingPlan} and records the
 * movement as a finance transaction. The balance may go negative (debt), after which the student
 * shows as overdue/UNPAID until it is topped up. Kept separate from {@code EnrollmentServiceImpl}
 * so the monthly-renewal scheduler can reuse it without pulling in the whole enrollment flow.
 *
 * <p>This is also the single place a student's discount is applied: the plan price is the full price,
 * the student is charged the price minus their discount, and the discounted part is stored on the
 * transaction so teacher payroll can still settle on the full price.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentBillingService {

    private final StudentRepo studentRepo;
    private final FinanceTransactionRepo financeTransactionRepo;
    private final StudentDiscountService studentDiscountService;

    /**
     * Deducts {@code plan.price} — less the student's discount, if they have one — from their som
     * balance and records a CHARGE transaction. Unlike a prepaid wallet, this always goes through: if
     * the balance is insufficient the student simply ends up in debt.
     *
     * @param group      the group this charge is attributed to (used by teacher payroll), or {@code null}.
     * @param acceptedBy the user performing the charge, or {@code null} for system/automated charges.
     * @return the som actually taken off the balance (the discounted amount), 0 for a free plan.
     */
    @Transactional
    public long chargeBillingPlan(Student student, BillingPlan plan, Group group, User acceptedBy, String note) {
        long price = plan.getPrice() != null ? plan.getPrice() : 0L;
        if (price <= 0) return 0L; // free plan, nothing to charge

        long discount = studentDiscountService.discountFor(student, price);
        long payable = price - discount;

        student.setCurrentBalance(student.getCurrentBalance() - payable);
        studentRepo.save(student);

        // A fully discounted charge is still recorded (amount 0, discount = price): it is what the
        // teacher is paid on, and it keeps the student's history explaining why nothing was billed.
        FinanceTransaction charge = new FinanceTransaction();
        charge.setStudent(student);
        charge.setGroup(group);
        charge.setAmount(-payable);
        charge.setDiscountAmount(discount);
        charge.setType(FinanceEnums.FinanceTransactionType.CHARGE);
        charge.setAcceptedBy(acceptedBy);
        charge.setNote(discount > 0 ? note + " — discount " + discount + " of " + price : note);
        financeTransactionRepo.save(charge);

        return payable;
    }

    /**
     * Credits {@code plan.price} — less the student's discount — back to the student's som balance and
     * records an ADJUSTMENT transaction. Used to reverse a per-lesson LESSON_PACK charge when an admin
     * excuses a lesson (e.g. a valid absence), so the student is not billed for a lesson that did not
     * count.
     *
     * <p>The discount is recomputed from what is in force now rather than read off the original charge.
     * A refund therefore matches the charge exactly in the normal case; if the discount changed between
     * the charge and the refund, the refund follows the current one.
     *
     * @param group      the group this refund is attributed to (used by teacher payroll), or {@code null}.
     * @param acceptedBy the user performing the refund, or {@code null} for system/automated refunds.
     * @return the som credited back to the balance, 0 for a free plan.
     */
    @Transactional
    public long refundBillingPlan(Student student, BillingPlan plan, Group group, User acceptedBy, String note) {
        long price = plan.getPrice() != null ? plan.getPrice() : 0L;
        if (price <= 0) return 0L; // free plan, nothing to refund

        long discount = studentDiscountService.discountFor(student, price);
        long refundable = price - discount;

        student.setCurrentBalance(student.getCurrentBalance() + refundable);
        studentRepo.save(student);

        FinanceTransaction refund = new FinanceTransaction();
        refund.setStudent(student);
        refund.setGroup(group);
        refund.setAmount(refundable);
        refund.setDiscountAmount(discount);
        refund.setType(FinanceEnums.FinanceTransactionType.ADJUSTMENT);
        refund.setAcceptedBy(acceptedBy);
        refund.setNote(discount > 0 ? note + " — discount " + discount + " of " + price : note);
        financeTransactionRepo.save(refund);

        return refundable;
    }
}
