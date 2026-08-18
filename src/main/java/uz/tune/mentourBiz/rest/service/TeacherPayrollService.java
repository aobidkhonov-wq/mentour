package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.TeacherGroupSalaryConfig;
import uz.tune.mentourBiz.rest.domain.userManagement.user.TeacherSalaryPlan;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.GroupStatus;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqTeacherSalaryPlan;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherGroupPayroll;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherPayroll;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherSalaryPlan;
import uz.tune.mentourBiz.rest.repository.FinanceTransactionRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherGroupSalaryConfigRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherSalaryPlanRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;


@Service
@RequiredArgsConstructor
public class TeacherPayrollService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    // Attendance that earns the teacher their per-student fee: the student turned up. Absences do not
    // count, even though the student is still billed for the lesson.
    private static final Set<AttendanceStatus> ATTENDANCE_EARNING_STATUSES =
            Set.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE);

    private final TeacherRepository teacherRepository;
    private final TeacherSalaryPlanRepository salaryPlanRepository;
    private final TeacherGroupSalaryConfigRepository groupSalaryConfigRepository;
    private final GroupRepository groupRepository;
    private final CourseLessonRepo courseLessonRepo;
    private final FinanceTransactionRepo financeTransactionRepo;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserScopeService userScopeService;


    @Transactional
    public ResTeacherSalaryPlan createSalaryPlan(ReqTeacherSalaryPlan req) {
        if (req == null || req.getTeacherUuid() == null) {
            throw new ValidationException("teacherUuid is required.");
        }
        Teacher teacher = loadTeacherInScope(req.getTeacherUuid());
        if (salaryPlanRepository.findByTeacher_User_Uuid(req.getTeacherUuid()).isPresent()) {
            throw new ValidationException(MessageKey.TEACHER_SALARY_PLAN_EXISTS.getKey());
        }
        return savePlan(teacher, new TeacherSalaryPlan(), req);
    }


    @Transactional
    public ResTeacherSalaryPlan updateSalaryPlan(UUID teacherUuid, ReqTeacherSalaryPlan req) {
        Teacher teacher = loadTeacherInScope(teacherUuid);
        TeacherSalaryPlan plan = salaryPlanRepository.findByTeacher_User_Uuid(teacherUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_SALARY_PLAN_NOT_FOUND.getKey()));
        return savePlan(teacher, plan, req != null ? req : new ReqTeacherSalaryPlan());
    }


    @Transactional
    public ResponseMessage deleteSalaryPlan(UUID teacherUuid) {
        loadTeacherInScope(teacherUuid);
        TeacherSalaryPlan plan = salaryPlanRepository.findByTeacher_User_Uuid(teacherUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_SALARY_PLAN_NOT_FOUND.getKey()));

        // Overrides point at the plan, so they have to go first.
        List<TeacherGroupSalaryConfig> overrides = groupSalaryConfigRepository.findAllBySalaryPlan_Uuid(plan.getUuid());
        if (!overrides.isEmpty()) {
            groupSalaryConfigRepository.deleteAll(overrides);
            groupSalaryConfigRepository.flush();
        }
        salaryPlanRepository.delete(plan);
        return new ResponseMessage("Teacher salary plan deleted.");
    }

    @Transactional(readOnly = true)
    public ResTeacherSalaryPlan getSalaryPlan(UUID teacherUuid) {
        Teacher teacher = loadTeacherInScope(teacherUuid);

        return salaryPlanRepository.findByTeacher_User_Uuid(teacherUuid)
                .map(plan -> toPlanResponse(plan, teacher,
                        groupSalaryConfigRepository.findAllBySalaryPlan_Uuid(plan.getUuid())))
                .orElseGet(() -> ResTeacherSalaryPlan.builder()
                        .teacherUuid(teacherUuid)
                        .teacherName(teacherName(teacher))
                        .configured(false)
                        .fixedSalary(0L)
                        .percentPerGroup(0)
                        .fixedPerStudent(0L)
                        .isActive(false)
                        .groupOverrides(List.of())
                        .build());
    }


    @Transactional(readOnly = true)
    public List<ResTeacherSalaryPlan> listSalaryPlans(UUID schoolUuid, boolean activeOnly) {
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();  // null == every school

        List<UUID> schools;
        if (schoolUuid != null) {
            if (authorized != null && !authorized.contains(schoolUuid)) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }
            schools = List.of(schoolUuid);
        } else {
            schools = authorized;
        }

        List<TeacherSalaryPlan> plans;
        if (schools == null) {
            plans = salaryPlanRepository.findAllWithTeacher();
        } else if (schools.isEmpty()) {
            return List.of();
        } else {
            plans = salaryPlanRepository.findAllByTeacher_School_UuidIn(schools);
        }
        if (activeOnly) {
            plans = plans.stream().filter(p -> p.getIsActive() == null || p.getIsActive()).toList();
        }
        if (plans.isEmpty()) return List.of();

        // Pull every plan's overrides in one query rather than one per plan.
        Map<UUID, List<TeacherGroupSalaryConfig>> overridesByPlan = new HashMap<>();
        for (TeacherGroupSalaryConfig cfg : groupSalaryConfigRepository.findAllBySalaryPlan_UuidIn(
                plans.stream().map(TeacherSalaryPlan::getUuid).toList())) {
            if (cfg.getSalaryPlan() == null) continue;
            overridesByPlan.computeIfAbsent(cfg.getSalaryPlan().getUuid(), k -> new ArrayList<>()).add(cfg);
        }

        return plans.stream()
                .filter(p -> p.getTeacher() != null)
                .map(p -> toPlanResponse(p, p.getTeacher(), overridesByPlan.getOrDefault(p.getUuid(), List.of())))
                .toList();
    }

    private ResTeacherSalaryPlan savePlan(Teacher teacher, TeacherSalaryPlan plan, ReqTeacherSalaryPlan req) {
        long fixedSalary = nonNegative(req.getFixedSalary(), "fixedSalary");
        int percentPerGroup = percent(req.getPercentPerGroup(), "percentPerGroup");
        long fixedPerStudent = nonNegative(req.getFixedPerStudent(), "fixedPerStudent");

        plan.setTeacher(teacher);
        plan.setFixedSalary(fixedSalary);
        plan.setPercentPerGroup(percentPerGroup);
        plan.setFixedPerStudent(fixedPerStudent);
        plan.setIsActive(req.getIsActive() == null || req.getIsActive());
        plan = salaryPlanRepository.save(plan);

        return toPlanResponse(plan, teacher, replaceGroupOverrides(plan, req.getGroupOverrides()));
    }

    private Teacher loadTeacherInScope(UUID teacherUuid) {
        if (teacherUuid == null) throw new ValidationException("teacherUuid is required.");

        Teacher teacher = teacherRepository.findByUser_Uuid(teacherUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey()));

        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();  // null == every school
        boolean visible = authorized == null
                || (teacher.getSchool() != null && authorized.contains(teacher.getSchool().getUuid()));
        if (!visible) {
            throw new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey());
        }
        return teacher;
    }

    private List<TeacherGroupSalaryConfig> replaceGroupOverrides(
            TeacherSalaryPlan plan, List<ReqTeacherSalaryPlan.GroupOverride> requested) {

        // Delete and flush before inserting: Hibernate orders inserts ahead of deletes within a
        // transaction, so re-saving an override for the same group would otherwise trip the
        // (salary_plan_id, group_id) unique constraint.
        List<TeacherGroupSalaryConfig> existing = groupSalaryConfigRepository.findAllBySalaryPlan_Uuid(plan.getUuid());
        if (!existing.isEmpty()) {
            groupSalaryConfigRepository.deleteAll(existing);
            groupSalaryConfigRepository.flush();
        }
        if (requested == null || requested.isEmpty()) return List.of();

        // Collapse duplicates up front — the last entry for a group wins.
        Map<UUID, Integer> percentByGroup = new LinkedHashMap<>();
        for (ReqTeacherSalaryPlan.GroupOverride ov : requested) {
            if (ov == null || ov.getGroupUuid() == null) continue;
            if (ov.getPercent() == null) {
                throw new ValidationException("groupOverrides.percent is required for each group override.");
            }
            percentByGroup.put(ov.getGroupUuid(), percent(ov.getPercent(), "groupOverrides.percent"));
        }

        List<TeacherGroupSalaryConfig> saved = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : percentByGroup.entrySet()) {
            Group group = groupRepository.findByUuid(entry.getKey())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
            TeacherGroupSalaryConfig cfg = new TeacherGroupSalaryConfig();
            cfg.setSalaryPlan(plan);
            cfg.setGroup(group);
            cfg.setOverridePercent(entry.getValue());
            saved.add(groupSalaryConfigRepository.save(cfg));
        }
        return saved;
    }

    private static ResTeacherSalaryPlan toPlanResponse(
            TeacherSalaryPlan plan, Teacher teacher, List<TeacherGroupSalaryConfig> overrides) {

        List<ResTeacherSalaryPlan.GroupOverride> rows = new ArrayList<>();
        for (TeacherGroupSalaryConfig cfg : overrides) {
            if (cfg.getGroup() == null) continue;
            rows.add(ResTeacherSalaryPlan.GroupOverride.builder()
                    .groupUuid(cfg.getGroup().getUuid())
                    .groupName(cfg.getGroup().getName())
                    .percent(cfg.getOverridePercent())
                    .build());
        }

        return ResTeacherSalaryPlan.builder()
                .teacherUuid(teacher.getUser() != null ? teacher.getUser().getUuid() : null)
                .teacherName(teacherName(teacher))
                .configured(true)
                .fixedSalary(nz(plan.getFixedSalary()))
                .percentPerGroup(nz(plan.getPercentPerGroup()))
                .fixedPerStudent(nz(plan.getFixedPerStudent()))
                .isActive(plan.getIsActive() == null || plan.getIsActive())
                .groupOverrides(rows)
                .build();
    }

    private static long nonNegative(Long value, String field) {
        long v = nz(value);
        if (v < 0) throw new ValidationException(field + " cannot be negative.");
        return v;
    }

    private static int percent(Integer value, String field) {
        int v = nz(value);
        if (v < 0 || v > 100) throw new ValidationException(field + " must be between 0 and 100.");
        return v;
    }


    @Transactional(readOnly = true)
    public ResTeacherPayroll computeMonthly(UUID teacherUuid, Integer year, Integer month) {
        Teacher teacher = loadTeacherInScope(teacherUuid);

        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now(UZ_ZONE);
        LocalDate first = ym.atDay(1);
        Instant from = first.atStartOfDay(UZ_ZONE).toInstant();
        Instant toExclusive = first.plusMonths(1).atStartOfDay(UZ_ZONE).toInstant();
        // Lesson counts cover the whole month, lessons still to come included. They used to stop at
        // "now", which made totalLessons a running tally rather than the month's lesson plan: it grew
        // with every day taught, so lessonShare and the per-student denominator both moved underneath
        // the salary all month. Over a full month the two windows agree; the difference only shows on
        // a month still in progress, where the per-student fee now fills in as the lessons are held
        // instead of being paid in full from the first week.

     Map<UUID, Group> groupById = new LinkedHashMap<>();
        for (Group g : groupRepository.findAllByTeacher_User_Uuid(teacherUuid)) {
            groupById.put(g.getUuid(), g);
        }
        List<UUID> conductedGroupUuids =
                courseLessonRepo.findGroupsConductedByTeacher(teacherUuid, from, toExclusive);
        List<UUID> missing = conductedGroupUuids.stream().filter(id -> !groupById.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            groupRepository.findAllByUuidIn(missing).forEach(g -> groupById.put(g.getUuid(), g));
        }

        Set<UUID> relevantGroupUuids = new LinkedHashSet<>(groupById.keySet());
        GroupRevenue revenue = relevantGroupUuids.isEmpty()
                ? GroupRevenue.empty()
                : settleRevenue(relevantGroupUuids, from, toExclusive);


        Map<UUID, Long> totalLessonsByGroup = relevantGroupUuids.isEmpty() ? Map.of()
                : toMap(courseLessonRepo.countLessonsByGroup(relevantGroupUuids, from, toExclusive));
        Map<UUID, Long> teacherLessonsByGroup = relevantGroupUuids.isEmpty() ? Map.of()
                : toMap(courseLessonRepo.countLessonsByGroupForTeacher(
                        relevantGroupUuids, teacherUuid, from, toExclusive));

        Map<UUID, Long> attendanceUnitsByGroup = relevantGroupUuids.isEmpty() ? Map.of()
                : toMap(attendanceRecordRepository.countAttendanceByGroupForTeacher(
                        relevantGroupUuids, teacherUuid, ATTENDANCE_EARNING_STATUSES, from, toExclusive));


        Optional<TeacherSalaryPlan> planOpt = salaryPlanRepository.findByTeacher_User_Uuid(teacherUuid);
        boolean configured = planOpt.isPresent();
        boolean active = configured && (planOpt.get().getIsActive() == null || planOpt.get().getIsActive());
        long fixedSalary = active ? nz(planOpt.get().getFixedSalary()) : 0L;
        int defaultPercent = active ? nz(planOpt.get().getPercentPerGroup()) : 0;
        long fixedPerStudent = active ? nz(planOpt.get().getFixedPerStudent()) : 0L;
        Map<UUID, Integer> overrides = new HashMap<>();
        if (active) {
            for (TeacherGroupSalaryConfig cfg : groupSalaryConfigRepository.findAllBySalaryPlan_Uuid(planOpt.get().getUuid())) {
                if (cfg.getGroup() != null && cfg.getOverridePercent() != null) {
                    overrides.put(cfg.getGroup().getUuid(), cfg.getOverridePercent());
                }
            }
        }

        List<ResTeacherGroupPayroll> groupRows = new ArrayList<>();
        long totalGroupBilled = 0L;
        long totalGroupCollected = 0L;
        long totalTeacherBilled = 0L;
        long totalTeacherCollected = 0L;
        long totalGroupSalary = 0L;
        for (Group group : groupById.values()) {
            UUID gid = group.getUuid();
            boolean isRegular = group.getTeacher() != null && group.getTeacher().getUser() != null
                    && teacherUuid.equals(group.getTeacher().getUser().getUuid());

            long groupBilled = revenue.billed().getOrDefault(gid, 0L);
            long groupCollected = revenue.collected().getOrDefault(gid, 0L);
            long totalLessons = totalLessonsByGroup.getOrDefault(gid, 0L);
            long teacherLessons = teacherLessonsByGroup.getOrDefault(gid, 0L);
            long attendanceUnits = attendanceUnitsByGroup.getOrDefault(gid, 0L);
            long billedStudents = revenue.students().getOrDefault(gid, Set.of()).size();


            boolean anyActivity = groupBilled != 0 || groupCollected != 0 || totalLessons > 0 || attendanceUnits > 0;
            if (!anyActivity && group.getGroupStatus() != GroupStatus.ACTIVE) continue;

            double share = totalLessons > 0 ? (double) teacherLessons / totalLessons : (isRegular ? 1.0 : 0.0);
            long teacherBilled = Math.round(groupBilled * share);
            long teacherCollected = Math.round(groupCollected * share);
            int percent = overrides.getOrDefault(gid, defaultPercent);

              Long revenueShareSalary = configured ? Math.round(teacherBilled * percent / 100.0) : null;
            Long perStudentSalary = configured
                    ? (totalLessons > 0 ? Math.round(fixedPerStudent * (double) attendanceUnits / totalLessons) : 0L)
                    : null;
            Long groupSalary = configured ? nz(revenueShareSalary) + nz(perStudentSalary) : null;

            totalGroupBilled += groupBilled;
            totalGroupCollected += groupCollected;
            totalTeacherBilled += teacherBilled;
            totalTeacherCollected += teacherCollected;
            if (groupSalary != null) totalGroupSalary += groupSalary;

            groupRows.add(ResTeacherGroupPayroll.builder()
                    .groupUuid(gid)
                    .groupName(group.getName())
                    .studentCount(group.getStudentCount() != null ? group.getStudentCount() : 0L)
                    .billedStudentCount(billedStudents)
                    .substitute(!isRegular)
                    .groupBilledRevenue(groupBilled)
                    .groupCollectedRevenue(groupCollected)
                    .totalLessons(totalLessons)
                    .teacherLessons(teacherLessons)
                    .lessonShare(Math.round(share * 10000.0) / 10000.0)
                    .teacherBilledRevenue(teacherBilled)
                    .teacherCollectedRevenue(teacherCollected)
                    .percent(percent)
                    .revenueShareSalary(revenueShareSalary)
                    .attendanceUnits(attendanceUnits)
                    .perStudentSalary(perStudentSalary)
                    .groupSalary(groupSalary)
                    .build());
        }

        return ResTeacherPayroll.builder()
                .teacherUuid(teacherUuid)
                .teacherName(teacherName(teacher))
                .period(ym.toString())
                .fixedSalary(configured ? fixedSalary : null)
                .defaultPercent(configured ? defaultPercent : null)
                .groups(groupRows)
                .totalBilledRevenue(totalGroupBilled)
                .totalCollectedRevenue(totalGroupCollected)
                .totalTeacherBilledRevenue(totalTeacherBilled)
                .totalTeacherCollectedRevenue(totalTeacherCollected)
                .totalGroupSalary(configured ? totalGroupSalary : null)
                .totalSalary(configured ? fixedSalary + totalGroupSalary : null)
                .salaryPlanConfigured(configured)
                .salaryPlanActive(configured ? active : null)
                .build();
    }

    record GroupRevenue(Map<UUID, Long> billed, Map<UUID, Long> collected, Map<UUID, Set<UUID>> students) {
        static GroupRevenue empty() {
            return new GroupRevenue(Map.of(), Map.of(), Map.of());
        }

        static GroupRevenue mutable() {
            return new GroupRevenue(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }

    private static final class Debt {
        private final UUID groupUuid;  // null for wallet-level charges that belong to no group
        private long remaining;

        private Debt(UUID groupUuid, long remaining) {
            this.groupUuid = groupUuid;
            this.remaining = remaining;
        }
    }

    private GroupRevenue settleRevenue(Set<UUID> groupUuids, Instant from, Instant toExclusive) {
        List<UUID> students = financeTransactionRepo.findStudentsBilledByGroups(groupUuids, toExclusive);
        if (students.isEmpty()) return GroupRevenue.empty();

        Map<UUID, List<Object[]>> ledgerByStudent = new LinkedHashMap<>();
        for (Object[] row : financeTransactionRepo.findLedgerForStudents(students, toExclusive)) {
            ledgerByStudent.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add(row);
        }

        GroupRevenue revenue = GroupRevenue.mutable();
        for (Map.Entry<UUID, List<Object[]>> entry : ledgerByStudent.entrySet()) {
            settleStudent(entry.getKey(), entry.getValue(), groupUuids, from, toExclusive, revenue);
        }
        return revenue;
    }

    static void settleStudent(UUID studentUuid, List<Object[]> ledger, Set<UUID> groups,
                              Instant from, Instant toExclusive, GroupRevenue out) {
        LinkedList<Debt> outstanding = new LinkedList<>();
        long credit = 0L;  // money paid in that no charge has claimed yet

        for (Object[] row : ledger) {
            UUID groupUuid = (UUID) row[1];
            FinanceEnums.FinanceTransactionType type = (FinanceEnums.FinanceTransactionType) row[2];
            long amount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            Instant at = (Instant) row[4];
            // What the student was let off by their discount. The teacher is paid on the full price, so
            // this is added back onto the charge below instead of shrinking the group's revenue.
            long discount = row.length > 5 && row[5] != null ? ((Number) row[5]).longValue() : 0L;
            boolean inWindow = at != null && !at.isBefore(from) && at.isBefore(toExclusive);

            switch (type) {
                case CHARGE -> {
                    long debt = -amount;  // charges are stored as negative amounts
                    long fullPrice = debt + discount;
                    if (fullPrice <= 0) break;
                    if (inWindow) {
                        addBilled(out, groups, groupUuid, fullPrice, studentUuid);
                        // The discounted part is never invoiced, so no payment will ever arrive for it.
                        // The school carries it, and it counts as collected the moment it is granted —
                        // that is what keeps the teacher whole on a discounted student.
                        if (discount > 0) addCollected(out, groups, groupUuid, discount);
                    }
                    if (debt <= 0) break;  // fully discounted: nothing left for the student to pay

                    // A student who paid ahead settles the charge the instant it appears.
                    long paidUpfront = Math.min(credit, debt);
                    if (paidUpfront > 0) {
                        credit -= paidUpfront;
                        if (inWindow) addCollected(out, groups, groupUuid, paidUpfront);
                    }
                    if (debt > paidUpfront) {
                        outstanding.addLast(new Debt(groupUuid, debt - paidUpfront));
                    }
                }
                case PAYMENT -> {
                    long money = amount;
                    if (money <= 0) break;
                    if (groupUuid != null) {
                        money = settleGroupDebt(outstanding, groupUuid, money, inWindow, out, groups);
                    }
                    while (money > 0 && !outstanding.isEmpty()) {
                        Debt debt = outstanding.getFirst();
                        long applied = Math.min(money, debt.remaining);
                        debt.remaining -= applied;
                        money -= applied;
                        if (debt.remaining == 0) outstanding.removeFirst();
                        if (inWindow) addCollected(out, groups, debt.groupUuid, applied);
                    }
                    credit += money;  // nothing left to settle: the student is paid ahead
                }
                case ADJUSTMENT -> {
                    long money = amount;
                    if (money <= 0 && discount <= 0) break;
                    if (groupUuid != null) {
                        if (inWindow) {
                            reduceBilled(out, groups, groupUuid, money + discount);
                            if (discount > 0) addCollected(out, groups, groupUuid, -discount);
                        }
                        if (money <= 0) break;  // fully discounted charge: no cash to give back
                        money = refundGroupDebt(outstanding, groupUuid, money);
                        if (money > 0) {
                            if (inWindow) addCollected(out, groups, groupUuid, -money);
                            credit += money;
                        }
                    } else {
                        writeOffDebt(outstanding, money);
                    }
                }
            }
        }
    }

    private static long settleGroupDebt(LinkedList<Debt> outstanding, UUID groupUuid, long money,
                                        boolean inWindow, GroupRevenue out, Set<UUID> groups) {
        Iterator<Debt> it = outstanding.iterator();
        while (money > 0 && it.hasNext()) {
            Debt debt = it.next();
            if (!groupUuid.equals(debt.groupUuid)) continue;
            long applied = Math.min(money, debt.remaining);
            debt.remaining -= applied;
            money -= applied;
            if (debt.remaining == 0) it.remove();
            if (inWindow) addCollected(out, groups, debt.groupUuid, applied);
        }
        return money;
    }

    private static long refundGroupDebt(LinkedList<Debt> outstanding, UUID groupUuid, long money) {
        Iterator<Debt> it = outstanding.iterator();
        while (money > 0 && it.hasNext()) {
            Debt debt = it.next();
            if (!groupUuid.equals(debt.groupUuid)) continue;
            long cancelled = Math.min(money, debt.remaining);
            debt.remaining -= cancelled;
            money -= cancelled;
            if (debt.remaining == 0) it.remove();
        }
        return money;
    }

    private static void writeOffDebt(LinkedList<Debt> outstanding, long money) {
        Iterator<Debt> it = outstanding.iterator();
        while (money > 0 && it.hasNext()) {
            Debt debt = it.next();
            long cancelled = Math.min(money, debt.remaining);
            debt.remaining -= cancelled;
            money -= cancelled;
            if (debt.remaining == 0) it.remove();
        }
    }

    private static void addBilled(GroupRevenue out, Set<UUID> groups, UUID groupUuid, long amount, UUID studentUuid) {
        if (groupUuid == null || !groups.contains(groupUuid)) return;
        out.billed().merge(groupUuid, amount, Long::sum);
        if (amount > 0) out.students().computeIfAbsent(groupUuid, k -> new HashSet<>()).add(studentUuid);
    }

    private static void reduceBilled(GroupRevenue out, Set<UUID> groups, UUID groupUuid, long amount) {
        if (groupUuid == null || !groups.contains(groupUuid)) return;
        out.billed().put(groupUuid, Math.max(0L, out.billed().getOrDefault(groupUuid, 0L) - amount));
    }

    private static void addCollected(GroupRevenue out, Set<UUID> groups, UUID groupUuid, long amount) {
        if (groupUuid == null || !groups.contains(groupUuid)) return;
        out.collected().merge(groupUuid, amount, Long::sum);
    }

    private static String teacherName(Teacher teacher) {
        return teacher.getUser() != null
                ? (teacher.getUser().getFirstName() + " " + teacher.getUser().getLastName()) : null;
    }

    private static Map<UUID, Long> toMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }
}
