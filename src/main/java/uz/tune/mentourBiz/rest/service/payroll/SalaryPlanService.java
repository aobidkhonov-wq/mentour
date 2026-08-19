package uz.tune.mentourBiz.rest.service.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.payroll.SalaryPlan;
import uz.tune.mentourBiz.rest.domain.payroll.SalaryPlanBonus;
import uz.tune.mentourBiz.rest.domain.payroll.SalaryPlanDeduction;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.TeacherSalaryPlan;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqSalaryPlan;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResSalaryPlan;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResSalaryPlansOverview;
import uz.tune.mentourBiz.rest.repository.payroll.SalaryPlanRepository;
import uz.tune.mentourBiz.rest.repository.payroll.TeacherPayslipRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherSalaryPlanRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/**
 * Manages the school's reusable salary plans — the templates behind the Plans screen — and who is on
 * them. Assigning a teacher copies the plan's structure onto their personal
 * {@link TeacherSalaryPlan}, which is what the monthly calculation actually reads; that keeps the
 * calculation independent of the template and lets one teacher be nudged off it without disturbing the
 * others.
 */
@Service
@RequiredArgsConstructor
public class SalaryPlanService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    private final SalaryPlanRepository salaryPlanRepository;
    private final TeacherSalaryPlanRepository teacherSalaryPlanRepository;
    private final TeacherPayslipRepository payslipRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolRepo schoolRepo;
    private final UserScopeService userScopeService;

    @Transactional(readOnly = true)
    public Page<ResSalaryPlan> list(PayrollEnums.SalaryPlanStatus status,
                                    PayrollEnums.SalaryPlanType planType,
                                    String search,
                                    Pageable pageable) {
        Page<SalaryPlan> plans = salaryPlanRepository.findWithFilters(
                userScopeService.getAuthorizedSchoolUuids(), status, planType, blankToNull(search), pageable);

        Map<UUID, Long> teacherCounts = teacherCounts(plans.getContent());
        Map<UUID, Long> impacts = monthlyImpacts(plans.getContent());
        return plans.map(plan -> toResponse(plan, teacherCounts, impacts, false));
    }

    @Transactional(readOnly = true)
    public ResSalaryPlan get(UUID uuid) {
        SalaryPlan plan = loadInScope(uuid);
        List<SalaryPlan> one = List.of(plan);
        return toResponse(plan, teacherCounts(one), monthlyImpacts(one), true);
    }

    /** The KPI cards above the plan list, over every plan the caller can see. */
    @Transactional(readOnly = true)
    public ResSalaryPlansOverview overview() {
        List<SalaryPlan> plans = salaryPlanRepository.findActive(userScopeService.getAuthorizedSchoolUuids());
        Map<UUID, Long> teacherCounts = teacherCounts(plans);
        Map<UUID, Long> impacts = monthlyImpacts(plans);

        long assigned = teacherCounts.values().stream().mapToLong(Long::longValue).sum();
        long impact = impacts.values().stream().mapToLong(Long::longValue).sum();

        return ResSalaryPlansOverview.builder()
                .totalPlans((long) plans.size())
                .assignedTeachers(assigned)
                .monthlyPayrollImpact(impact)
                .averageMonthlyCostPerTeacher(assigned > 0 ? Math.round((double) impact / assigned) : 0L)
                .build();
    }

    @Transactional
    public ResSalaryPlan create(ReqSalaryPlan req) {
        if (req == null || isBlank(req.getName())) {
            throw new ValidationException("Salary plan name is required.");
        }
        SalaryPlan plan = new SalaryPlan();
        plan.setSchool(resolveSchool(req.getSchoolUuid()));
        apply(plan, req);
        return get(salaryPlanRepository.save(plan).getUuid());
    }

    @Transactional
    public ResSalaryPlan update(UUID uuid, ReqSalaryPlan req) {
        SalaryPlan plan = loadInScope(uuid);
        if (req == null) throw new ValidationException("Request body is required.");
        if (isBlank(req.getName())) throw new ValidationException("Salary plan name is required.");
        apply(plan, req);
        salaryPlanRepository.save(plan);
        return get(uuid);
    }

    /** Copy a plan, including its bonuses and deductions, as a fresh ACTIVE plan with nobody on it. */
    @Transactional
    public ResSalaryPlan duplicate(UUID uuid) {
        SalaryPlan source = loadInScope(uuid);

        SalaryPlan copy = new SalaryPlan();
        copy.setSchool(source.getSchool());
        copy.setName(source.getName() + " (copy)");
        copy.setDescription(source.getDescription());
        copy.setPlanType(source.getPlanType());
        copy.setStatus(PayrollEnums.SalaryPlanStatus.ACTIVE);
        copy.setFixedMonthlySalary(source.getFixedMonthlySalary());
        copy.setPercentOfLessonValue(source.getPercentOfLessonValue());
        copy.setFixedAmountPerLesson(source.getFixedAmountPerLesson());
        copy.setMinimumLessonsRequirement(source.getMinimumLessonsRequirement());
        copy.setCalculationMode(source.getCalculationMode());
        copy.setAppliesToAllGroups(source.getAppliesToAllGroups());
        copy.setTaxPercent(source.getTaxPercent());

        for (SalaryPlanBonus bonus : source.getBonuses()) {
            SalaryPlanBonus b = new SalaryPlanBonus();
            b.setSalaryPlan(copy);
            b.setName(bonus.getName());
            b.setDescription(bonus.getDescription());
            b.setAmount(bonus.getAmount());
            b.setAutomatic(bonus.getAutomatic());
            copy.getBonuses().add(b);
        }
        for (SalaryPlanDeduction deduction : source.getDeductions()) {
            SalaryPlanDeduction d = new SalaryPlanDeduction();
            d.setSalaryPlan(copy);
            d.setName(deduction.getName());
            d.setDescription(deduction.getDescription());
            d.setAmount(deduction.getAmount());
            d.setCategory(deduction.getCategory());
            d.setAutomatic(deduction.getAutomatic());
            copy.getDeductions().add(d);
        }

        return get(salaryPlanRepository.save(copy).getUuid());
    }

    /**
     * Archive a plan. Teachers stay on the salary they were given — archiving stops the plan being
     * handed out again, it does not cut anyone's pay.
     */
    @Transactional
    public ResponseMessage archive(UUID uuid) {
        SalaryPlan plan = loadInScope(uuid);
        plan.setStatus(PayrollEnums.SalaryPlanStatus.ARCHIVED);
        salaryPlanRepository.save(plan);
        return new ResponseMessage("Salary plan archived.");
    }

    /** The "Assigned Teachers" tab. */
    @Transactional(readOnly = true)
    public List<AssignedTeacher> assignedTeachers(UUID uuid) {
        loadInScope(uuid);
        List<AssignedTeacher> result = new ArrayList<>();
        for (TeacherSalaryPlan assignment : teacherSalaryPlanRepository.findAllBySalaryPlan_Uuid(uuid)) {
            Teacher teacher = assignment.getTeacher();
            if (teacher == null || teacher.getUser() == null) continue;
            result.add(new AssignedTeacher(
                    teacher.getUser().getUuid(),
                    teacher.getUser().getFirstName() + " " + teacher.getUser().getLastName(),
                    assignment.getFixedSalary(),
                    assignment.getPercentPerGroup(),
                    assignment.getFixedPerStudent(),
                    assignment.getIsActive() == null || assignment.getIsActive()));
        }
        return result;
    }

    /**
     * Put teachers on a plan, copying its structure onto each of them. Anything they had configured
     * personally is replaced — being on a plan means being paid what the plan says.
     */
    @Transactional
    public ResponseMessage assignTeachers(UUID uuid, List<UUID> teacherUuids) {
        SalaryPlan plan = loadInScope(uuid);
        if (plan.getStatus() == PayrollEnums.SalaryPlanStatus.ARCHIVED) {
            throw new ValidationException("An archived plan cannot be assigned to teachers.");
        }
        if (teacherUuids == null || teacherUuids.isEmpty()) {
            throw new ValidationException("teacherUuids is required.");
        }

        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        int assigned = 0;
        for (UUID teacherUuid : new LinkedHashSet<>(teacherUuids)) {
            Teacher teacher = teacherRepository.findByUser_Uuid(teacherUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey()));
            if (authorized != null && (teacher.getSchool() == null
                    || !authorized.contains(teacher.getSchool().getUuid()))) {
                throw new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey());
            }

            TeacherSalaryPlan assignment = teacherSalaryPlanRepository.findByTeacher_User_Uuid(teacherUuid)
                    .orElseGet(TeacherSalaryPlan::new);
            assignment.setTeacher(teacher);
            assignment.setSalaryPlan(plan);
            assignment.setFixedSalary(nz(plan.getFixedMonthlySalary()));
            assignment.setPercentPerGroup(nz(plan.getPercentOfLessonValue()));
            assignment.setFixedPerStudent(nz(plan.getFixedAmountPerLesson()));
            assignment.setIsActive(Boolean.TRUE);
            teacherSalaryPlanRepository.save(assignment);
            assigned++;
        }
        return new ResponseMessage(assigned + " teacher(s) assigned to " + plan.getName() + ".");
    }

    // ---- internals -------------------------------------------------------------------------------

    private void apply(SalaryPlan plan, ReqSalaryPlan req) {
        plan.setName(req.getName().trim());
        plan.setDescription(req.getDescription());
        plan.setPlanType(req.getPlanType() != null ? req.getPlanType() : PayrollEnums.SalaryPlanType.PERCENTAGE);
        plan.setStatus(req.getStatus() != null ? req.getStatus() : PayrollEnums.SalaryPlanStatus.ACTIVE);
        plan.setFixedMonthlySalary(nonNegative(req.getFixedMonthlySalary(), "fixedMonthlySalary"));
        plan.setPercentOfLessonValue(percent(req.getPercentOfLessonValue(), "percentOfLessonValue"));
        plan.setFixedAmountPerLesson(nonNegative(req.getFixedAmountPerLesson(), "fixedAmountPerLesson"));
        int minimumLessons = nz(req.getMinimumLessonsRequirement());
        if (minimumLessons < 0) throw new ValidationException("minimumLessonsRequirement cannot be negative.");
        plan.setMinimumLessonsRequirement(minimumLessons);
        plan.setCalculationMode(req.getCalculationMode() != null
                ? req.getCalculationMode() : PayrollEnums.CalculationMode.PER_COMPLETED_LESSON);
        plan.setAppliesToAllGroups(req.getAppliesToAllGroups() == null || req.getAppliesToAllGroups());
        plan.setTaxPercent(percent(req.getTaxPercent(), "taxPercent"));

        // orphanRemoval clears out whatever is no longer listed.
        plan.getBonuses().clear();
        if (req.getBonuses() != null) {
            for (ReqSalaryPlan.Component c : req.getBonuses()) {
                if (c == null || isBlank(c.getName())) continue;
                SalaryPlanBonus bonus = new SalaryPlanBonus();
                bonus.setSalaryPlan(plan);
                bonus.setName(c.getName().trim());
                bonus.setDescription(c.getDescription());
                bonus.setAmount(nonNegative(c.getAmount(), "bonuses.amount"));
                bonus.setAutomatic(Boolean.TRUE.equals(c.getAutomatic()));
                plan.getBonuses().add(bonus);
            }
        }

        plan.getDeductions().clear();
        if (req.getDeductions() != null) {
            for (ReqSalaryPlan.Component c : req.getDeductions()) {
                if (c == null || isBlank(c.getName())) continue;
                SalaryPlanDeduction deduction = new SalaryPlanDeduction();
                deduction.setSalaryPlan(plan);
                deduction.setName(c.getName().trim());
                deduction.setDescription(c.getDescription());
                deduction.setAmount(nonNegative(c.getAmount(), "deductions.amount"));
                deduction.setCategory(c.getCategory() != null
                        ? c.getCategory() : PayrollEnums.PayslipLineCategory.OTHER_DEDUCTION);
                deduction.setAutomatic(Boolean.TRUE.equals(c.getAutomatic()));
                plan.getDeductions().add(deduction);
            }
        }
    }

    private ResSalaryPlan toResponse(SalaryPlan plan, Map<UUID, Long> teacherCounts,
                                     Map<UUID, Long> impacts, boolean withComponents) {
        ResSalaryPlan.ResSalaryPlanBuilder builder = ResSalaryPlan.builder()
                .uuid(plan.getUuid())
                .name(plan.getName())
                .description(plan.getDescription())
                .planType(plan.getPlanType())
                .status(plan.getStatus())
                .schoolUuid(plan.getSchool() != null ? plan.getSchool().getUuid() : null)
                .teacherCount(teacherCounts.getOrDefault(plan.getUuid(), 0L))
                .monthlyImpact(impacts.getOrDefault(plan.getUuid(), 0L))
                .fixedMonthlySalary(nz(plan.getFixedMonthlySalary()))
                .percentOfLessonValue(nz(plan.getPercentOfLessonValue()))
                .fixedAmountPerLesson(nz(plan.getFixedAmountPerLesson()))
                .minimumLessonsRequirement(nz(plan.getMinimumLessonsRequirement()))
                .calculationMode(plan.getCalculationMode())
                .appliesToAllGroups(plan.getAppliesToAllGroups())
                .taxPercent(nz(plan.getTaxPercent()))
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt());

        if (withComponents) {
            List<ResSalaryPlan.Component> bonuses = new ArrayList<>();
            for (SalaryPlanBonus bonus : plan.getBonuses()) {
                bonuses.add(ResSalaryPlan.Component.builder()
                        .uuid(bonus.getUuid())
                        .name(bonus.getName())
                        .description(bonus.getDescription())
                        .amount(nz(bonus.getAmount()))
                        .automatic(bonus.getAutomatic())
                        .build());
            }
            List<ResSalaryPlan.Component> deductions = new ArrayList<>();
            for (SalaryPlanDeduction deduction : plan.getDeductions()) {
                deductions.add(ResSalaryPlan.Component.builder()
                        .uuid(deduction.getUuid())
                        .name(deduction.getName())
                        .description(deduction.getDescription())
                        .amount(nz(deduction.getAmount()))
                        .automatic(deduction.getAutomatic())
                        .category(deduction.getCategory())
                        .build());
            }
            builder.bonuses(bonuses).deductions(deductions);
        }
        return builder.build();
    }

    private Map<UUID, Long> teacherCounts(List<SalaryPlan> plans) {
        if (plans.isEmpty()) return Map.of();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : salaryPlanRepository.countTeachersByPlan(uuids(plans))) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** What each plan actually cost this month, read off the payslips generated for it. */
    private Map<UUID, Long> monthlyImpacts(List<SalaryPlan> plans) {
        if (plans.isEmpty()) return Map.of();
        YearMonth now = YearMonth.now(UZ_ZONE);
        Map<UUID, Long> impacts = new HashMap<>();
        for (Object[] row : payslipRepository.sumNetPayByPlan(uuids(plans), now.getYear(), now.getMonthValue())) {
            impacts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return impacts;
    }

    private static List<UUID> uuids(List<SalaryPlan> plans) {
        return plans.stream().map(SalaryPlan::getUuid).toList();
    }

    private SalaryPlan loadInScope(UUID uuid) {
        SalaryPlan plan = salaryPlanRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.BILLING_PLAN_NOT_FOUND.getKey()));
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        boolean visible = authorized == null
                || (plan.getSchool() != null && authorized.contains(plan.getSchool().getUuid()));
        if (!visible) throw new EntityNotFoundException(MessageKey.BILLING_PLAN_NOT_FOUND.getKey());
        return plan;
    }

    private School resolveSchool(UUID requestUuid) {
        UUID schoolUuid = userScopeService.resolveSchoolUuid(requestUuid);
        if (schoolUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        return schoolRepo.findByUuid(schoolUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
    }

    private static long nonNegative(Long value, String field) {
        long v = value != null ? value : 0L;
        if (v < 0) throw new ValidationException(field + " cannot be negative.");
        return v;
    }

    private static int percent(Integer value, String field) {
        int v = value != null ? value : 0;
        if (v < 0 || v > 100) throw new ValidationException(field + " must be between 0 and 100.");
        return v;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    /** A row of the plan's "Assigned Teachers" tab. */
    public record AssignedTeacher(UUID teacherUuid, String teacherName, Long fixedSalary,
                                  Integer percentPerGroup, Long fixedPerStudent, Boolean active) {
    }
}
