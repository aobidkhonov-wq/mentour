package uz.tune.mentourBiz.rest.endpoint.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.rest.payload.res.stats.*;
import uz.tune.mentourBiz.rest.service.statistics.SchoolAdminStatisticService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/statistics/school-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'SCHOOL_ADMIN', 'SCHOOL_DIRECTOR')")
public class SchoolAdminStatisticEndpoint {

    private final SchoolAdminStatisticService statisticService;

    @GetMapping("/me")
    public ResponseEntity<ResSchoolInfo> getSchoolInfo() {
        return ResponseEntity.ok(statisticService.getSchoolInfo());
    }

    @GetMapping("/attendance/today")
    public ResponseEntity<ResTodayAttendance> getTodayAttendance(
            @RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(statisticService.getTodayAttendance(schoolUuid));
    }

    @GetMapping("/overview")
    public ResponseEntity<ResSchoolOverview> getOverview() {
        return ResponseEntity.ok(statisticService.getOverview());
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    @GetMapping("/attendance/summary")
    public ResponseEntity<ResAttendanceSummary> getAttendanceSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceSummary(parseDate(from), parseDate(to)));
    }

    @GetMapping("/attendance/by-course")
    public ResponseEntity<List<ResAttendanceByCourse>> getAttendanceByCourse(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceByCourse(parseDate(from), parseDate(to)));
    }

    @GetMapping("/attendance/by-student")
    public ResponseEntity<List<ResAttendanceByStudent>> getAttendanceByStudent(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceByStudent(parseDate(from), parseDate(to)));
    }

    @GetMapping("/attendance/trend")
    public ResponseEntity<List<ResAttendanceTrend>> getAttendanceTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getAttendanceTrend(months));
    }

    @GetMapping("/attendance/low-attendance-lessons")
    public ResponseEntity<List<ResLowAttendanceLesson>> getLowAttendanceLessons(
            @RequestParam(defaultValue = "60") int threshold,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getLowAttendanceLessons(threshold, parseDate(from), parseDate(to)));
    }

    @GetMapping("/attendance/courses/{courseId}/students")
    public ResponseEntity<List<CourseStudentAttendanceDto>> getCourseStudentAttendance(
            @PathVariable Long courseId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getCourseStudentAttendance(courseId, parseDate(from), parseDate(to)));
    }

    @GetMapping("/attendance/by-group")
    public ResponseEntity<List<GroupAttendanceDto>> getAttendanceByGroup(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAttendanceByGroup(parseDate(from), parseDate(to)));
    }

    @GetMapping("/attendance/groups/{groupId}/students")
    public ResponseEntity<List<GroupStudentAttendanceDto>> getGroupStudentAttendance(
            @PathVariable Long groupId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getGroupStudentAttendance(groupId, parseDate(from), parseDate(to)));
    }

    @GetMapping("/attendance/at-risk-students")
    public ResponseEntity<List<ResAtRiskStudent>> getAtRiskStudents(
            @RequestParam(defaultValue = "60") int threshold,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(statisticService.getAtRiskStudents(threshold, parseDate(from), parseDate(to)));
    }

    // ── Teachers ──────────────────────────────────────────────────────────────

    @GetMapping("/teachers/ranking")
    public ResponseEntity<List<ResTeacherRanking>> getTeacherRanking() {
        return ResponseEntity.ok(statisticService.getTeacherRanking());
    }

    @GetMapping("/teachers/summary")
    public ResponseEntity<ResTeacherSummary> getTeacherSummary() {
        return ResponseEntity.ok(statisticService.getTeacherSummary());
    }

    @GetMapping("/teachers")
    public ResponseEntity<List<ResTeacherActivity>> getTeacherActivity() {
        return ResponseEntity.ok(statisticService.getTeacherActivity());
    }

    @GetMapping("/teachers/{teacherUuid}/courses")
    public ResponseEntity<List<ResTeacherCourse>> getTeacherCourses(@PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherCourses(teacherUuid));
    }

    @GetMapping("/teachers/{teacherUuid}/groups")
    public ResponseEntity<List<ResTeacherGroup>> getTeacherGroups(@PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherGroups(teacherUuid));
    }

    @GetMapping("/teachers/{teacherUuid}/lessons")
    public ResponseEntity<List<ResTeacherLesson>> getTeacherLessons(@PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherLessons(teacherUuid));
    }

    @GetMapping("/teachers/{teacherUuid}/trend")
    public ResponseEntity<List<ResTeacherAttendanceTrend>> getTeacherAttendanceTrend(
            @PathVariable UUID teacherUuid,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getTeacherAttendanceTrend(teacherUuid, months));
    }

    @GetMapping("/teachers/{teacherUuid}/retention-summary")
    public ResponseEntity<TeacherRetentionSummaryDto> getTeacherRetentionSummary(@PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(statisticService.getTeacherRetentionSummary(teacherUuid));
    }

    // ── Enrollment ────────────────────────────────────────────────────────────

    @GetMapping("/enrollment/summary")
    public ResponseEntity<ResEnrollmentSummary> getEnrollmentSummary() {
        return ResponseEntity.ok(statisticService.getEnrollmentSummary());
    }

    @GetMapping("/enrollment/trend")
    public ResponseEntity<List<ResEnrollmentTrend>> getEnrollmentTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getEnrollmentTrend(months));
    }

    // ── MAU ───────────────────────────────────────────────────────────────────

    @GetMapping("/mau/summary")
    public ResponseEntity<ResMauSummary> getMauSummary() {
        return ResponseEntity.ok(statisticService.getMauSummary());
    }

    @GetMapping("/mau/trend")
    public ResponseEntity<List<ResMauTrend>> getMauTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getMauTrend(months));
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @GetMapping("/payments/summary")
    public ResponseEntity<ResPaymentSummary> getPaymentSummary() {
        return ResponseEntity.ok(statisticService.getPaymentSummary());
    }

    @GetMapping("/payments/trend")
    public ResponseEntity<List<ResPaymentTrend>> getPaymentTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(statisticService.getPaymentTrend(months));
    }

    @GetMapping("/payments/by-student")
    public ResponseEntity<List<ResPaymentByStudent>> getPaymentsByStudent() {
        return ResponseEntity.ok(statisticService.getPaymentsByStudent());
    }

    // ── Subscription & Lessons ────────────────────────────────────────────────

    @GetMapping("/subscription")
    public ResponseEntity<ResSchoolSubscription> getSubscription() {
        return ResponseEntity.ok(statisticService.getSubscription());
    }

    @GetMapping("/lessons")
    public ResponseEntity<List<ResSchoolLesson>> getLessons() {
        return ResponseEntity.ok(statisticService.getLessons());
    }

    // ── Learning & Groups ─────────────────────────────────────────────────────

    @GetMapping("/learning/summary")
    public ResponseEntity<ResLearningSummary> getLearningSummary() {
        return ResponseEntity.ok(statisticService.getLearningSummary());
    }

    @GetMapping("/groups/analytics")
    public ResponseEntity<List<ResGroupAnalytics>> getGroupAnalytics() {
        return ResponseEntity.ok(statisticService.getGroupAnalytics());
    }

    @GetMapping("/groups/{groupId}/students")
    public ResponseEntity<List<ResGroupStudentSummary>> getGroupStudentSummary(@PathVariable Long groupId) {
        return ResponseEntity.ok(statisticService.getGroupStudentSummary(groupId));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
