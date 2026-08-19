package uz.tune.mentourBiz.rest.payload.req.student;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.DiscountType;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ReqStudentDiscount {

    // The student the discount belongs to. Required on create, ignored on update.
    private UUID studentUuid;

    private DiscountType type;

    // FIXED only: som taken off each charge, e.g. 100000.
    private Long amount;

    // PERCENT only: 1..100, e.g. 10 for "10% off".
    private Integer percent;

    // Defaults to today when omitted. A future date stores the discount without applying it yet.
    private LocalDate startDate;

    // How many months the discount runs for, e.g. 3. Ignored when permanent = true; when both are
    // omitted the discount is permanent.
    private Integer durationMonths;

    // true = runs until it is switched off. Wins over durationMonths.
    private Boolean permanent;

    private String note;

    // Switch the discount off without deleting it. Defaults to true on create.
    private Boolean isActive;
}
