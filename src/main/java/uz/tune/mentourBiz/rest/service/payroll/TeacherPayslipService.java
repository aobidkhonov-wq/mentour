package uz.tune.mentourBiz.rest.service.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.payroll.*;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.TeacherSalaryPlan;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqGeneratePayslips;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqPayslipPayment;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollEvent;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollOverview;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayslipDetail;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayslipSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherGroupPayroll;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherPayroll;
import uz.tune.mentourBiz.rest.repository.payroll.PayrollEventRepository;
import uz.tune.mentourBiz.rest.repository.payroll.TeacherPayslipRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherSalaryPlanRepository;
import uz.tune.mentourBiz.rest.service.TeacherPayrollService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/**
 * Turns the live monthly calculation into payslips that can be reviewed, approved and paid, and serves
 * the payroll Overview screen.
 *
 * <p>The distinction that matters: {@code TeacherPayrollService} answers "what does this teacher earn
 * for this month, as of right now", which changes every time a transaction or lesson is edited. A
 * payslip freezes that answer. Once it is approved the stored figures are what the school owes, no
 * matter what happens to the underlying data afterwards.
 */
@Service
@RequiredArgsConstructor
public class TeacherPayslipService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");
    private static final String CURRENCY_WORD = "sum";

    private final TeacherPayslipRepository payslipRepository;
    private final PayrollEventRepository payrollEventRepository;
    private final TeacherSalaryPlanRepository teacherSalaryPlanRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherPayrollService teacherPayrollService;
    private final UserScopeService userScopeService;
    private final UserService userService;

    // ---- Overview --------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ResPayrollOverview overview(Integer year, Integer month) {
        YearMonth ym = resolvePeriod(year, month);
        List<UUID> schools = userScopeService.getAuthorizedSchoolUuids();

        List<TeacherPayslip> payslips = payslipRepository.findAllForPeriod(ym.getYear(), ym.getMonthValue(), schools);
        YearMonth previous = ym.minusMonths(1);
        List<TeacherPayslip> previousPayslips =
                payslipRepository.findAllForPeriod(previous.getYear(), previous.getMonthValue(), schools);

        long total = payslips.stream().mapToLong(p -> nz(p.getNetPay())).sum();
        long previousTotal = previousPayslips.stream().mapToLong(p -> nz(p.getNetPay())).sum();

        long paid = sumWhere(payslips, PayrollEnums.PayslipStatus.PAID);
        long pending = sumWhere(payslips, PayrollEnums.PayslipStatus.PENDING);
        long paidTeachers = payslips.stream()
                .filter(p -> p.getStatus() == PayrollEnums.PayslipStatus.PAID).count();

        long activeTeachers = countActiveTeachers(schools);
        long withPayslip = payslips.size();

        return ResPayrollOverview.builder()
                .period(ym.toString())
                .totalPayroll(total)
                .changePercentVsLastMonth(previousTotal > 0
                        ? Math.round((total - previousTotal) * 1000.0 / previousTotal) / 10.0 : null)
                .totalTeachers(withPayslip)
                .activeTeachers(activeTeachers)
                .paidAmount(paid)
                .paidPercent(share(paid, total))
                .pendingApprovalAmount(pending)
                .pendingApprovalPercent(share(pending, total))
                .paidTeacherCount(paidTeachers)
                .missingPayslipCount(Math.max(0, activeTeachers - withPayslip))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ResPayslipSummary> list(Integer year, Integer month, PayrollEnums.PayslipStatus status,
                                        String search, Pageable pageable) {
        YearMonth ym = resolvePeriod(year, month);
        return payslipRepository.findForPeriod(
                        ym.getYear(), ym.getMonthValue(), userScopeService.getAuthorizedSchoolUuids(),
                        status, blankToNull(search), pageable)
                .map(TeacherPayslipService::toSummary);
    }

    /**
     * The Paycheck Details panel. A draft is rebuilt from today's data before it is returned: the panel
     * shows the stored lines next to the live per-group breakdown, and a draft that was generated before
     * a transaction, a lesson or a bonus landed would show a Summary tab that contradicts its own Lessons
     * tab. Anything past DRAFT has been signed off and is returned exactly as it was frozen.
     */
    @Transactional
    public ResPayslipDetail detail(UUID payslipUuid) {
        TeacherPayslip payslip = loadInScope(payslipUuid);

        ResTeacherPayroll payroll = null;
        if (payslip.getStatus() == PayrollEnums.PayslipStatus.DRAFT
                && payslip.getTeacher() != null && payslip.getTeacher().getUser() != null) {
            payroll = buildPayslip(payslip.getTeacher(), payslip,
                    YearMonth.of(payslip.getPeriodYear(), payslip.getPeriodMonth()));
        }
        return toDetail(payslip, payroll);
    }

    /**
     * The payslip's History tab: every payroll event booked against the same teacher and period,
     * newest first. Split out of {@link #detail} because a busy month runs to hundreds of rows — a
     * payslip is opened far more often than its history is read through.
     *
     * <p>{@code eventTypes} narrows the feed to bonuses, deductions and so on; an empty or absent list
     * means all of them. The teacher and the period are the payslip's own and cannot be widened here —
     * {@code /payroll/events} is the feed that spans teachers.
     */
    @Transactional(readOnly = true)
    public Page<ResPayrollEvent> history(UUID payslipUuid,
                                         List<PayrollEnums.PayrollEventType> eventTypes,
                                         Pageable pageable) {
        TeacherPayslip payslip = loadInScope(payslipUuid);
        UUID teacherUuid = teacherUuid(payslip);
        if (teacherUuid == null) return Page.empty(pageable);

        // No school filter: loadInScope has already refused a payslip outside the caller's schools, and
        // an event whose school was never set would drop out of a school-filtered query.
        return payrollEventRepository.findWithFilters(
                        null, List.of(teacherUuid), emptyToNull(eventTypes), null, null, null,
                        payslip.getPeriodYear(), payslip.getPeriodMonth(), null, null, null, pageable)
                .map(PayrollEventMapper::toResponse);
    }

    // ---- Generation ------------------------------------------------------------------------------

    /**
     * Build payslips for a month. Teachers who already have one are left alone unless
     * {@code regenerateDrafts} is set, and even then an approved or paid payslip is never touched.
     */
    @Transactional
    public ResponseMessage generate(ReqGeneratePayslips req) {
        YearMonth ym = resolvePeriod(req != null ? req.getYear() : null, req != null ? req.getMonth() : null);
        boolean regenerate = req != null && Boolean.TRUE.equals(req.getRegenerateDrafts());

        List<Teacher> teachers = resolveTeachers(req);
        if (teachers.isEmpty()) {
            return new ResponseMessage("No teachers in scope for " + ym + ".");
        }

        int created = 0;
        int refreshed = 0;
        int skipped = 0;
        for (Teacher teacher : teachers) {
            UUID teacherUuid = teacher.getUser().getUuid();
            Optional<TeacherPayslip> existingOpt = payslipRepository
                    .findByTeacher_User_UuidAndPeriodYearAndPeriodMonth(teacherUuid, ym.getYear(), ym.getMonthValue());

            if (existingOpt.isPresent()) {
                TeacherPayslip existing = existingOpt.get();
                if (!regenerate || existing.getStatus() != PayrollEnums.PayslipStatus.DRAFT) {
                    skipped++;
                    continue;
                }
                buildPayslip(teacher, existing, ym);
                refreshed++;
            } else {
                TeacherPayslip payslip = new TeacherPayslip();
                payslip.setTeacher(teacher);
                payslip.setSchool(teacher.getSchool());
                payslip.setStatus(PayrollEnums.PayslipStatus.DRAFT);
                buildPayslip(teacher, payslip, ym);
                created++;
            }
        }

        return new ResponseMessage(String.format(
                "%s: %d payslip(s) created, %d refreshed, %d left as they were.",
                ym, created, refreshed, skipped));
    }

    /**
     * Rebuild a teacher's draft payslip after something it is derived from changed — a bonus or a
     * deduction booked by hand, for instance. Without this a bonus added after generation would show up
     * on the Bonuses tab while the totals kept ignoring it.
     *
     * <p>Only a draft is touched: an approved or paid payslip is a record of what was signed off and must
     * not move. A teacher who has no payslip for the period is not given one either — generating is an
     * explicit action, and the event is picked up whenever that happens.
     */
    @Transactional
    public void refreshDraft(Teacher teacher, YearMonth ym) {
        if (teacher == null || teacher.getUser() == null || ym == null) return;
        payslipRepository.findByTeacher_User_UuidAndPeriodYearAndPeriodMonth(
                        teacher.getUser().getUuid(), ym.getYear(), ym.getMonthValue())
                .filter(payslip -> payslip.getStatus() == PayrollEnums.PayslipStatus.DRAFT)
                .ifPresent(payslip -> buildPayslip(teacher, payslip, ym));
    }

    /**
     * Fill a payslip from the live calculation and the period's manual payroll events, returning the
     * calculation it was built from so a caller that also needs the per-group breakdown does not have to
     * run the whole month a second time.
     */
    private ResTeacherPayroll buildPayslip(Teacher teacher, TeacherPayslip payslip, YearMonth ym) {
        UUID teacherUuid = teacher.getUser().getUuid();
        ResTeacherPayroll payroll = teacherPayrollService.computeMonthly(teacherUuid, ym.getYear(), ym.getMonthValue());
        TeacherSalaryPlan assignment = teacherSalaryPlanRepository.findByTeacher_User_Uuid(teacherUuid).orElse(null);
        SalaryPlan plan = assignment != null ? assignment.getSalaryPlan() : null;

        payslip.setSalaryPlan(plan);
        payslip.setPeriodYear(ym.getYear());
        payslip.setPeriodMonth(ym.getMonthValue());
        payslip.setPeriodStart(ym.atDay(1));
        payslip.setPeriodEnd(ym.atEndOfMonth());
        payslip.setGeneratedAt(Instant.now());
        payslip.getLines().clear();

        // ---- Earnings, straight from the monthly calculation ----
        long regular = 0L;
        long substitute = 0L;
        long lessonsTaught = 0L;
        for (ResTeacherGroupPayroll group : safe(payroll.getGroups())) {
            long groupSalary = nz(group.getGroupSalary());
            if (Boolean.TRUE.equals(group.getSubstitute())) {
                substitute += groupSalary;
            } else {
                regular += groupSalary;
            }
            lessonsTaught += nz(group.getTeacherLessons());
        }

        addLine(payslip, PayrollEnums.PayslipLineKind.EARNING, PayrollEnums.PayslipLineCategory.FIXED_SALARY,
                "Fixed Salary", nz(payroll.getFixedSalary()), null, 10);
        addLine(payslip, PayrollEnums.PayslipLineKind.EARNING, PayrollEnums.PayslipLineCategory.REGULAR_LESSONS,
                "Regular Lessons", regular, lessonsTaught, 20);
        addLine(payslip, PayrollEnums.PayslipLineKind.EARNING, PayrollEnums.PayslipLineCategory.SUBSTITUTE_LESSONS,
                "Substitute Lessons", substitute, null, 30);

        // ---- Manual events booked against this period ----
        // Only the hand-entered ones: the generated rows are rebuilt below and already sit inside the
        // group figures above, so counting them here would pay everything twice.
        List<PayrollEvent> manualEvents = payrollEventRepository
                .findAllByTeacher_User_UuidAndPeriodYearAndPeriodMonthOrderByOccurredAtDesc(
                        teacherUuid, ym.getYear(), ym.getMonthValue())
                .stream()
                .filter(event -> event.getAddedBy() != null)
                .toList();

        for (PayrollEvent event : manualEvents) {
            long amount = nz(event.getAmount());
            if (amount == 0) continue;

            if (amount > 0) {
                PayrollEnums.PayslipLineCategory category = event.getCategory() != null ? event.getCategory()
                        : (event.getEventType() == PayrollEnums.PayrollEventType.BONUS
                                ? PayrollEnums.PayslipLineCategory.BONUS
                                : PayrollEnums.PayslipLineCategory.OTHER_EARNINGS);
                addLine(payslip, PayrollEnums.PayslipLineKind.EARNING, category,
                        event.getTitle(), amount, null, 40);
            } else {
                PayrollEnums.PayslipLineCategory category = event.getCategory() != null ? event.getCategory()
                        : PayrollEnums.PayslipLineCategory.OTHER_DEDUCTION;
                addLine(payslip, PayrollEnums.PayslipLineKind.DEDUCTION, category,
                        event.getTitle(), -amount, null, 60);
            }
        }

        // ---- What the plan pays or withholds on its own ----
        // Only the components flagged automatic: the rest describe something an admin has to judge
        // ("Student Retention Bonus"), and they reach the payslip as a hand-entered event above.
        if (plan != null) {
            for (SalaryPlanBonus bonus : plan.getBonuses()) {
                if (!Boolean.TRUE.equals(bonus.getAutomatic())) continue;
                addLine(payslip, PayrollEnums.PayslipLineKind.EARNING, PayrollEnums.PayslipLineCategory.BONUS,
                        bonus.getName(), nz(bonus.getAmount()), null, 45);
            }
            for (SalaryPlanDeduction deduction : plan.getDeductions()) {
                if (!Boolean.TRUE.equals(deduction.getAutomatic())) continue;
                PayrollEnums.PayslipLineCategory category = deduction.getCategory() != null
                        ? deduction.getCategory() : PayrollEnums.PayslipLineCategory.OTHER_DEDUCTION;
                addLine(payslip, PayrollEnums.PayslipLineKind.DEDUCTION, category,
                        deduction.getName(), nz(deduction.getAmount()), null, 55);
            }
        }

        // ---- Tax, withheld on everything earned above ----
        long totalEarnings = sumLines(payslip, PayrollEnums.PayslipLineKind.EARNING);
        int taxPercent = plan != null ? nz(plan.getTaxPercent()) : 0;
        if (taxPercent > 0 && totalEarnings > 0) {
            addLine(payslip, PayrollEnums.PayslipLineKind.DEDUCTION, PayrollEnums.PayslipLineCategory.TAX,
                    "Tax (" + taxPercent + "%)", Math.round(totalEarnings * taxPercent / 100.0), null, 50);
        }

        long totalDeductions = sumLines(payslip, PayrollEnums.PayslipLineKind.DEDUCTION);
        payslip.setTotalEarnings(totalEarnings);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetPay(totalEarnings - totalDeductions);
        // Saved before the events are written: they point at the payslip and need its id.
        payslip = payslipRepository.save(payslip);

        writeLessonEvents(teacher, payslip, payroll, ym);
        for (PayrollEvent event : manualEvents) {
            event.setPayslip(payslip);
        }
        payrollEventRepository.saveAll(manualEvents);
        return payroll;
    }

    /** Record one history entry per group that earned money, so the log explains the payslip. */
    private void writeLessonEvents(Teacher teacher, TeacherPayslip payslip, ResTeacherPayroll payroll, YearMonth ym) {
        UUID teacherUuid = teacher.getUser().getUuid();
        payrollEventRepository.deleteAllByTeacher_User_UuidAndPeriodYearAndPeriodMonthAndAddedByIsNull(
                teacherUuid, ym.getYear(), ym.getMonthValue());
        payrollEventRepository.flush();

        List<PayrollEvent> events = new ArrayList<>();
        for (ResTeacherGroupPayroll group : safe(payroll.getGroups())) {
            long amount = nz(group.getGroupSalary());
            if (amount == 0 && nz(group.getTeacherLessons()) == 0) continue;

            PayrollEvent event = new PayrollEvent();
            event.setSchool(teacher.getSchool());
            event.setTeacher(teacher);
            event.setEventType(PayrollEnums.PayrollEventType.LESSON_EARNING);
            event.setTitle(Boolean.TRUE.equals(group.getSubstitute())
                    ? "Substitute lessons conducted" : "Lessons conducted");
            event.setSubtitle(group.getGroupName());
            event.setAmount(amount);
            event.setStudentCount(group.getBilledStudentCount());
            event.setNote(group.getTeacherLessons() + " lesson(s) of " + group.getTotalLessons());
            event.setPayslip(payslip);
            event.setPeriodYear(ym.getYear());
            event.setPeriodMonth(ym.getMonthValue());
            event.setOccurredAt(ym.atEndOfMonth().atStartOfDay(UZ_ZONE).toInstant());
            events.add(event);
        }
        payrollEventRepository.saveAll(events);
    }

    // ---- Approval and payment --------------------------------------------------------------------

    /** Move a payslip on for review. */
    @Transactional
    public ResPayslipDetail submitForApproval(UUID payslipUuid) {
        TeacherPayslip payslip = loadInScope(payslipUuid);
        if (payslip.getStatus() != PayrollEnums.PayslipStatus.DRAFT) {
            throw new ValidationException("Only a draft payslip can be sent for approval.");
        }
        payslip.setStatus(PayrollEnums.PayslipStatus.PENDING);
        return toDetail(payslipRepository.save(payslip));
    }

    /** Sign off the figures. From here on they are frozen. */
    @Transactional
    public ResPayslipDetail approve(UUID payslipUuid) {
        TeacherPayslip payslip = loadInScope(payslipUuid);
        if (payslip.getStatus() != PayrollEnums.PayslipStatus.DRAFT
                && payslip.getStatus() != PayrollEnums.PayslipStatus.PENDING) {
            throw new ValidationException("Only a draft or pending payslip can be approved.");
        }
        payslip.setStatus(PayrollEnums.PayslipStatus.APPROVED);
        payslip.setApprovedBy(userService.getCurrentUser());
        payslip.setApprovedAt(Instant.now());
        return toDetail(payslipRepository.save(payslip));
    }

    /** Record that the money has gone out. */
    @Transactional
    public ResPayslipDetail markPaid(UUID payslipUuid, ReqPayslipPayment req) {
        TeacherPayslip payslip = loadInScope(payslipUuid);
        if (payslip.getStatus() != PayrollEnums.PayslipStatus.APPROVED) {
            throw new ValidationException("A payslip must be approved before it can be marked paid.");
        }
        payslip.setStatus(PayrollEnums.PayslipStatus.PAID);
        payslip.setPaymentMethod(req != null ? req.getPaymentMethod() : null);
        payslip.setPaymentDate(req != null && req.getPaymentDate() != null
                ? req.getPaymentDate() : LocalDate.now(UZ_ZONE));
        if (req != null && req.getNote() != null) payslip.setNote(req.getNote());
        payslip.setPaidBy(userService.getCurrentUser());
        payslip.setPaidAt(Instant.now());
        return toDetail(payslipRepository.save(payslip));
    }

    /** Send an approved payslip back to draft, e.g. when a mistake is spotted before payment. */
    @Transactional
    public ResPayslipDetail reopen(UUID payslipUuid) {
        TeacherPayslip payslip = loadInScope(payslipUuid);
        if (payslip.getStatus() == PayrollEnums.PayslipStatus.PAID) {
            throw new ValidationException("A paid payslip cannot be reopened; cancel it and issue a correction.");
        }
        payslip.setStatus(PayrollEnums.PayslipStatus.DRAFT);
        payslip.setApprovedBy(null);
        payslip.setApprovedAt(null);
        return toDetail(payslipRepository.save(payslip));
    }

    // ---- internals -------------------------------------------------------------------------------

    private List<Teacher> resolveTeachers(ReqGeneratePayslips req) {
        List<UUID> requested = req != null ? req.getTeacherUuids() : null;
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();

        if (requested != null && !requested.isEmpty()) {
            List<Teacher> teachers = new ArrayList<>();
            for (UUID uuid : new LinkedHashSet<>(requested)) {
                Teacher teacher = teacherRepository.findByUser_Uuid(uuid)
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey()));
                if (authorized != null && (teacher.getSchool() == null
                        || !authorized.contains(teacher.getSchool().getUuid()))) {
                    throw new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey());
                }
                teachers.add(teacher);
            }
            return teachers;
        }

        UUID requestedSchool = req != null ? req.getSchoolUuid() : null;
        List<UUID> schools = requestedSchool != null ? List.of(requestedSchool) : authorized;
        if (schools != null && schools.isEmpty()) return List.of();
        return teacherRepository.findWithFilters(schools, UserStatus.ACTIVE, null, Pageable.unpaged()).getContent();
    }

    private long countActiveTeachers(List<UUID> schools) {
        if (schools != null && schools.isEmpty()) return 0L;
        return teacherRepository.findWithFilters(schools, UserStatus.ACTIVE, null, Pageable.unpaged())
                .getTotalElements();
    }

    private static void addLine(TeacherPayslip payslip, PayrollEnums.PayslipLineKind kind,
                                PayrollEnums.PayslipLineCategory category, String label,
                                long amount, Long quantity, int sortOrder) {
        if (amount == 0) return;  // a zero line is noise on a payslip
        TeacherPayslipLine line = new TeacherPayslipLine();
        line.setPayslip(payslip);
        line.setKind(kind);
        line.setCategory(category);
        line.setLabel(label);
        line.setAmount(amount);
        line.setQuantity(quantity);
        line.setSortOrder(sortOrder);
        payslip.getLines().add(line);
    }

    private static long sumLines(TeacherPayslip payslip, PayrollEnums.PayslipLineKind kind) {
        return payslip.getLines().stream()
                .filter(l -> l.getKind() == kind)
                .mapToLong(l -> nz(l.getAmount()))
                .sum();
    }

    private static ResPayslipSummary toSummary(TeacherPayslip payslip) {
        return ResPayslipSummary.builder()
                .uuid(payslip.getUuid())
                .teacherUuid(teacherUuid(payslip))
                .teacherName(teacherName(payslip))
                .teacherInitials(initials(teacherName(payslip)))
                .periodStart(payslip.getPeriodStart())
                .periodEnd(payslip.getPeriodEnd())
                .totalPay(nz(payslip.getTotalEarnings()))
                .netPay(nz(payslip.getNetPay()))
                .status(payslip.getStatus())
                .build();
    }

    private ResPayslipDetail toDetail(TeacherPayslip payslip) {
        return toDetail(payslip, null);
    }

    /**
     * Map a payslip for the details panel. {@code prebuilt} is the monthly calculation when the caller
     * has just run it; passing it avoids computing the same month twice.
     */
    private ResPayslipDetail toDetail(TeacherPayslip payslip, ResTeacherPayroll prebuilt) {
        List<ResPayslipDetail.Line> earnings = new ArrayList<>();
        List<ResPayslipDetail.Line> deductions = new ArrayList<>();
        payslip.getLines().stream()
                .sorted(Comparator.comparing(l -> l.getSortOrder() != null ? l.getSortOrder() : 0))
                .forEach(line -> {
                    ResPayslipDetail.Line row = ResPayslipDetail.Line.builder()
                            .uuid(line.getUuid())
                            .category(line.getCategory())
                            .label(line.getLabel())
                            .amount(nz(line.getAmount()))
                            .quantity(line.getQuantity())
                            .note(line.getNote())
                            .build();
                    if (line.getKind() == PayrollEnums.PayslipLineKind.DEDUCTION) {
                        deductions.add(row);
                    } else {
                        earnings.add(row);
                    }
                });

        UUID teacherUuid = teacherUuid(payslip);
        YearMonth ym = YearMonth.of(payslip.getPeriodYear(), payslip.getPeriodMonth());

        List<PayrollEvent> events = teacherUuid != null
                ? payrollEventRepository.findAllByTeacher_User_UuidAndPeriodYearAndPeriodMonthOrderByOccurredAtDesc(
                        teacherUuid, payslip.getPeriodYear(), payslip.getPeriodMonth())
                : List.of();

        // The Lessons tab shows the live per-group breakdown; on an approved payslip it is context for
        // the frozen figures rather than their source.
        List<ResTeacherGroupPayroll> groups = List.of();
        if (prebuilt != null) {
            groups = safe(prebuilt.getGroups());
        } else if (teacherUuid != null) {
            ResTeacherPayroll payroll = teacherPayrollService.computeMonthly(
                    teacherUuid, payslip.getPeriodYear(), payslip.getPeriodMonth());
            groups = safe(payroll.getGroups());
        }

        String planName = payslip.getSalaryPlan() != null ? payslip.getSalaryPlan().getName() : null;
        String name = teacherName(payslip);

        return ResPayslipDetail.builder()
                .uuid(payslip.getUuid())
                .teacherUuid(teacherUuid)
                .teacherName(name)
                .teacherInitials(initials(name))
                .teacherTitle(planName)
                .period(ym.toString())
                .periodStart(payslip.getPeriodStart())
                .periodEnd(payslip.getPeriodEnd())
                .status(payslip.getStatus())
                .paymentMethod(payslip.getPaymentMethod())
                .paymentDate(payslip.getPaymentDate())
                .salaryPlanUuid(payslip.getSalaryPlan() != null ? payslip.getSalaryPlan().getUuid() : null)
                .salaryPlanName(planName)
                .earnings(earnings)
                .deductions(deductions)
                .totalEarnings(nz(payslip.getTotalEarnings()))
                .totalDeductions(nz(payslip.getTotalDeductions()))
                .netPay(nz(payslip.getNetPay()))
                .netPayInWords(NumberToWords.spell(nz(payslip.getNetPay()), CURRENCY_WORD))
                .groups(groups)
                .bonuses(mapEvents(events, PayrollEnums.PayrollEventType.BONUS))
                .adjustments(mapEvents(events, PayrollEnums.PayrollEventType.ADJUSTMENT))
                .note(payslip.getNote())
                .generatedAt(payslip.getGeneratedAt())
                .approvedByName(userName(payslip.getApprovedBy()))
                .approvedAt(payslip.getApprovedAt())
                .paidByName(userName(payslip.getPaidBy()))
                .paidAt(payslip.getPaidAt())
                .build();
    }

    private static List<ResPayrollEvent> mapEvents(
            List<PayrollEvent> events, PayrollEnums.PayrollEventType type) {
        return PayrollEventMapper.toResponses(
                events.stream().filter(e -> e.getEventType() == type).toList());
    }

    private TeacherPayslip loadInScope(UUID uuid) {
        TeacherPayslip payslip = payslipRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Payslip not found."));
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        boolean visible = authorized == null
                || (payslip.getSchool() != null && authorized.contains(payslip.getSchool().getUuid()));
        if (!visible) throw new EntityNotFoundException("Payslip not found.");
        return payslip;
    }

    static YearMonth resolvePeriod(Integer year, Integer month) {
        if (year == null || month == null) return YearMonth.now(UZ_ZONE);
        if (month < 1 || month > 12) throw new ValidationException("month must be between 1 and 12.");
        return YearMonth.of(year, month);
    }

    private static long sumWhere(List<TeacherPayslip> payslips, PayrollEnums.PayslipStatus status) {
        return payslips.stream().filter(p -> p.getStatus() == status).mapToLong(p -> nz(p.getNetPay())).sum();
    }

    private static Double share(long part, long total) {
        return total > 0 ? Math.round(part * 1000.0 / total) / 10.0 : 0.0;
    }

    private static UUID teacherUuid(TeacherPayslip payslip) {
        return payslip.getTeacher() != null && payslip.getTeacher().getUser() != null
                ? payslip.getTeacher().getUser().getUuid() : null;
    }

    private static String teacherName(TeacherPayslip payslip) {
        return payslip.getTeacher() != null ? userName(payslip.getTeacher().getUser()) : null;
    }

    static String userName(User user) {
        return user != null ? (user.getFirstName() + " " + user.getLastName()) : null;
    }

    static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.toString();
    }

    private static <T> List<T> safe(List<T> list) {
        return list != null ? list : List.of();
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static <T> Collection<T> emptyToNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list;
    }
}
