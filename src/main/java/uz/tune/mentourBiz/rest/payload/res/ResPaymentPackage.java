package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ResPaymentPackage {
    private UUID uuid;
    private String name;
    private Long price;
    private FinanceEnums.FinanceCurrency currency;
    private Integer dueDate;
    private Integer courseCount;
    private Integer studentCount;
    private List<ResCourseView> resGroup;

    // Coin/som billing model fields (populated only when this DTO is built from a BillingPlan).
    private BillingPlanType type;
    private Integer lessonCount;
    private Integer countMonth;

    public ResPaymentPackage(PaymentPackage pkg, Integer courseCount, Long studentCount, List<Course> course) {
        this.uuid = pkg.getUuid();
        this.name = pkg.getName();
        this.price = pkg.getPrice();
        this.dueDate = pkg.getPaymentDueDate();
        this.currency = pkg.getCurrency();
        this.courseCount = courseCount;
        this.studentCount = studentCount != null ? studentCount.intValue() : 0;

        if (course != null) {
            this.resGroup = course.stream().map(ResCourseView::new).toList();
        } else {
            this.resGroup = new ArrayList<>();
        }
    }

    public ResPaymentPackage(PaymentPackage pg) {
        this.name = pg.getName();
        this.price = pg.getPrice();
        this.dueDate = pg.getPaymentDueDate();
        this.currency = pg.getCurrency();
    }

    // Coin/som billing model: the plan the student is actually enrolled on (Enrollment.billingPlan).
    // BillingPlan has no currency/dueDate, so those stay defaulted; price is in som (UZS).
    public ResPaymentPackage(BillingPlan plan) {
        this.uuid = plan.getUuid();
        this.name = plan.getName();
        this.price = plan.getPrice();
        this.currency = FinanceEnums.FinanceCurrency.UZS;
        this.type = plan.getType();
        this.lessonCount = plan.getLessonCount();
        this.countMonth = plan.getCountMonth();
    }
}