package uz.tune.mentourBiz.rest.service.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.payroll.PayrollEvent;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqPayrollEvent;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollEvent;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollEventsOverview;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.payroll.PayrollEventRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/**
 * The payroll History feed: every lesson that earned money, bonus awarded, deduction applied and manual
 * correction, plus the endpoint an admin uses to add one.
 */
@Service
@RequiredArgsConstructor
public class PayrollEventService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    private final PayrollEventRepository payrollEventRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final StudentRepo studentRepo;
    private final UserScopeService userScopeService;
    private final UserService userService;
    private final TeacherPayslipService teacherPayslipService;

    /** The filtered, paginated feed. Every filter is optional. */
    @Transactional(readOnly = true)
    public Page<ResPayrollEvent> list(List<UUID> teacherUuids,
                                      List<PayrollEnums.PayrollEventType> eventTypes,
                                      List<UUID> groupUuids,
                                      UUID studentUuid,
                                      UUID addedByUuid,
                                      Integer year,
                                      Integer month,
                                      LocalDate fromDate,
                                      LocalDate toDate,
                                      String search,
                                      Pageable pageable) {

        Instant from = fromDate != null ? fromDate.atStartOfDay(UZ_ZONE).toInstant() : null;
        // Inclusive end date: everything up to the last instant of that day.
        Instant toExclusive = toDate != null ? toDate.plusDays(1).atStartOfDay(UZ_ZONE).toInstant() : null;

        return payrollEventRepository.findWithFilters(
                        userScopeService.getAuthorizedSchoolUuids(),
                        emptyToNull(teacherUuids),
                        emptyToNull(eventTypes),
                        emptyToNull(groupUuids),
                        studentUuid,
                        addedByUuid,
                        year, month,
                        from, toExclusive,
                        blankToNull(search),
                        pageable)
                .map(PayrollEventMapper::toResponse);
    }

    /** The KPI cards and the event-type legend. */
    @Transactional(readOnly = true)
    public ResPayrollEventsOverview overview(Integer year, Integer month) {
        Map<PayrollEnums.PayrollEventType, Long> counts = new EnumMap<>(PayrollEnums.PayrollEventType.class);
        for (Object[] row : payrollEventRepository.countByType(
                userScopeService.getAuthorizedSchoolUuids(), year, month)) {
            counts.put((PayrollEnums.PayrollEventType) row[0], ((Number) row[1]).longValue());
        }

        long lessons = counts.getOrDefault(PayrollEnums.PayrollEventType.LESSON_EARNING, 0L);
        long bonuses = counts.getOrDefault(PayrollEnums.PayrollEventType.BONUS, 0L);
        long deductions = counts.getOrDefault(PayrollEnums.PayrollEventType.DEDUCTION, 0L);
        long adjustments = counts.getOrDefault(PayrollEnums.PayrollEventType.ADJUSTMENT, 0L);
        long total = lessons + bonuses + deductions + adjustments;

        return ResPayrollEventsOverview.builder()
                .totalEvents(total)
                .lessonEarnings(lessons)
                .lessonEarningsPercent(share(lessons, total))
                .bonuses(bonuses)
                .bonusesPercent(share(bonuses, total))
                .deductions(deductions)
                .deductionsPercent(share(deductions, total))
                .adjustments(adjustments)
                .adjustmentsPercent(share(adjustments, total))
                .build();
    }

    /**
     * Book a bonus, a deduction or a correction by hand. The teacher's draft payslip for the period is
     * rebuilt straight away so the totals include it; booking against an already approved or paid month
     * has no effect on what was signed off — issue it against the current period instead.
     */
    @Transactional
    public ResPayrollEvent create(ReqPayrollEvent req) {
        if (req == null || req.getTeacherUuid() == null) {
            throw new ValidationException("teacherUuid is required.");
        }
        if (req.getEventType() == null || req.getEventType() == PayrollEnums.PayrollEventType.LESSON_EARNING) {
            throw new ValidationException("eventType must be BONUS, DEDUCTION or ADJUSTMENT.");
        }
        if (req.getAmount() == null || req.getAmount() == 0) {
            throw new ValidationException("amount is required and cannot be zero.");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ValidationException("title is required.");
        }

        Teacher teacher = teacherRepository.findByUser_Uuid(req.getTeacherUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey()));
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        if (authorized != null && (teacher.getSchool() == null
                || !authorized.contains(teacher.getSchool().getUuid()))) {
            throw new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey());
        }

        YearMonth period = TeacherPayslipService.resolvePeriod(req.getYear(), req.getMonth());

        // "Deduct 50 000" is how people say it, so a deduction is accepted positive and stored negative.
        long amount = req.getEventType() == PayrollEnums.PayrollEventType.DEDUCTION
                ? -Math.abs(req.getAmount())
                : req.getAmount();

        PayrollEvent event = new PayrollEvent();
        event.setSchool(teacher.getSchool());
        event.setTeacher(teacher);
        event.setEventType(req.getEventType());
        event.setTitle(req.getTitle().trim());
        event.setSubtitle(req.getSubtitle());
        event.setAmount(amount);
        event.setCategory(req.getCategory() != null ? req.getCategory() : defaultCategory(req.getEventType(), amount));
        event.setNote(req.getNote());
        event.setPeriodYear(period.getYear());
        event.setPeriodMonth(period.getMonthValue());
        event.setOccurredAt(Instant.now());
        event.setAddedBy(userService.getCurrentUser());

        if (req.getGroupUuid() != null) {
            event.setGroup(groupRepository.findByUuid(req.getGroupUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey())));
        }
        if (req.getStudentUuid() != null) {
            event.setStudent(studentRepo.findByUser_Uuid(req.getStudentUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey())));
        }

        ResPayrollEvent response = PayrollEventMapper.toResponse(payrollEventRepository.save(event));
        teacherPayslipService.refreshDraft(teacher, period);
        return response;
    }

    /**
     * Remove a manually booked event. Events payroll generated itself are rebuilt from the calculation
     * and cannot be deleted on their own, and neither can anything already tied to a signed-off payslip.
     */
    @Transactional
    public ResponseMessage delete(UUID uuid) {
        PayrollEvent event = payrollEventRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Payroll event not found."));

        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        if (authorized != null && (event.getSchool() == null
                || !authorized.contains(event.getSchool().getUuid()))) {
            throw new EntityNotFoundException("Payroll event not found.");
        }
        if (event.getAddedBy() == null) {
            throw new ValidationException("System generated events cannot be deleted; regenerate the payslip instead.");
        }
        if (event.getPayslip() != null
                && event.getPayslip().getStatus() != PayrollEnums.PayslipStatus.DRAFT) {
            throw new ValidationException("This event belongs to a payslip that is no longer a draft.");
        }

        Teacher teacher = event.getTeacher();
        Integer year = event.getPeriodYear();
        Integer month = event.getPeriodMonth();

        payrollEventRepository.delete(event);
        // The payslip was built with this event in it, so it has to come back out of the totals.
        payrollEventRepository.flush();
        if (year != null && month != null) {
            teacherPayslipService.refreshDraft(teacher, YearMonth.of(year, month));
        }
        return new ResponseMessage("Payroll event deleted.");
    }

    private static PayrollEnums.PayslipLineCategory defaultCategory(
            PayrollEnums.PayrollEventType type, long amount) {
        if (type == PayrollEnums.PayrollEventType.BONUS) return PayrollEnums.PayslipLineCategory.BONUS;
        if (type == PayrollEnums.PayrollEventType.DEDUCTION) return PayrollEnums.PayslipLineCategory.OTHER_DEDUCTION;
        return amount >= 0
                ? PayrollEnums.PayslipLineCategory.OTHER_EARNINGS
                : PayrollEnums.PayslipLineCategory.OTHER_DEDUCTION;
    }

    private static Double share(long part, long total) {
        return total > 0 ? Math.round(part * 1000.0 / total) / 10.0 : 0.0;
    }

    private static <T> Collection<T> emptyToNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
