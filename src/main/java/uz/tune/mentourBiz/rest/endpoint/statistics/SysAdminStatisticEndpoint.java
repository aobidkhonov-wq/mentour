package uz.tune.mentourBiz.rest.endpoint.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import uz.tune.mentourBiz.rest.payload.res.stats.CourseStudentAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAtRiskStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResLowAttendanceLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResEnrollmentSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolOverview;
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
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherRanking;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherActivity;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherGroup;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherAttendanceTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolCourses;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupStudentAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.TeacherRetentionSummaryDto;
import uz.tune.mentourBiz.rest.payload.res.stats.ResUserLastLogin;
import uz.tune.mentourBiz.rest.service.statistics.SysAdminStatisticService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics/sys-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class SysAdminStatisticEndpoint {

    private final SysAdminStatisticService statisticService;

    @GetMapping("/schools/last-lesson")
    public ResponseEntity<List<ResSchoolLastLesson>> getSchoolLastLessons(
            @RequestParam(defaultValue = "desc") String sort) {
        return ResponseEntity.ok(statisticService.getSchoolLastLessons(sort));
    }

    @GetMapping("/schools/last-course")
    public ResponseEntity<List<ResSchoolLastCourse>> getSchoolLastCourse(
            @RequestParam(defaultValue = "desc") String sort) {
        return ResponseEntity.ok(statisticService.getSchoolLastCourse(sort));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<ResSchoolCourses>> getAllCoursesInfo() {
        return ResponseEntity.ok(statisticService.getAllCoursesInfo());
    }

    @GetMapping("/schools/{uuid}/lessons")
    public ResponseEntity<List<ResSchoolLesson>> getSchoolLessons(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getSchoolLessons(uuid));
    }

    @GetMapping("/schools/{uuid}/attendance/summary")
    public ResponseEntity<ResAttendanceSummary> getAttendanceSummary(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceSummary(uuid, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/attendance/by-course")
    public ResponseEntity<List<ResAttendanceByCourse>> getAttendanceByCourse(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceByCourse(uuid, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/attendance/by-student")
    public ResponseEntity<List<ResAttendanceByStudent>> getAttendanceByStudent(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceByStudent(uuid, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/attendance/trend")
    public ResponseEntity<List<ResAttendanceTrend>> getAttendanceTrend(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getAttendanceTrend(uuid, months));
    }

    @GetMapping("/schools/{uuid}/attendance/low-attendance-lessons")
    public ResponseEntity<List<ResLowAttendanceLesson>> getLowAttendanceLessons(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "60") int threshold,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getLowAttendanceLessons(uuid, threshold, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/attendance/courses/{courseId}/students")
    public ResponseEntity<List<CourseStudentAttendanceDto>> getCourseStudentAttendance(
            @PathVariable UUID uuid,
            @PathVariable Long courseId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getCourseStudentAttendance(uuid, courseId, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/attendance/by-group")
    public ResponseEntity<List<GroupAttendanceDto>> getAttendanceByGroup(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceByGroup(uuid, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/attendance/groups/{groupId}/students")
    public ResponseEntity<List<GroupStudentAttendanceDto>> getGroupStudentAttendance(
            @PathVariable UUID uuid,
            @PathVariable Long groupId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getGroupStudentAttendance(uuid, groupId, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/attendance/at-risk-students")
    public ResponseEntity<List<ResAtRiskStudent>> getAtRiskStudents(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "60") int threshold,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAtRiskStudents(uuid, threshold, parseDate(from), parseDate(to)));
    }

    @GetMapping("/schools/{uuid}/teachers/ranking")
    public ResponseEntity<List<ResTeacherRanking>> getTeacherRanking(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getTeacherRanking(uuid));
    }

    @GetMapping("/schools/{uuid}/teachers/summary")
    public ResponseEntity<ResTeacherSummary> getTeacherSummary(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getTeacherSummary(uuid));
    }

    @GetMapping("/schools/{uuid}/teachers")
    public ResponseEntity<List<ResTeacherActivity>> getTeacherActivity(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getTeacherActivity(uuid));
    }

    @GetMapping("/schools/{uuid}/teachers/{teacherUuid}/courses")
    public ResponseEntity<List<ResTeacherCourse>> getTeacherCourses(
            @PathVariable UUID uuid,
            @PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherCourses(uuid, teacherUuid));
    }

    @GetMapping("/schools/{uuid}/teachers/{teacherUuid}/groups")
    public ResponseEntity<List<ResTeacherGroup>> getTeacherGroups(
            @PathVariable UUID uuid,
            @PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherGroups(uuid, teacherUuid));
    }

    @GetMapping("/schools/{uuid}/teachers/{teacherUuid}/lessons")
    public ResponseEntity<List<ResTeacherLesson>> getTeacherLessons(
            @PathVariable UUID uuid,
            @PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherLessons(uuid, teacherUuid));
    }

    @GetMapping("/schools/{uuid}/teachers/{teacherUuid}/trend")
    public ResponseEntity<List<ResTeacherAttendanceTrend>> getTeacherAttendanceTrend(
            @PathVariable UUID uuid,
            @PathVariable UUID teacherUuid,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getTeacherAttendanceTrend(uuid, teacherUuid, months));
    }

    @GetMapping("/schools/{uuid}/overview")
    public ResponseEntity<ResSchoolOverview> getSchoolOverview(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getSchoolOverview(uuid));
    }

    @GetMapping("/schools/growth/by-month")
    public ResponseEntity<List<ResSchoolGrowthByMonth>> getSchoolsByMonth(
            @RequestParam String month,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(statisticService.getSchoolsByMonth(month, status));
    }

    @GetMapping("/schools/growth/summary")
    public ResponseEntity<ResSchoolGrowthSummary> getSchoolGrowthSummary() {
        return ResponseEntity.ok(statisticService.getSchoolGrowthSummary());
    }

    @GetMapping("/schools/growth/trend")
    public ResponseEntity<List<ResSchoolGrowthTrend>> getSchoolGrowthTrend(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(statisticService.getSchoolGrowthTrend(months));
    }

    @GetMapping("/enrollment/trend")
    public ResponseEntity<List<ResEnrollmentTrend>> getPlatformEnrollmentTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getPlatformEnrollmentTrend(months));
    }

    @GetMapping("/schools/{uuid}/enrollment/summary")
    public ResponseEntity<ResEnrollmentSummary> getEnrollmentSummary(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getEnrollmentSummary(uuid));
    }

    @GetMapping("/schools/{uuid}/enrollment/trend")
    public ResponseEntity<List<ResEnrollmentTrend>> getEnrollmentTrend(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getEnrollmentTrend(uuid, months));
    }

    @GetMapping("/subscriptions/overview")
    public ResponseEntity<ResSubscriptionOverview> getSubscriptionOverview() {
        return ResponseEntity.ok(statisticService.getSubscriptionOverview());
    }

    @GetMapping("/schools/{uuid}/subscription")
    public ResponseEntity<ResSchoolSubscription> getSchoolSubscription(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getSchoolSubscription(uuid));
    }

    @GetMapping("/mau/summary")
    public ResponseEntity<ResMauSummary> getPlatformMauSummary() {
        return ResponseEntity.ok(statisticService.getPlatformMauSummary());
    }

    @GetMapping("/mau/trend")
    public ResponseEntity<List<ResMauTrend>> getPlatformMauTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getPlatformMauTrend(months));
    }

    @GetMapping("/mau/by-school")
    public ResponseEntity<List<ResMauBySchool>> getMauBySchool() {
        return ResponseEntity.ok(statisticService.getMauBySchool());
    }

    @GetMapping("/schools/{uuid}/mau/summary")
    public ResponseEntity<ResMauSummary> getSchoolMauSummary(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getSchoolMauSummary(uuid));
    }

    @GetMapping("/schools/{uuid}/mau/trend")
    public ResponseEntity<List<ResMauTrend>> getSchoolMauTrend(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getSchoolMauTrend(uuid, months));
    }

    @GetMapping("/schools/{uuid}/payments/summary")
    public ResponseEntity<ResPaymentSummary> getPaymentSummary(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getPaymentSummary(uuid));
    }

    @GetMapping("/schools/{uuid}/payments/trend")
    public ResponseEntity<List<ResPaymentTrend>> getPaymentTrend(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getPaymentTrend(uuid, months));
    }

    @GetMapping("/schools/{uuid}/payments/by-student")
    public ResponseEntity<List<ResPaymentByStudent>> getPaymentsByStudent(@PathVariable UUID uuid) {
        return ResponseEntity.ok(statisticService.getPaymentsByStudent(uuid));
    }

    @GetMapping("/schools/{uuid}/teachers/{teacherUuid}/retention-summary")
    public ResponseEntity<TeacherRetentionSummaryDto> getTeacherRetentionSummary(
            @PathVariable UUID uuid,
            @PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherRetentionSummary(uuid, teacherUuid));
    }

    @GetMapping("/users/inactive")
    public ResponseEntity<List<ResUserLastLogin>> getInactiveUsers(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(statisticService.getInactiveUsers(days, role));
    }

    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
