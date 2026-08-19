package uz.tune.mentourBiz.rest.service.statistics;

import uz.tune.mentourBiz.rest.payload.res.stats.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SchoolAdminStatisticService {
    ResSchoolInfo getSchoolInfo();
    ResTodayAttendance getTodayAttendance(UUID schoolUuid);

    ResSchoolOverview getOverview();

    ResAttendanceSummary getAttendanceSummary(Instant from, Instant to);
    List<ResAttendanceByCourse> getAttendanceByCourse(Instant from, Instant to);
    List<ResAttendanceByStudent> getAttendanceByStudent(Instant from, Instant to);
    List<ResAttendanceTrend> getAttendanceTrend(int months);
    List<ResLowAttendanceLesson> getLowAttendanceLessons(int threshold, Instant from, Instant to);
    List<CourseStudentAttendanceDto> getCourseStudentAttendance(Long courseId, Instant from, Instant to);
    List<GroupAttendanceDto> getAttendanceByGroup(Instant from, Instant to);
    List<GroupStudentAttendanceDto> getGroupStudentAttendance(Long groupId, Instant from, Instant to);
    TeacherRetentionSummaryDto getTeacherRetentionSummary(UUID teacherUuid);
    List<ResAtRiskStudent> getAtRiskStudents(int threshold, Instant from, Instant to);

    List<ResTeacherRanking> getTeacherRanking();
    ResTeacherSummary getTeacherSummary();
    List<ResTeacherActivity> getTeacherActivity();
    List<ResTeacherCourse> getTeacherCourses(UUID teacherUuid);
    List<ResTeacherGroup> getTeacherGroups(UUID teacherUuid);
    List<ResTeacherLesson> getTeacherLessons(UUID teacherUuid);
    List<ResTeacherAttendanceTrend> getTeacherAttendanceTrend(UUID teacherUuid, int months);

    ResEnrollmentSummary getEnrollmentSummary();
    List<ResEnrollmentTrend> getEnrollmentTrend(int months);

    ResMauSummary getMauSummary();
    List<ResMauTrend> getMauTrend(int months);

    ResPaymentSummary getPaymentSummary();
    List<ResPaymentTrend> getPaymentTrend(int months);
    List<ResPaymentByStudent> getPaymentsByStudent();

    ResSchoolSubscription getSubscription();
    List<ResSchoolLesson> getLessons();

    ResLearningSummary getLearningSummary();
    List<ResGroupAnalytics> getGroupAnalytics();
    List<ResGroupStudentSummary> getGroupStudentSummary(Long groupId);
}
