package uz.tune.mentourBiz.rest.service.statistics.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.payload.res.stats.TeacherGroupRetentionDto;
import uz.tune.mentourBiz.rest.payload.res.stats.TeacherRetentionSummaryDto;
import uz.tune.mentourBiz.rest.payload.res.stats.ResGroupStudentSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.*;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.service.statistics.SchoolAdminStatisticService;
import uz.tune.mentourBiz.rest.service.statistics.SysAdminStatisticService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolAdminStatisticServiceImpl implements SchoolAdminStatisticService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserScopeService userScopeService;
    private final SysAdminStatisticService sysAdminStatisticService;

    private UUID resolvedSchoolUuid() {
        UUID uuid = userScopeService.resolveSchoolUuid(null);
        if (uuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        return uuid;
    }

    @Override @Transactional(readOnly = true)
    public ResSchoolInfo getSchoolInfo() {
        UUID uuid = userScopeService.resolveSchoolUuid(null);
        if (uuid == null) return null;
        return sysAdminStatisticService.getSchoolInfo(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public ResTodayAttendance getTodayAttendance(UUID schoolUuid) {
        UUID targetSchoolUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        if (targetSchoolUuid == null) {
            throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }
        LocalDate today = LocalDate.now(UZ_ZONE);
        Instant startOfDay = today.atStartOfDay(UZ_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(UZ_ZONE).toInstant();
        Double pct = attendanceRecordRepository.getTodayAttendancePercentage(targetSchoolUuid, startOfDay, endOfDay);
        double rounded = pct != null ? Math.round(pct * 10.0) / 10.0 : 0.0;
        return new ResTodayAttendance(today, rounded);
    }

    @Override @Transactional(readOnly = true)
    public ResSchoolOverview getOverview() {
        return sysAdminStatisticService.getSchoolOverview(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public ResAttendanceSummary getAttendanceSummary(Instant from, Instant to) {
        return sysAdminStatisticService.getAttendanceSummary(resolvedSchoolUuid(), from, to);
    }

    @Override @Transactional(readOnly = true)
    public List<ResAttendanceByCourse> getAttendanceByCourse(Instant from, Instant to) {
        return sysAdminStatisticService.getAttendanceByCourse(resolvedSchoolUuid(), from, to);
    }

    @Override @Transactional(readOnly = true)
    public List<ResAttendanceByStudent> getAttendanceByStudent(Instant from, Instant to) {
        return sysAdminStatisticService.getAttendanceByStudent(resolvedSchoolUuid(), from, to);
    }

    @Override @Transactional(readOnly = true)
    public List<ResAttendanceTrend> getAttendanceTrend(int months) {
        return sysAdminStatisticService.getAttendanceTrend(resolvedSchoolUuid(), months);
    }

    @Override @Transactional(readOnly = true)
    public List<ResLowAttendanceLesson> getLowAttendanceLessons(int threshold, Instant from, Instant to) {
        return sysAdminStatisticService.getLowAttendanceLessons(resolvedSchoolUuid(), threshold, from, to);
    }

    @Override @Transactional(readOnly = true)
    public List<CourseStudentAttendanceDto> getCourseStudentAttendance(Long courseId, Instant from, Instant to) {
        return sysAdminStatisticService.getCourseStudentAttendance(resolvedSchoolUuid(), courseId, from, to);
    }

    @Override @Transactional(readOnly = true)
    public List<ResAtRiskStudent> getAtRiskStudents(int threshold, Instant from, Instant to) {
        return sysAdminStatisticService.getAtRiskStudents(resolvedSchoolUuid(), threshold, from, to);
    }

    @Override @Transactional(readOnly = true)
    public List<ResTeacherRanking> getTeacherRanking() {
        return sysAdminStatisticService.getTeacherRanking(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public ResTeacherSummary getTeacherSummary() {
        return sysAdminStatisticService.getTeacherSummary(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResTeacherActivity> getTeacherActivity() {
        return sysAdminStatisticService.getTeacherActivity(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResTeacherCourse> getTeacherCourses(UUID teacherUuid) {
        return sysAdminStatisticService.getTeacherCourses(resolvedSchoolUuid(), teacherUuid);
    }

    @Override @Transactional(readOnly = true)
    public List<ResTeacherGroup> getTeacherGroups(UUID teacherUuid) {
        return sysAdminStatisticService.getTeacherGroups(resolvedSchoolUuid(), teacherUuid);
    }

    @Override @Transactional(readOnly = true)
    public List<ResTeacherLesson> getTeacherLessons(UUID teacherUuid) {
        return sysAdminStatisticService.getTeacherLessons(resolvedSchoolUuid(), teacherUuid);
    }

    @Override @Transactional(readOnly = true)
    public List<ResTeacherAttendanceTrend> getTeacherAttendanceTrend(UUID teacherUuid, int months) {
        return sysAdminStatisticService.getTeacherAttendanceTrend(resolvedSchoolUuid(), teacherUuid, months);
    }

    @Override @Transactional(readOnly = true)
    public ResEnrollmentSummary getEnrollmentSummary() {
        return sysAdminStatisticService.getEnrollmentSummary(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResEnrollmentTrend> getEnrollmentTrend(int months) {
        return sysAdminStatisticService.getEnrollmentTrend(resolvedSchoolUuid(), months);
    }

    @Override @Transactional(readOnly = true)
    public ResMauSummary getMauSummary() {
        return sysAdminStatisticService.getSchoolMauSummary(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResMauTrend> getMauTrend(int months) {
        return sysAdminStatisticService.getSchoolMauTrend(resolvedSchoolUuid(), months);
    }

    @Override @Transactional(readOnly = true)
    public ResPaymentSummary getPaymentSummary() {
        return sysAdminStatisticService.getPaymentSummary(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResPaymentTrend> getPaymentTrend(int months) {
        return sysAdminStatisticService.getPaymentTrend(resolvedSchoolUuid(), months);
    }

    @Override @Transactional(readOnly = true)
    public List<ResPaymentByStudent> getPaymentsByStudent() {
        return sysAdminStatisticService.getPaymentsByStudent(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public ResSchoolSubscription getSubscription() {
        return sysAdminStatisticService.getSchoolSubscription(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResSchoolLesson> getLessons() {
        return sysAdminStatisticService.getSchoolLessons(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public ResLearningSummary getLearningSummary() {
        return sysAdminStatisticService.getLearningSummary(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResGroupAnalytics> getGroupAnalytics() {
        return sysAdminStatisticService.getGroupAnalytics(resolvedSchoolUuid());
    }

    @Override @Transactional(readOnly = true)
    public List<ResGroupStudentSummary> getGroupStudentSummary(Long groupId) {
        UUID schoolUuid = resolvedSchoolUuid();
        if (!sysAdminStatisticService.groupBelongsToSchool(schoolUuid, groupId)) {
            throw new PermissionForbidden("Group does not belong to your school");
        }
        return sysAdminStatisticService.getGroupStudentSummary(schoolUuid, groupId);
    }

    @Override @Transactional(readOnly = true)
    public List<GroupAttendanceDto> getAttendanceByGroup(Instant from, Instant to) {
        return sysAdminStatisticService.getAttendanceByGroup(resolvedSchoolUuid(), from, to);
    }

    @Override @Transactional(readOnly = true)
    public List<GroupStudentAttendanceDto> getGroupStudentAttendance(Long groupId, Instant from, Instant to) {
        return sysAdminStatisticService.getGroupStudentAttendance(resolvedSchoolUuid(), groupId, from, to);
    }

    @Override @Transactional(readOnly = true)
    public TeacherRetentionSummaryDto getTeacherRetentionSummary(UUID teacherUuid) {
        UUID schoolUuid = resolvedSchoolUuid();
        if (!sysAdminStatisticService.teacherBelongsToSchool(schoolUuid, teacherUuid)) {
            throw new PermissionForbidden("Teacher does not belong to your school");
        }
        return sysAdminStatisticService.getTeacherRetentionSummary(schoolUuid, teacherUuid);
    }
}
