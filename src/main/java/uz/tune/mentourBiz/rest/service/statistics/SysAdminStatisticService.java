package uz.tune.mentourBiz.rest.service.statistics;

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
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.CourseStudentAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupStudentAttendanceDto;
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
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolCourses;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResUserLastLogin;
import uz.tune.mentourBiz.rest.payload.res.stats.ResLearningSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResGroupAnalytics;
import uz.tune.mentourBiz.rest.payload.res.stats.ResGroupStudentSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SysAdminStatisticService {
    List<ResSchoolLastLesson> getSchoolLastLessons(String sort);
    List<ResSchoolLastCourse> getSchoolLastCourse(String sort);
    List<ResSchoolCourses> getAllCoursesInfo();
    List<ResSchoolLesson> getSchoolLessons(UUID schoolUuid);

    ResAttendanceSummary getAttendanceSummary(UUID schoolUuid, Instant from, Instant to);
    List<ResAttendanceByCourse> getAttendanceByCourse(UUID schoolUuid, Instant from, Instant to);
    List<ResAttendanceByStudent> getAttendanceByStudent(UUID schoolUuid, Instant from, Instant to);
    List<ResAttendanceTrend> getAttendanceTrend(UUID schoolUuid, int months);
    List<ResLowAttendanceLesson> getLowAttendanceLessons(UUID schoolUuid, int threshold, Instant from, Instant to);
    List<ResAtRiskStudent> getAtRiskStudents(UUID schoolUuid, int threshold, Instant from, Instant to);
    List<CourseStudentAttendanceDto> getCourseStudentAttendance(UUID schoolUuid, Long courseId, Instant from, Instant to);
    List<GroupAttendanceDto> getAttendanceByGroup(UUID schoolUuid, Instant from, Instant to);
    List<GroupStudentAttendanceDto> getGroupStudentAttendance(UUID schoolUuid, Long groupId, Instant from, Instant to);
    TeacherRetentionSummaryDto getTeacherRetentionSummary(UUID schoolUuid, UUID teacherUuid);
    boolean teacherBelongsToSchool(UUID schoolUuid, UUID teacherUuid);

    ResTeacherSummary getTeacherSummary(UUID schoolUuid);
    List<ResTeacherRanking> getTeacherRanking(UUID schoolUuid);
    List<ResTeacherActivity> getTeacherActivity(UUID schoolUuid);
    List<ResTeacherCourse> getTeacherCourses(UUID schoolUuid, UUID teacherUuid);
    List<ResTeacherGroup> getTeacherGroups(UUID schoolUuid, UUID teacherUuid);
    List<ResTeacherLesson> getTeacherLessons(UUID schoolUuid, UUID teacherUuid);
    List<ResTeacherAttendanceTrend> getTeacherAttendanceTrend(UUID schoolUuid, UUID teacherUuid, int months);

    ResPaymentSummary getPaymentSummary(UUID schoolUuid);
    List<ResPaymentTrend> getPaymentTrend(UUID schoolUuid, int months);
    List<ResPaymentByStudent> getPaymentsByStudent(UUID schoolUuid);

    ResSchoolInfo getSchoolInfo(UUID schoolUuid);
    ResSchoolOverview getSchoolOverview(UUID schoolUuid);
    ResSchoolGrowthSummary getSchoolGrowthSummary();
    List<ResSchoolGrowthTrend> getSchoolGrowthTrend(int months);
    List<ResSchoolGrowthByMonth> getSchoolsByMonth(String month, String status);
    ResEnrollmentSummary getEnrollmentSummary(UUID schoolUuid);
    List<ResEnrollmentTrend> getEnrollmentTrend(UUID schoolUuid, int months);
    List<ResEnrollmentTrend> getPlatformEnrollmentTrend(int months);
    ResSubscriptionOverview getSubscriptionOverview();
    ResSchoolSubscription getSchoolSubscription(UUID schoolUuid);

    ResMauSummary getPlatformMauSummary();
    List<ResMauTrend> getPlatformMauTrend(int months);
    List<ResMauBySchool> getMauBySchool();
    ResMauSummary getSchoolMauSummary(UUID schoolUuid);
    List<ResMauTrend> getSchoolMauTrend(UUID schoolUuid, int months);

    List<ResUserLastLogin> getInactiveUsers(int days, String role);

    ResLearningSummary getLearningSummary(UUID schoolUuid);
    List<ResGroupAnalytics> getGroupAnalytics(UUID schoolUuid);
    boolean groupBelongsToSchool(UUID schoolUuid, Long groupId);
    List<ResGroupStudentSummary> getGroupStudentSummary(UUID schoolUuid, Long groupId);
}
