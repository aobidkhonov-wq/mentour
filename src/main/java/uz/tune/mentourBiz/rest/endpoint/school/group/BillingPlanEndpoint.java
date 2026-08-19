package uz.tune.mentourBiz.rest.endpoint.school.group;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqBillingPlan;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.group.enrollment.ResBillingPlan;
import uz.tune.mentourBiz.rest.repository.group.BillingPlanRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/billing-plans")
@RequiredArgsConstructor
public class BillingPlanEndpoint {

    private final BillingPlanRepository billingPlanRepository;
    private final UserScopeService userScopeService;
    private final SchoolRepo schoolRepo;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResBillingPlan>> list(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) UUID schoolUuid) {
        UUID currentSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
        UUID scopedSchool;
        if (schoolUuid == null) {
            scopedSchool = userScopeService.resolveSchoolUuid(currentSchoolUuid);
        }else {
            scopedSchool = schoolUuid;
        }

        List<BillingPlan> plans;
        if (scopedSchool != null) {
            plans = activeOnly
                    ? billingPlanRepository.findAllBySchool_UuidAndIsActiveTrue(scopedSchool)
                    : billingPlanRepository.findAllBySchool_Uuid(scopedSchool);
        } else {
            plans = activeOnly
                    ? billingPlanRepository.findAllByIsActiveTrue()
                    : billingPlanRepository.findAll();
        }
        return ResponseEntity.ok(plans.stream().map(ResBillingPlan::new).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResBillingPlan> create(@RequestBody ReqBillingPlan req) {
        validate(req);
        BillingPlan plan = new BillingPlan();
        plan.setSchool(resolveSchool(req.getSchoolUuid()));
        apply(plan, req);
        billingPlanRepository.save(plan);
        return ResponseEntity.ok(new ResBillingPlan(plan));
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResBillingPlan> update(@PathVariable UUID uuid, @RequestBody ReqBillingPlan req) {
//        validate(req);
        BillingPlan plan = findScoped(uuid);
        apply(plan, req);
        billingPlanRepository.save(plan);
        return ResponseEntity.ok(new ResBillingPlan(plan));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        BillingPlan plan = findScoped(uuid);

        // Soft-disable rather than hard delete: existing enrollments still reference this plan.
        plan.setIsActive(Boolean.FALSE);
        billingPlanRepository.save(plan);
        return ResponseEntity.ok(new ResponseMessage("Billing plan deactivated."));
    }

    // Loads a plan, enforcing that a SCHOOL_ADMIN can only touch plans of their own school.
    private BillingPlan findScoped(UUID uuid) {
        UUID scopedSchool = userScopeService.resolveSchoolUuid(null);
        return (scopedSchool != null
                ? billingPlanRepository.findByUuidAndSchool_Uuid(uuid, scopedSchool)
                : billingPlanRepository.findByUuid(uuid))
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.BILLING_PLAN_NOT_FOUND.getKey()));
    }

    // Resolves the school a plan must belong to: SCHOOL_ADMIN -> their own school (requestUuid ignored);
    // SYS_ADMIN -> the requested school, which is mandatory since the plan cannot be school-less.
    private School resolveSchool(UUID requestUuid) {
        UUID schoolUuid = userScopeService.resolveSchoolUuid(requestUuid);
        if (schoolUuid == null) {
            throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }
        return schoolRepo.findByUuid(schoolUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
    }

    private void validate(ReqBillingPlan req) {
        if (req.getType() == null) {
            throw new ValidationException("Billing plan type is required.");
        }
        if (req.getPrice() == null || req.getPrice() < 0) {
            throw new ValidationException("price is required.");
        }
        if (req.getType() == BillingPlanType.FIXED_MONTHLY && (req.getCountMonth() == null || req.getCountMonth() <= 0)) {
            throw new ValidationException("countMonth is required for a MONTHLY plan.");
        }
    }

    private void apply(BillingPlan plan, ReqBillingPlan req) {
        boolean monthly = req.getType() == BillingPlanType.FIXED_MONTHLY;
        if (req.getName() != null) {
            plan.setName(req.getName());
        }
        if (req.getType() != null) {
            plan.setType(req.getType());
        }
        if (req.getPrice() != null) {
            plan.setPrice(req.getPrice());
        }
        if (req.getCountMonth() != null) {
            plan.setCountMonth(monthly ? req.getCountMonth() : null);
        }
        if (req.getLessonCount() != null) {
            plan.setLessonCount(monthly ? req.getLessonCount() : null);
        }
        if (req.getIsActive() != null) plan.setIsActive(req.getIsActive());
    }
}
