package uz.tune.mentourBiz.rest.service.statistics.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.payload.res.stats.ResCourseInfo;
import uz.tune.mentourBiz.rest.payload.res.stats.ResEnrollmentSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolOverview;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolInfo;
import uz.tune.mentourBiz.rest.payload.res.stats.ResEnrollmentTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResMauBySchool;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolGrowthByMonth;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolGrowthSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolGrowthTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolSubscription;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSubscriptionOverview;
import uz.tune.mentourBiz.rest.payload.res.stats.ResMauSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResMauTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentByStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentMethodStat;
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.CourseStudentAttendanceDto;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupStudentAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.TeacherGroupRetentionDto;
import uz.tune.mentourBiz.rest.payload.res.stats.TeacherRetentionSummaryDto;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAtRiskStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResLowAttendanceLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherRanking;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherActivity;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherAttendanceTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherGroup;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResCourseItem;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolCourses;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResUserLastLogin;
import uz.tune.mentourBiz.rest.payload.res.stats.ResLearningSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResGroupAnalytics;
import uz.tune.mentourBiz.rest.payload.res.stats.ResGroupStudentSummary;
import uz.tune.mentourBiz.rest.repository.statistic.StatisticRepo;
import uz.tune.mentourBiz.rest.service.statistics.SysAdminStatisticService;

@Service
@RequiredArgsConstructor
public class SysAdminStatisticServiceImpl implements SysAdminStatisticService {

    private final StatisticRepo statisticRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ResSchoolLastLesson> getSchoolLastLessons(String sort) {
        return statisticRepo.findAllSchoolLastLessons(sort);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResSchoolLastCourse> getSchoolLastCourse(String sort) {
        return statisticRepo.findAllSchoolLastCourse(sort);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResSchoolLesson> getSchoolLessons(UUID schoolUuid) {
        return statisticRepo.findLessonsBySchoolUuid(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResSchoolCourses> getAllCoursesInfo() {
        List<ResCourseInfo> flat = statisticRepo.findAllCoursesInfo();
        Map<java.util.UUID, ResSchoolCourses> map = new LinkedHashMap<>();
        for (ResCourseInfo row : flat) {
            map.computeIfAbsent(row.getSchoolUuid(), uuid -> {
                ResSchoolCourses school = new ResSchoolCourses();
                school.setSchoolUuid(uuid);
                school.setSchoolName(row.getSchoolName());
                school.setCourses(new ArrayList<>());
                return school;
            }).getCourses().add(new ResCourseItem(
                    row.getCourseId(),
                    row.getCourseName(),
                    row.getCourseStatus(),
                    row.getCourseCreatedAt(),
                    row.getGroupName(),
                    row.getStartDate(),
                    row.getLastLessonCreatedAt()
            ));
        }
        return new ArrayList<>(map.values());
    }

    @Override
    @Transactional(readOnly = true)
    public ResAttendanceSummary getAttendanceSummary(UUID schoolUuid, Instant from, Instant to) {
        return statisticRepo.findAttendanceSummaryBySchool(schoolUuid, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResAttendanceByCourse> getAttendanceByCourse(UUID schoolUuid, Instant from, Instant to) {
        return statisticRepo.findAttendanceByCourse(schoolUuid, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResAttendanceByStudent> getAttendanceByStudent(UUID schoolUuid, Instant from, Instant to) {
        return statisticRepo.findAttendanceByStudent(schoolUuid, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResAttendanceTrend> getAttendanceTrend(UUID schoolUuid, int months) {
        return statisticRepo.findAttendanceTrend(schoolUuid, months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResLowAttendanceLesson> getLowAttendanceLessons(UUID schoolUuid, int threshold, Instant from, Instant to) {
        return statisticRepo.findLowAttendanceLessons(schoolUuid, threshold, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResAtRiskStudent> getAtRiskStudents(UUID schoolUuid, int threshold, Instant from, Instant to) {
        return statisticRepo.findAtRiskStudents(schoolUuid, threshold, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseStudentAttendanceDto> getCourseStudentAttendance(UUID schoolUuid, Long courseId, Instant from, Instant to) {
        return statisticRepo.findCourseStudentAttendance(schoolUuid, courseId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public ResTeacherSummary getTeacherSummary(UUID schoolUuid) {
        return statisticRepo.findTeacherSummaryBySchool(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResTeacherRanking> getTeacherRanking(UUID schoolUuid) {
        return statisticRepo.findTeacherRanking(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResTeacherActivity> getTeacherActivity(UUID schoolUuid) {
        return statisticRepo.findTeacherActivityBySchool(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResTeacherCourse> getTeacherCourses(UUID schoolUuid, UUID teacherUuid) {
        return statisticRepo.findCoursesByTeacher(schoolUuid, teacherUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResTeacherGroup> getTeacherGroups(UUID schoolUuid, UUID teacherUuid) {
        List<ResTeacherGroup> groups = statisticRepo.findGroupsByTeacher(schoolUuid, teacherUuid);
        List<ResTeacherCourse> courses = statisticRepo.findCoursesByTeacher(schoolUuid, teacherUuid);
        Map<Long, List<ResTeacherCourse>> byGroup = courses.stream()
                .collect(Collectors.groupingBy(ResTeacherCourse::getGroupId));
        groups.forEach(g -> g.setCourses(byGroup.getOrDefault(g.getGroupId(), List.of())));
        return groups;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResTeacherLesson> getTeacherLessons(UUID schoolUuid, UUID teacherUuid) {
        return statisticRepo.findLessonsByTeacher(schoolUuid, teacherUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResTeacherAttendanceTrend> getTeacherAttendanceTrend(UUID schoolUuid, UUID teacherUuid, int months) {
        return statisticRepo.findTeacherAttendanceTrend(schoolUuid, teacherUuid, months);
    }

    @Override
    @Transactional(readOnly = true)
    public ResPaymentSummary getPaymentSummary(UUID schoolUuid) {
        long[] scalars = statisticRepo.findPaymentScalarsBySchool(schoolUuid);
        List<ResPaymentMethodStat> byMethod = statisticRepo.findPaymentByMethodBySchool(schoolUuid);
        return new ResPaymentSummary(
                scalars[0],  // total_collected
                scalars[1],  // total_charged
                scalars[2],  // total_outstanding (sum of per-student debts, negatives excluded)
                scalars[3],  // payment_count
                scalars[4],  // students_paid
                byMethod
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResPaymentTrend> getPaymentTrend(UUID schoolUuid, int months) {
        return statisticRepo.findPaymentTrendBySchool(schoolUuid, months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResPaymentByStudent> getPaymentsByStudent(UUID schoolUuid) {
        return statisticRepo.findPaymentsByStudentBySchool(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public ResSchoolInfo getSchoolInfo(UUID schoolUuid) {
        return statisticRepo.findSchoolInfo(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public ResSchoolOverview getSchoolOverview(UUID schoolUuid) {
        return statisticRepo.findSchoolOverview(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResSchoolGrowthByMonth> getSchoolsByMonth(String month, String status) {
        return statisticRepo.findSchoolsByMonth(month, status);
    }

    @Override
    @Transactional(readOnly = true)
    public ResSchoolGrowthSummary getSchoolGrowthSummary() {
        return statisticRepo.findSchoolGrowthSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResSchoolGrowthTrend> getSchoolGrowthTrend(int months) {
        return statisticRepo.findSchoolGrowthTrend(months);
    }

    @Override
    @Transactional(readOnly = true)
    public ResEnrollmentSummary getEnrollmentSummary(UUID schoolUuid) {
        return statisticRepo.findEnrollmentSummaryBySchool(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResEnrollmentTrend> getEnrollmentTrend(UUID schoolUuid, int months) {
        return statisticRepo.findEnrollmentTrendBySchool(schoolUuid, months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResEnrollmentTrend> getPlatformEnrollmentTrend(int months) {
        return statisticRepo.findPlatformEnrollmentTrend(months);
    }

    @Override
    @Transactional(readOnly = true)
    public ResSubscriptionOverview getSubscriptionOverview() {
        return statisticRepo.findSubscriptionOverview();
    }

    @Override
    @Transactional(readOnly = true)
    public ResSchoolSubscription getSchoolSubscription(UUID schoolUuid) {
        return statisticRepo.findSchoolSubscription(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public ResMauSummary getPlatformMauSummary() {
        return statisticRepo.findPlatformMauSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResMauTrend> getPlatformMauTrend(int months) {
        return statisticRepo.findPlatformMauTrend(months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResMauBySchool> getMauBySchool() {
        return statisticRepo.findMauBySchool();
    }

    @Override
    @Transactional(readOnly = true)
    public ResMauSummary getSchoolMauSummary(UUID schoolUuid) {
        return statisticRepo.findSchoolMauSummary(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResMauTrend> getSchoolMauTrend(UUID schoolUuid, int months) {
        return statisticRepo.findSchoolMauTrend(schoolUuid, months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResUserLastLogin> getInactiveUsers(int days, String role) {
        return statisticRepo.findInactiveUsers(days, role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupAttendanceDto> getAttendanceByGroup(UUID schoolUuid, Instant from, Instant to) {
        return statisticRepo.findAttendanceByGroup(schoolUuid, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupStudentAttendanceDto> getGroupStudentAttendance(UUID schoolUuid, Long groupId, Instant from, Instant to) {
        return statisticRepo.findGroupStudentAttendance(schoolUuid, groupId, from, to);
    }

    @Override
    public boolean teacherBelongsToSchool(UUID schoolUuid, UUID teacherUuid) {
        return statisticRepo.teacherBelongsToSchool(schoolUuid, teacherUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherRetentionSummaryDto getTeacherRetentionSummary(UUID schoolUuid, UUID teacherUuid) {
        if (!statisticRepo.teacherBelongsToSchool(schoolUuid, teacherUuid)) {
            throw new EntityNotFoundException("Teacher not found in school");
        }
        List<TeacherGroupRetentionDto> byGroup = statisticRepo.findTeacherRetentionByGroup(schoolUuid, teacherUuid);
        long enrolled = byGroup.stream().mapToLong(TeacherGroupRetentionDto::getEnrolled).sum();
        long retained = byGroup.stream().mapToLong(TeacherGroupRetentionDto::getRetained).sum();
        long dropped = byGroup.stream().mapToLong(TeacherGroupRetentionDto::getDropped).sum();
        Double rate = enrolled > 0 ? Math.round(retained * 1000.0 / enrolled) / 10.0 : null;
        return new TeacherRetentionSummaryDto(enrolled, retained, dropped, rate, byGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public ResLearningSummary getLearningSummary(UUID schoolUuid) {
        return statisticRepo.findLearningSummaryBySchool(schoolUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResGroupAnalytics> getGroupAnalytics(UUID schoolUuid) {
        return statisticRepo.findGroupAnalyticsBySchool(schoolUuid);
    }

    @Override
    public boolean groupBelongsToSchool(UUID schoolUuid, Long groupId) {
        return statisticRepo.groupBelongsToSchool(schoolUuid, groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResGroupStudentSummary> getGroupStudentSummary(UUID schoolUuid, Long groupId) {
        return statisticRepo.findGroupStudentSummary(schoolUuid, groupId);
    }
}
