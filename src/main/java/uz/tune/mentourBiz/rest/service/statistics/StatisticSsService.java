package uz.tune.mentourBiz.rest.service.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.ResTeacherRank;
import uz.tune.mentourBiz.rest.payload.res.ResDirectorOverallStats;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.stats.ResMonthlyStats;
import uz.tune.mentourBiz.rest.repository.FinanceTransactionRepo;
import uz.tune.mentourBiz.rest.repository.OrganizationRepository;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.shop.ItemRedemptionRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.FinanceService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.stats.ResGrowthMetrics;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticSsService {

    private final StudentRepo studentRepo;
    private final UserScopeService userScopeService;
    private final UserService userService;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final SchoolRepo schoolRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final FinanceService financeService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ItemRedemptionRepository itemRedemptionRepository;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final OrganizationRepository organizationRepository;
    private final FinanceTransactionRepo transactionRepo; // Added missing field


    private Instant getStartOfCurrentMonth() {
        return LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.of("Asia/Tashkent"))
                .toInstant();
    }


    @Transactional(readOnly = true)
    public ResGrowthMetrics getGrowthMetrics(UUID schoolUuid) {
        User user = userService.getCurrentUser();
        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolUuid);

        Collection<UUID> schoolUuids;
        if (resolvedId != null) {
            schoolUuids = List.of(resolvedId);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        // --- STUDENTS ---
        long totalStudentsNow = studentRepo.countTotalActiveMulti(schoolUuids, now);
        long totalStudentsThen = studentRepo.countTotalActiveMulti(schoolUuids, thirtyDaysAgo);

        // --- TEACHERS ---
        long totalTeachersNow = teacherRepository.countTotalActiveMulti(schoolUuids, now);
        long totalTeachersThen = teacherRepository.countTotalActiveMulti(schoolUuids, thirtyDaysAgo);

        // --- CLASSES ---
        long totalClassesNow = groupRepository.countTotalActiveMulti(schoolUuids, now);
        long totalClassesThen = groupRepository.countTotalActiveMulti(schoolUuids, thirtyDaysAgo);

        return ResGrowthMetrics.builder()
                .students(new ResGrowthMetrics.GrowthDetail(
                        totalStudentsNow,
                        totalStudentsThen,
                        calculatePercentage(totalStudentsNow, totalStudentsThen)))
                .teachers(new ResGrowthMetrics.GrowthDetail(
                        totalTeachersNow,
                        totalTeachersThen,
                        calculatePercentage(totalTeachersNow, totalTeachersThen)))
                .classes(new ResGrowthMetrics.GrowthDetail(
                        totalClassesNow,
                        totalClassesThen,
                        calculatePercentage(totalClassesNow, totalClassesThen)))
                .build();
    }

    // Update the calculatePercentage to reflect population growth
    private double calculatePercentage(long currentTotal, long previousTotal) {
        if (previousTotal == 0) {
            return currentTotal > 0 ? 100.0 : 0.0;
        }
        // Formula: ((New - Old) / Old) * 100
        double change = ((double) (currentTotal - previousTotal) / previousTotal) * 100;
        return Math.round(change * 10.0) / 10.0;
    }

    @Transactional(readOnly = true)
    public ResMonthlyStats getStudentInflowStats(int year, UUID schoolUuid) {
        UUID targetSchoolUuid = userScopeService.resolveSchoolUuid(schoolUuid);

        if (targetSchoolUuid == null) {
            throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }

        List<Object[]> queryResults = studentRepo.countNewStudentsByMonth(targetSchoolUuid, year);
        Integer[] monthlyCounts = new Integer[12];
        Arrays.fill(monthlyCounts, 0);
        for (Object[] row : queryResults) {
            int month = ((Number) row[0]).intValue();
            int count = ((Number) row[1]).intValue();
            monthlyCounts[month - 1] = count;
        }

        List<Double> growthRates = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            if (i == 0) { growthRates.add(0.0); continue; }
            double current = monthlyCounts[i];
            double previous = monthlyCounts[i - 1];
            if (previous == 0) {
                growthRates.add(current > 0 ? 100.0 : 0.0);
            } else {
                double percentChange = ((current - previous) / previous) * 100;
                growthRates.add(Math.round(percentChange * 10.0) / 10.0);
            }
        }
        return ResMonthlyStats.builder().counts(Arrays.asList(monthlyCounts)).growthRates(growthRates).build();
    }

    @Transactional(readOnly = true)
    public ResGrowthMetrics getOrganizationGrowthMetrics(UUID orgUuid) {
        User user = userService.getCurrentUser();
        UUID targetOrg = user.getRole().equals(UserRole.SYS_ADMIN) ? orgUuid :
                schoolDirectorRepo.findByUser(user).get().getOrganization().getUuid();

        ZoneId zone = ZoneId.of("Asia/Tashkent");
        ZonedDateTime now = ZonedDateTime.now(zone);
        Instant startCurr = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone).toInstant();
        Instant endCurr = now.toInstant();
        ZonedDateTime firstOfLastMonth = now.minusMonths(1).withDayOfMonth(1);
        Instant startPrev = firstOfLastMonth.toLocalDate().atStartOfDay(zone).toInstant();
        Instant endPrev = firstOfLastMonth.withDayOfMonth(firstOfLastMonth.toLocalDate().lengthOfMonth())
                .toLocalDate().atTime(LocalTime.MAX).atZone(zone).toInstant();

        long currStudents = studentRepo.countNewActiveStudentsByOrg(targetOrg, startCurr, endCurr);
        long prevStudents = studentRepo.countNewActiveStudentsByOrg(targetOrg, startPrev, endPrev);
        long currTeachers = teacherRepository.countNewActiveTeachersByOrg(targetOrg, startCurr, endCurr);
        long prevTeachers = teacherRepository.countNewActiveTeachersByOrg(targetOrg, startPrev, endPrev);
        long currGroups = groupRepository.countNewActiveGroupsByOrg(targetOrg, startCurr, endCurr);
        long prevGroups = groupRepository.countNewActiveGroupsByOrg(targetOrg, startPrev, endPrev);

        return ResGrowthMetrics.builder()
                .students(new ResGrowthMetrics.GrowthDetail(currStudents, prevStudents, calculatePercentage(currStudents, prevStudents)))
                .teachers(new ResGrowthMetrics.GrowthDetail(currTeachers, prevTeachers, calculatePercentage(currTeachers, prevTeachers)))
                .classes(new ResGrowthMetrics.GrowthDetail(currGroups, prevGroups, calculatePercentage(currGroups, prevGroups)))
                .build();
    }

    public ResDirectorOverallStats getDirectorDetailedStats(UUID organizationUuid, boolean activeOnly) {
        User user = userService.getCurrentUser();
        List<UUID> schoolUuids = userScopeService.getAuthorizedSchoolUuids();

        if (user.getRole().equals(UserRole.SYS_ADMIN) && organizationUuid != null) {
            Organization organization = organizationRepository.findByUuid(organizationUuid).orElseThrow();
            schoolUuids = organization.getSchools().stream().map(School::getUuid).toList();
        }

        if (schoolUuids.isEmpty()) return ResDirectorOverallStats.builder().build();

        long totalStudents = studentRepo.countActiveStudentsIn(schoolUuids);
        long totalGroups = groupRepository.countActiveGroupsIn(schoolUuids);
        long totalCollected = 0;
        for(UUID sid : schoolUuids) {
            try {
                totalCollected += financeService.getFinanceSummary(sid,null, null, null,null,null, activeOnly).getCollectedRevenue();
            } catch (Exception ignored) {}
        }

        Long totalDebt = studentRepo.sumTotalOutstandingBalanceIn(schoolUuids);
        Double avgAttendance = attendanceRecordRepository.getAvgAttendanceIn(schoolUuids);
        long pendingOrders = itemRedemptionRepository.countPendingOrdersIn(schoolUuids);
        Instant threshold = Instant.now().plus(Duration.ofDays(7));

        final List<UUID> finalSchoolUuids = schoolUuids;
        long expiringSchools = schoolSubscriptionRepo.findAllByExpiresAtBeforeAndExpiresAtAfter(threshold, Instant.now(), Pageable.unpaged())
                .stream().filter(s -> finalSchoolUuids.contains(s.getSchool().getUuid())).count();

        long totalTeachers = teacherRepository.countActiveTeachersMulti(schoolUuids);

        return ResDirectorOverallStats.builder()
                .totalSchools(schoolUuids.size())
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalActiveGroups(totalGroups)
                .totalCollectedRevenueMonth(totalCollected)
                .totalOutstandingDebt(totalDebt != null ? Math.abs(totalDebt) : 0)
                .avgOrganizationAttendance(avgAttendance != null ? Math.round(avgAttendance * 10.0) / 10.0 : 0.0)
                .pendingShopOrders(pendingOrders)
                .expiringSchoolsCount(expiringSchools)
                .growthMetrics(getOrganizationGrowthMetrics(organizationUuid))
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrganizationAggregateDashboard(UUID orgUuid) {
        User user = userService.getCurrentUser();
        Organization org = (user.getRole().equals(UserRole.SYS_ADMIN)) ?
                organizationRepository.findByUuid(orgUuid).orElseThrow() :
                schoolDirectorRepo.findByUser(user).get().getOrganization();

        List<UUID> schoolUuids = org.getSchools().stream().map(School::getUuid).toList();

        long totalStudents = studentRepo.countActiveStudentsMulti(schoolUuids);
        long totalTeachers = teacherRepository.countActiveTeachersMulti(schoolUuids);
        long totalGroups = groupRepository.countActiveGroupsMulti(schoolUuids);

        Map<String, Long> revenueByCurrency = new HashMap<>();
        for (School s : org.getSchools()) {
            try {
                long collected = transactionRepo.sumCollectedRevenueFiltered(s.getUuid(), getStartOfCurrentMonth(), true);
                String currency = (s.getRegion() != null && s.getRegion().getCurrency() != null) ? s.getRegion().getCurrency() : "UZS";
                revenueByCurrency.merge(currency, collected, Long::sum);
            } catch (Exception ignored) {}
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSchools", org.getSchools().size());
        stats.put("totalStudents", totalStudents);
        stats.put("totalTeachers", totalTeachers);
        stats.put("totalGroups", totalGroups);
        stats.put("revenue", revenueByCurrency);
        stats.put("status", org.getStatus());
        stats.put("expiresAt", org.getExpiresAt());

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ResTeacherRank> getTopTeachers(UUID schoolUuid, Pageable pageable) {
        User user = userService.getCurrentUser();

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolUuid);

        Collection<UUID> schoolUuids;
        if (resolvedId != null) {
            schoolUuids = List.of(resolvedId);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        List<ResTeacherRank> allTopTeachers = new ArrayList<>();

        if (schoolUuids == null) {
            return Collections.emptyList();
        }

        for (UUID uuid : schoolUuids) {
            List<Object[]> results = teacherRepository.findTopTeachersBySchool(uuid, pageable);
            for (Object[] row : results) {
                Teacher t = (Teacher) row[0];
                Long studentCount = (Long) row[1];
                Double avgProgress = (Double) row[2];

                allTopTeachers.add(new ResTeacherRank(
                        t.getUser().getUuid(),
                        t.getUser().getFirstName() + " " + t.getUser().getLastName(),
                        studentCount.intValue(),
                        avgProgress != null ? Math.round(avgProgress * 10.0) / 10.0 : 0.0,
                        t.getUser().getAttachment() != null ? new ResAttachment(t.getUser().getAttachment()) : null
                ));
            }
        }

        return allTopTeachers.stream()
                .sorted(Comparator.comparing(ResTeacherRank::getAvgPerformance).reversed())
                .limit(pageable.getPageSize())
                .toList();
    }
}