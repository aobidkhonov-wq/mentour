package uz.tune.mentourBiz.rest.repository.statistic;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.rest.payload.res.stats.ResCourseInfo;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceByStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAttendanceTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResLowAttendanceLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.CourseStudentAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.ResAtRiskStudent;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherRanking;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherActivity;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherAttendanceTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherGroup;
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
import uz.tune.mentourBiz.rest.payload.res.stats.ResPaymentTrend;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastCourse;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLastLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.ResSchoolLesson;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.GroupStudentAttendanceDto;
import uz.tune.mentourBiz.rest.payload.res.stats.ResUserLastLogin;
import uz.tune.mentourBiz.rest.payload.res.stats.TeacherGroupRetentionDto;
import uz.tune.mentourBiz.rest.payload.res.stats.ResLearningSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResGroupAnalytics;
import uz.tune.mentourBiz.rest.payload.res.stats.ResGroupStudentSummary;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class StatisticRepo {

    private final JdbcTemplate jdbcTemplate;

    public StatisticRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ResSchoolLastLesson> findAllSchoolLastLessons(String sort) {
        String order = "asc".equalsIgnoreCase(sort) ? "ASC" : "DESC";
        String sql = "SELECT school_uuid, school_name, last_scheduled_at, last_lesson_created_at FROM school_last_scheduled_lesson ORDER BY last_scheduled_at " + order;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ResSchoolLastLesson res = new ResSchoolLastLesson();
            res.setSchoolUuid(UUID.fromString(rs.getString("school_uuid")));
            res.setSchoolName(rs.getString("school_name"));
            res.setLastScheduledAt(rs.getTimestamp("last_scheduled_at").toInstant());
            Timestamp lastCreated = rs.getTimestamp("last_lesson_created_at");
            res.setLastLessonCreatedAt(lastCreated != null ? lastCreated.toInstant() : null);
            return res;
        });
    }

    public List<ResCourseInfo> findAllCoursesInfo() {
        String sql = """
                SELECT s.uuid AS school_uuid, s.name AS school_name,
                       c.id AS course_id, c.name AS course_name, c.status AS course_status,
                       c.created_at AS course_created_at,
                       g.name AS group_name,
                       MIN(cl.start_time) AS start_date,
                       MAX(cl.created_at) AS last_lesson_created_at
                FROM courses c
                JOIN schools s ON s.id = c.school_id
                LEFT JOIN groups g ON g.id = c.group_id
                LEFT JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                WHERE c.status IN ('ACTIVE', 'FINISHED')
                  AND s.status = 'ACTIVE'
                GROUP BY s.uuid, s.name, c.id, c.name, c.status, c.created_at, g.name
                ORDER BY s.name, c.created_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ResCourseInfo res = new ResCourseInfo();
            res.setSchoolUuid(UUID.fromString(rs.getString("school_uuid")));
            res.setSchoolName(rs.getString("school_name"));
            res.setCourseId(rs.getLong("course_id"));
            res.setCourseName(rs.getString("course_name"));
            res.setCourseStatus(rs.getString("course_status"));
            Timestamp createdAt = rs.getTimestamp("course_created_at");
            res.setCourseCreatedAt(createdAt != null ? createdAt.toInstant() : null);
            res.setGroupName(rs.getString("group_name"));
            Timestamp startDate = rs.getTimestamp("start_date");
            res.setStartDate(startDate != null ? startDate.toInstant() : null);
            Timestamp lastLessonCreatedAt = rs.getTimestamp("last_lesson_created_at");
            res.setLastLessonCreatedAt(lastLessonCreatedAt != null ? lastLessonCreatedAt.toInstant() : null);
            return res;
        });
    }

    public List<ResSchoolLastCourse> findAllSchoolLastCourse(String sort) {
        String order = "asc".equalsIgnoreCase(sort) ? "ASC" : "DESC";
        String sql = """
                SELECT s.uuid AS school_uuid, s.name AS school_name,
                       MAX(c.created_at) AS last_course_created_at,
                       COUNT(c.id) AS total_courses
                FROM schools s
                LEFT JOIN courses c ON c.school_id = s.id AND c.status != 'DELETED'
                WHERE s.status = 'ACTIVE'
                GROUP BY s.uuid, s.name
                ORDER BY last_course_created_at """ + " " + order + " NULLS LAST";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ResSchoolLastCourse res = new ResSchoolLastCourse();
            res.setSchoolUuid(UUID.fromString(rs.getString("school_uuid")));
            res.setSchoolName(rs.getString("school_name"));
            Timestamp ts = rs.getTimestamp("last_course_created_at");
            res.setLastCourseCreatedAt(ts != null ? ts.toInstant() : null);
            res.setTotalCourses(rs.getLong("total_courses"));
            return res;
        });
    }

    public List<ResSchoolLesson> findLessonsBySchoolUuid(UUID schoolUuid) {
        String sql = """
                SELECT cl.uuid AS lesson_uuid, cl.name AS lesson_name,
                       cl.start_time, cl.end_time, cl.status, cl.type_lesson,
                       c.id AS course_id, c.name AS course_name
                FROM course_lessons cl
                JOIN courses c ON c.id = cl.course_id
                JOIN schools s ON s.id = c.school_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND c.status != 'DELETED'
                  AND cl.status != 'DELETED'
                ORDER BY cl.start_time ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ResSchoolLesson res = new ResSchoolLesson();
            String uuid = rs.getString("lesson_uuid");
            res.setLessonUuid(uuid != null ? UUID.fromString(uuid) : null);
            res.setName(rs.getString("lesson_name"));
            Timestamp startTime = rs.getTimestamp("start_time");
            res.setStartTime(startTime != null ? startTime.toInstant() : null);
            Timestamp endTime = rs.getTimestamp("end_time");
            res.setEndTime(endTime != null ? endTime.toInstant() : null);
            res.setStatus(rs.getString("status"));
            res.setLessonType(rs.getString("type_lesson"));
            res.setCourseId(rs.getLong("course_id"));
            res.setCourseName(rs.getString("course_name"));
            return res;
        }, schoolUuid);
    }

    public ResAttendanceSummary findAttendanceSummaryBySchool(UUID schoolUuid, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        String base = """
                SELECT
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked,
                  COUNT(*) AS total,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM attendance_records ar
                JOIN course_lessons cl ON cl.id = ar.course_lesson_id
                JOIN courses c ON c.id = cl.course_id
                JOIN schools s ON s.id = c.school_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND c.status != 'DELETED'
                  AND cl.status != 'DELETED'
                  AND cl.start_time <= NOW()
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        return jdbcTemplate.queryForObject(sql.toString(), (rs, rowNum) -> new ResAttendanceSummary(
                rs.getLong("present"),
                rs.getLong("absent"),
                rs.getLong("late"),
                rs.getLong("not_marked"),
                rs.getLong("total"),
                rs.getDouble("attendance_rate")
        ), params.toArray());
    }

    public List<ResAttendanceByCourse> findAttendanceByCourse(UUID schoolUuid, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        String base = """
                SELECT
                  c.id AS course_id,
                  c.name AS course_name,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked,
                  COUNT(*) AS total,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM attendance_records ar
                JOIN course_lessons cl ON cl.id = ar.course_lesson_id
                JOIN courses c ON c.id = cl.course_id
                JOIN schools s ON s.id = c.school_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND c.status = 'ACTIVE'
                  AND cl.status != 'DELETED'
                  AND cl.start_time <= NOW()
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        sql.append(" GROUP BY c.id, c.name ORDER BY attendance_rate ASC NULLS LAST");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new ResAttendanceByCourse(
                rs.getLong("course_id"),
                rs.getString("course_name"),
                rs.getLong("present"),
                rs.getLong("absent"),
                rs.getLong("late"),
                rs.getLong("not_marked"),
                rs.getLong("total"),
                rs.getDouble("attendance_rate")
        ), params.toArray());
    }

    public List<ResAttendanceByStudent> findAttendanceByStudent(UUID schoolUuid, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        String base = """
                SELECT
                  st.uuid AS student_uuid,
                  u.first_name,
                  u.last_name,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked,
                  COUNT(*) AS total,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM attendance_records ar
                JOIN students st ON st.id = ar.student_id
                JOIN users u ON u.id = st.user_id
                JOIN course_lessons cl ON cl.id = ar.course_lesson_id
                JOIN courses c ON c.id = cl.course_id
                JOIN schools s ON s.id = c.school_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND c.status != 'DELETED'
                  AND cl.status != 'DELETED'
                  AND cl.start_time <= NOW()
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        sql.append(" GROUP BY st.uuid, u.first_name, u.last_name ORDER BY attendance_rate ASC NULLS LAST");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String uuid = rs.getString("student_uuid");
            return new ResAttendanceByStudent(
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getLong("present"),
                    rs.getLong("absent"),
                    rs.getLong("late"),
                    rs.getLong("not_marked"),
                    rs.getLong("total"),
                    rs.getDouble("attendance_rate")
            );
        }, params.toArray());
    }

    public List<ResAttendanceTrend> findAttendanceTrend(UUID schoolUuid, int months) {
        String sql = """
                SELECT
                  DATE_TRUNC('month', cl.start_time) AS month,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked,
                  COUNT(*) AS total,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM attendance_records ar
                JOIN course_lessons cl ON cl.id = ar.course_lesson_id
                JOIN courses c ON c.id = cl.course_id
                JOIN schools s ON s.id = c.school_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND c.status != 'DELETED'
                  AND cl.status != 'DELETED'
                  AND cl.start_time >= NOW() - (? * INTERVAL '1 month')
                  AND cl.start_time <= NOW()
                GROUP BY DATE_TRUNC('month', cl.start_time)
                ORDER BY month ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResAttendanceTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("present"),
                    rs.getLong("absent"),
                    rs.getLong("late"),
                    rs.getLong("not_marked"),
                    rs.getLong("total"),
                    rs.getDouble("attendance_rate")
            );
        }, schoolUuid, months);
    }

    public List<ResLowAttendanceLesson> findLowAttendanceLessons(UUID schoolUuid, int threshold, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        String base = """
                SELECT
                  cl.uuid AS lesson_uuid,
                  cl.name AS lesson_name,
                  cl.start_time,
                  c.id AS course_id,
                  c.name AS course_name,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked,
                  COUNT(*) AS total,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM attendance_records ar
                JOIN course_lessons cl ON cl.id = ar.course_lesson_id
                JOIN courses c ON c.id = cl.course_id
                JOIN schools s ON s.id = c.school_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND c.status != 'DELETED'
                  AND cl.status != 'DELETED'
                  AND cl.start_time <= NOW()
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        params.add(threshold);
        sql.append("""
                 GROUP BY cl.uuid, cl.name, cl.start_time, c.id, c.name
                HAVING ROUND(
                  (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                  / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                1) < ?
                ORDER BY attendance_rate ASC NULLS LAST, cl.start_time DESC
                """);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String uuid = rs.getString("lesson_uuid");
            Timestamp startTime = rs.getTimestamp("start_time");
            return new ResLowAttendanceLesson(
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("lesson_name"),
                    startTime != null ? startTime.toInstant() : null,
                    rs.getLong("course_id"),
                    rs.getString("course_name"),
                    rs.getLong("present"),
                    rs.getLong("absent"),
                    rs.getLong("late"),
                    rs.getLong("not_marked"),
                    rs.getLong("total"),
                    rs.getDouble("attendance_rate")
            );
        }, params.toArray());
    }

    public List<ResAtRiskStudent> findAtRiskStudents(UUID schoolUuid, int threshold, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        String base = """
                SELECT
                  st.uuid AS student_uuid,
                  u.first_name,
                  u.last_name,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) AS total,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS rate
                FROM attendance_records ar
                JOIN students st ON st.id = ar.student_id
                JOIN users u ON u.id = st.user_id
                JOIN course_lessons cl ON cl.id = ar.course_lesson_id
                JOIN courses c ON c.id = cl.course_id
                JOIN schools s ON s.id = c.school_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND c.status != 'DELETED'
                  AND cl.status != 'DELETED'
                  AND cl.start_time <= NOW()
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        params.add(threshold);
        sql.append("""
                 GROUP BY st.uuid, u.first_name, u.last_name
                HAVING ROUND(
                  (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                  / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                1) < ?
                ORDER BY rate ASC NULLS LAST
                """);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String uuid = rs.getString("student_uuid");
            return new ResAtRiskStudent(
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getLong("present"),
                    rs.getLong("absent"),
                    rs.getLong("late"),
                    rs.getLong("total"),
                    rs.getDouble("rate")
            );
        }, params.toArray());
    }

    public List<CourseStudentAttendanceDto> findCourseStudentAttendance(UUID schoolUuid, Long courseId, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        params.add(courseId);
        String base = """
                SELECT
                    u.first_name                                                           AS first_name,
                    u.last_name                                                            AS last_name,
                    st.uuid                                                                AS student_uuid,
                    COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT')              AS present,
                    COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')               AS absent,
                    COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')                 AS late,
                    COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED')           AS not_marked,
                    COUNT(*)                                                               AS total,
                    ROUND(
                        (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                        / NULLIF(
                            COUNT(*) FILTER (WHERE ar.attendance_status IN ('PRESENT','ABSENT','LATE')),
                            0
                        ),
                        1
                    )                                                                      AS rate
                FROM attendance_records ar
                JOIN students      st ON st.id  = ar.student_id
                JOIN users          u ON  u.id  = st.user_id
                JOIN course_lessons cl ON cl.id = ar.course_lesson_id
                JOIN courses        c  ON  c.id = cl.course_id
                JOIN schools        s  ON  s.id = c.school_id
                WHERE s.uuid   = ?
                  AND c.id     = ?
                  AND c.status  != 'DELETED'
                  AND cl.status != 'DELETED'
                  AND cl.start_time <= NOW()
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        sql.append("""
                 GROUP BY st.uuid, u.first_name, u.last_name
                ORDER BY rate ASC NULLS LAST
                """);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String uuid = rs.getString("student_uuid");
            return new CourseStudentAttendanceDto(
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getLong("present"),
                    rs.getLong("absent"),
                    rs.getLong("late"),
                    rs.getLong("not_marked"),
                    rs.getLong("total"),
                    rs.getDouble("rate")
            );
        }, params.toArray());
    }

    public List<ResTeacherRanking> findTeacherRanking(UUID schoolUuid) {
        String sql = """
                SELECT
                  u.uuid AS teacher_uuid,
                  u.first_name,
                  u.last_name,
                  COUNT(DISTINCT cl.id) AS total_lessons,
                  COUNT(DISTINCT st.id) FILTER (WHERE c.id IS NOT NULL) AS total_students,
                  ROUND(
                    (COUNT(ar.id) FILTER (WHERE ar.attendance_status = 'PRESENT')
                     + COUNT(ar.id) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS student_attendance_rate,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED') * 100.0
                    / NULLIF(COUNT(ar.id), 0),
                  1) AS attendance_marking_rate,
                  COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'ACTIVE') AS active_groups,
                  MAX(cl.start_time) AS last_lesson_at
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                LEFT JOIN groups g ON g.teacher_id = t.id AND g.group_status = 'ACTIVE'
                LEFT JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                LEFT JOIN course_lessons cl ON cl.course_id = c.id
                  AND cl.status != 'DELETED'
                  AND cl.start_time <= NOW()
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                LEFT JOIN enrollments e ON e.group_id = g.id AND e.status != 'DELETED'
                LEFT JOIN students st ON st.id = e.student_id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND u.status != 'BLOCKED'
                GROUP BY u.uuid, u.first_name, u.last_name
                ORDER BY student_attendance_rate ASC NULLS LAST
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String uuid = rs.getString("teacher_uuid");
            Timestamp lastLesson = rs.getTimestamp("last_lesson_at");
            return new ResTeacherRanking(
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getLong("total_lessons"),
                    rs.getLong("total_students"),
                    rs.getDouble("student_attendance_rate"),
                    rs.getDouble("attendance_marking_rate"),
                    rs.getLong("active_groups"),
                    lastLesson != null ? lastLesson.toInstant() : null
            );
        }, schoolUuid);
    }

    private void appendDateRange(StringBuilder sql, List<Object> params, Instant from, Instant to) {
        if (from != null) {
            sql.append(" AND cl.start_time >= ?");
            params.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND cl.start_time <= ?");
            params.add(Timestamp.from(to));
        }
    }

    public ResTeacherSummary findTeacherSummaryBySchool(UUID schoolUuid) {
        String sql = """
                SELECT
                  COUNT(DISTINCT t.id) FILTER (WHERE u.status = 'ACTIVE') AS total_teachers,
                  COUNT(DISTINCT t.id) FILTER (WHERE u.status = 'ACTIVE' AND c.status = 'ACTIVE') AS teachers_with_groups,
                  COUNT(DISTINCT t.id) FILTER (WHERE u.status = 'ACTIVE')
                    - COUNT(DISTINCT t.id) FILTER (WHERE u.status = 'ACTIVE' AND c.status = 'ACTIVE') AS teachers_without_groups,
                  COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'ACTIVE') AS active_courses,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE cl.start_time >= NOW() - INTERVAL '30 days'
                                           AND cl.start_time <= NOW()
                                           AND ar.attendance_status != 'NOT_MARKED') * 100.0
                    / NULLIF(COUNT(ar.id) FILTER (WHERE cl.start_time >= NOW() - INTERVAL '30 days'
                                                    AND cl.start_time <= NOW()), 0),
                  1) AS marking_rate_30d
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                LEFT JOIN groups g ON g.teacher_id = t.id AND g.group_status = 'ACTIVE'
                LEFT JOIN courses c ON c.group_id = g.id AND c.status = 'ACTIVE'
                LEFT JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND u.status != 'BLOCKED'
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new ResTeacherSummary(
                rs.getLong("total_teachers"),
                rs.getLong("teachers_with_groups"),
                rs.getLong("teachers_without_groups"),
                rs.getLong("active_courses"),
                rs.getObject("marking_rate_30d") != null ? rs.getDouble("marking_rate_30d") : null
        ), schoolUuid);
    }

    public List<ResTeacherActivity> findTeacherActivityBySchool(UUID schoolUuid) {
        String sql = """
                SELECT
                  u.uuid AS teacher_uuid,
                  u.first_name,
                  u.last_name,
                  COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'ACTIVE') AS active_groups,
                  COUNT(DISTINCT cl.id) AS total_lessons,
                  COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED') AS marked_attendances,
                  COUNT(ar.id) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked_attendances,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED') * 100.0
                    / NULLIF(COUNT(ar.id), 0),
                  1) AS attendance_marking_rate,
                  MAX(cl.start_time) AS last_lesson_at,
                  u.last_active_at
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                LEFT JOIN groups g ON g.teacher_id = t.id AND g.group_status = 'ACTIVE'
                LEFT JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                LEFT JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED' AND cl.start_time <= NOW()
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND u.status != 'BLOCKED'
                GROUP BY u.uuid, u.first_name, u.last_name, u.last_active_at
                ORDER BY last_lesson_at DESC NULLS LAST
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String uuid = rs.getString("teacher_uuid");
            Timestamp lastLesson = rs.getTimestamp("last_lesson_at");
            Timestamp lastActive = rs.getTimestamp("last_active_at");
            return new ResTeacherActivity(
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getLong("active_groups"),
                    rs.getLong("total_lessons"),
                    rs.getLong("marked_attendances"),
                    rs.getLong("not_marked_attendances"),
                    rs.getDouble("attendance_marking_rate"),
                    lastLesson != null ? lastLesson.toInstant() : null,
                    lastActive != null ? lastActive.toInstant() : null
            );
        }, schoolUuid);
    }

    public List<ResTeacherCourse> findCoursesByTeacher(UUID schoolUuid, UUID teacherUuid) {
        String sql = """
                SELECT
                  c.id AS course_id,
                  c.name AS course_name,
                  g.id AS group_id,
                  COALESCE(g.name, c.name) AS group_name,
                  c.status AS course_status,
                  c.created_at AS course_created_at,
                  CASE WHEN c.status = 'FINISHED' THEN MAX(COALESCE(cl.end_time, cl.start_time)) ELSE NULL END AS course_finished_at,
                  (SELECT COUNT(DISTINCT e.id) FROM enrollments e
                   WHERE e.group_id = g.id AND e.status != 'DELETED') AS total_students,
                  COUNT(DISTINCT cl.id) AS total_lessons,
                  COUNT(DISTINCT CASE WHEN ar.attendance_status != 'NOT_MARKED' THEN cl.id END) AS marked_lessons,
                  ROUND(
                    COUNT(DISTINCT CASE WHEN ar.attendance_status != 'NOT_MARKED' THEN cl.id END) * 100.0
                    / NULLIF(COUNT(DISTINCT cl.id), 0),
                  1) AS marking_rate,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE ar.attendance_status IN ('PRESENT', 'LATE')) * 100.0
                    / NULLIF(COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS student_attendance_rate
                FROM courses c
                JOIN groups g ON g.id = c.group_id AND g.group_status != 'DELETED'
                JOIN teacher t ON t.id = g.teacher_id
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = c.school_id
                LEFT JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                WHERE s.uuid = ?
                  AND u.uuid = ?
                  AND c.status != 'DELETED'
                GROUP BY c.id, c.name, g.id, g.name, c.status, c.created_at
                ORDER BY c.created_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp createdAt = rs.getTimestamp("course_created_at");
            Timestamp finishedAt = rs.getTimestamp("course_finished_at");
            return new ResTeacherCourse(
                    rs.getLong("course_id"),
                    rs.getString("course_name"),
                    rs.getLong("group_id"),
                    rs.getString("group_name"),
                    rs.getLong("total_students"),
                    rs.getLong("total_lessons"),
                    rs.getLong("marked_lessons"),
                    rs.getDouble("marking_rate"),
                    rs.getObject("student_attendance_rate") != null ? rs.getDouble("student_attendance_rate") : null,
                    rs.getString("course_status"),
                    createdAt != null ? createdAt.toInstant() : null,
                    finishedAt != null ? finishedAt.toInstant() : null
            );
        }, schoolUuid, teacherUuid);
    }

    public List<ResTeacherGroup> findGroupsByTeacher(UUID schoolUuid, UUID teacherUuid) {
        String sql = """
                SELECT
                  g.id AS group_id,
                  g.uuid AS group_uuid,
                  g.name AS group_name,
                  g.group_status,
                  COUNT(DISTINCT e.id) FILTER (WHERE e.status != 'DELETED') AS total_students,
                  COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'ACTIVE') AS active_courses,
                  COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'FINISHED') AS finished_courses,
                  COUNT(DISTINCT c.id) FILTER (WHERE c.status != 'DELETED') AS total_courses,
                  COUNT(DISTINCT cl.id) AS total_lessons,
                  COUNT(DISTINCT CASE WHEN ar.attendance_status != 'NOT_MARKED' THEN cl.id END) AS marked_lessons,
                  ROUND(
                    COUNT(DISTINCT CASE WHEN ar.attendance_status != 'NOT_MARKED' THEN cl.id END) * 100.0
                    / NULLIF(COUNT(DISTINCT cl.id), 0),
                  1) AS marking_rate,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE ar.attendance_status IN ('PRESENT', 'LATE')) * 100.0
                    / NULLIF(COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                JOIN groups g ON g.teacher_id = t.id AND g.group_status != 'DELETED'
                LEFT JOIN enrollments e ON e.group_id = g.id
                LEFT JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                LEFT JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                WHERE s.uuid = ?
                  AND u.uuid = ?
                GROUP BY g.id, g.uuid, g.name, g.group_status
                HAVING COUNT(DISTINCT c.id) FILTER (WHERE c.status != 'DELETED') > 0
                ORDER BY g.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String uuid = rs.getString("group_uuid");
            return new ResTeacherGroup(
                    rs.getLong("group_id"),
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("group_name"),
                    rs.getString("group_status"),
                    rs.getLong("total_students"),
                    rs.getLong("active_courses"),
                    rs.getLong("finished_courses"),
                    rs.getLong("total_courses"),
                    rs.getLong("total_lessons"),
                    rs.getLong("marked_lessons"),
                    rs.getObject("marking_rate") != null ? rs.getDouble("marking_rate") : null,
                    rs.getObject("attendance_rate") != null ? rs.getDouble("attendance_rate") : null,
                    null
            );
        }, schoolUuid, teacherUuid);
    }

    public List<ResTeacherLesson> findLessonsByTeacher(UUID schoolUuid, UUID teacherUuid) {
        String sql = """
                SELECT
                  cl.uuid AS lesson_uuid,
                  cl.name AS lesson_name,
                  cl.start_time,
                  cl.end_time,
                  cl.status,
                  cl.type_lesson,
                  c.id AS course_id,
                  c.name AS course_name,
                  g.name AS group_name,
                  COUNT(DISTINCT e.id) FILTER (WHERE e.status != 'DELETED') AS total_students,
                  COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED') AS marked_count,
                  ROUND(
                    (COUNT(ar.id) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(ar.id) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                JOIN groups g ON g.teacher_id = t.id AND g.group_status != 'DELETED'
                JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                LEFT JOIN enrollments e ON e.group_id = g.id
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                WHERE s.uuid = ?
                  AND u.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND cl.start_time <= NOW()
                GROUP BY cl.uuid, cl.name, cl.start_time, cl.end_time, cl.status, cl.type_lesson,
                         c.id, c.name, g.name
                ORDER BY cl.start_time DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String lessonUuid = rs.getString("lesson_uuid");
            Timestamp startTime = rs.getTimestamp("start_time");
            Timestamp endTime = rs.getTimestamp("end_time");
            return new ResTeacherLesson(
                    lessonUuid != null ? UUID.fromString(lessonUuid) : null,
                    rs.getString("lesson_name"),
                    startTime != null ? startTime.toInstant() : null,
                    endTime != null ? endTime.toInstant() : null,
                    rs.getString("status"),
                    rs.getString("type_lesson"),
                    rs.getLong("course_id"),
                    rs.getString("course_name"),
                    rs.getString("group_name"),
                    rs.getLong("total_students"),
                    rs.getLong("marked_count"),
                    rs.getDouble("attendance_rate")
            );
        }, schoolUuid, teacherUuid);
    }

    public List<ResTeacherAttendanceTrend> findTeacherAttendanceTrend(UUID schoolUuid, UUID teacherUuid, int months) {
        String sql = """
                SELECT
                  DATE_TRUNC('month', cl.start_time) AS month,
                  COUNT(DISTINCT cl.id) AS total_lessons,
                  COUNT(DISTINCT CASE WHEN ar.attendance_status != 'NOT_MARKED' THEN cl.id END) AS marked_lessons,
                  COUNT(DISTINCT cl.id) - COUNT(DISTINCT CASE WHEN ar.attendance_status != 'NOT_MARKED' THEN cl.id END) AS unmarked_lessons,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED') * 100.0
                    / NULLIF(COUNT(ar.id), 0),
                  1) AS marking_rate
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                JOIN groups g ON g.teacher_id = t.id AND g.group_status != 'DELETED'
                JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                WHERE s.uuid = ?
                  AND u.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND cl.start_time >= NOW() - (? * INTERVAL '1 month')
                  AND cl.start_time <= NOW()
                GROUP BY DATE_TRUNC('month', cl.start_time)
                ORDER BY month ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResTeacherAttendanceTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("total_lessons"),
                    rs.getLong("marked_lessons"),
                    rs.getLong("unmarked_lessons"),
                    rs.getDouble("marking_rate")
            );
        }, schoolUuid, teacherUuid, months);
    }

    public long[] findPaymentScalarsBySchool(UUID schoolUuid) {
        String sql = """
                WITH per_student AS (
                  SELECT
                    ft.student_id,
                    COALESCE(SUM(ft.amount)        FILTER (WHERE ft.type = 'PAYMENT'), 0) AS paid,
                    COALESCE(SUM(ABS(ft.amount))   FILTER (WHERE ft.type = 'CHARGE'),  0) AS charged,
                    COUNT(ft.id)                   FILTER (WHERE ft.type = 'PAYMENT')     AS payment_cnt
                  FROM finance_transactions ft
                  JOIN students st ON st.id = ft.student_id
                  JOIN schools sc  ON sc.id = st.school_id
                  WHERE sc.uuid = ?
                  AND ft.deleted = false
                  GROUP BY ft.student_id
                )
                SELECT
                  COALESCE(SUM(paid),    0)                          AS total_collected,
                  COALESCE(SUM(charged), 0)                          AS total_charged,
                  COALESCE(SUM(GREATEST(0, charged - paid)), 0)      AS total_outstanding,
                  COALESCE(SUM(payment_cnt), 0)                      AS payment_count,
                  COUNT(*) FILTER (WHERE payment_cnt > 0)            AS students_paid
                FROM per_student
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new long[]{
                rs.getLong("total_collected"),
                rs.getLong("total_charged"),
                rs.getLong("total_outstanding"),
                rs.getLong("payment_count"),
                rs.getLong("students_paid")
        }, schoolUuid);
    }

    public List<ResPaymentMethodStat> findPaymentByMethodBySchool(UUID schoolUuid) {
        String sql = """
                SELECT
                  ft.method,
                  COUNT(ft.id) AS cnt,
                  SUM(ft.amount) AS amount
                FROM finance_transactions ft
                JOIN students st ON st.id = ft.student_id
                JOIN schools sc ON sc.id = st.school_id
                WHERE sc.uuid = ? AND ft.type = 'PAYMENT'
                  AND ft.deleted = false
                GROUP BY ft.method
                ORDER BY amount DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new ResPaymentMethodStat(
                        rs.getString("method"),
                        rs.getLong("cnt"),
                        rs.getLong("amount")
                ), schoolUuid);
    }

    public List<ResPaymentTrend> findPaymentTrendBySchool(UUID schoolUuid, int months) {
        String sql = """
                SELECT
                  DATE_TRUNC('month', ft.created_at AT TIME ZONE 'Asia/Tashkent') AS month,
                  SUM(ft.amount) AS total_collected,
                  COUNT(ft.id) AS payment_count
                FROM finance_transactions ft
                JOIN students st ON st.id = ft.student_id
                JOIN schools sc ON sc.id = st.school_id
                WHERE sc.uuid = ? AND ft.type = 'PAYMENT'
                  AND ft.deleted = false
                  AND ft.created_at >= NOW() - (? * INTERVAL '1 month')
                GROUP BY DATE_TRUNC('month', ft.created_at AT TIME ZONE 'Asia/Tashkent')
                ORDER BY month ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResPaymentTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("total_collected"),
                    rs.getLong("payment_count")
            );
        }, schoolUuid, months);
    }

    public ResMauSummary findPlatformMauSummary() {
        String sql = """
                WITH student_mau AS (
                  SELECT COUNT(DISTINCT ua.user_id) AS cnt
                  FROM user_activity ua
                  WHERE ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                    AND ua.role = 'STUDENT'
                ),
                teacher_mau AS (
                  SELECT COUNT(DISTINCT ua.user_id) AS cnt
                  FROM user_activity ua
                  WHERE ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                    AND ua.role = 'TEACHER'
                ),
                active_schools AS (
                  SELECT COUNT(DISTINCT school_id) AS cnt FROM (
                    SELECT st.school_id FROM user_activity ua
                    JOIN students st ON st.user_id = ua.user_id
                    WHERE ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                    UNION
                    SELECT t.school_id FROM user_activity ua
                    JOIN teacher t ON t.user_id = ua.user_id
                    WHERE ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                  ) s
                )
                SELECT sm.cnt AS active_students, tm.cnt AS active_teachers, asc2.cnt AS active_schools
                FROM student_mau sm, teacher_mau tm, active_schools asc2
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new ResMauSummary(
                        rs.getLong("active_students"),
                        rs.getLong("active_teachers"),
                        rs.getLong("active_schools")
                ));
    }

    public List<ResMauTrend> findPlatformMauTrend(int months) {
        String sql = """
                WITH student_mau AS (
                  SELECT DATE_TRUNC('month', date) AS month, COUNT(DISTINCT user_id) AS active_students
                  FROM user_activity
                  WHERE role = 'STUDENT'
                    AND date >= (NOW() - (? * INTERVAL '1 month'))::date
                  GROUP BY 1
                ),
                teacher_mau AS (
                  SELECT DATE_TRUNC('month', date) AS month, COUNT(DISTINCT user_id) AS active_teachers
                  FROM user_activity
                  WHERE role = 'TEACHER'
                    AND date >= (NOW() - (? * INTERVAL '1 month'))::date
                  GROUP BY 1
                )
                SELECT
                  COALESCE(s.month, t.month) AS month,
                  COALESCE(s.active_students, 0) AS active_students,
                  COALESCE(t.active_teachers, 0) AS active_teachers
                FROM student_mau s
                FULL OUTER JOIN teacher_mau t ON t.month = s.month
                ORDER BY month ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResMauTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("active_students"),
                    rs.getLong("active_teachers")
            );
        }, months, months);
    }

    public List<ResMauBySchool> findMauBySchool() {
        String sql = """
                WITH student_mau AS (
                  SELECT st.school_id, COUNT(DISTINCT ua.user_id) AS active_students
                  FROM user_activity ua
                  JOIN students st ON st.user_id = ua.user_id
                  WHERE ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                  GROUP BY st.school_id
                ),
                teacher_mau AS (
                  SELECT t.school_id, COUNT(DISTINCT ua.user_id) AS active_teachers
                  FROM user_activity ua
                  JOIN teacher t ON t.user_id = ua.user_id
                  WHERE ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                  GROUP BY t.school_id
                )
                SELECT
                  sc.uuid AS school_uuid,
                  sc.name AS school_name,
                  COALESCE(sm.active_students, 0) AS active_students,
                  COALESCE(tm.active_teachers, 0) AS active_teachers
                FROM schools sc
                LEFT JOIN student_mau sm ON sm.school_id = sc.id
                LEFT JOIN teacher_mau tm ON tm.school_id = sc.id
                WHERE sc.status = 'ACTIVE'
                  AND (sm.active_students > 0 OR tm.active_teachers > 0)
                ORDER BY active_students DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String uuidStr = rs.getString("school_uuid");
            return new ResMauBySchool(
                    uuidStr != null ? UUID.fromString(uuidStr) : null,
                    rs.getString("school_name"),
                    rs.getLong("active_students"),
                    rs.getLong("active_teachers")
            );
        });
    }

    public ResMauSummary findSchoolMauSummary(UUID schoolUuid) {
        String sql = """
                WITH school AS (SELECT id FROM schools WHERE uuid = ? AND status = 'ACTIVE'),
                student_mau AS (
                  SELECT COUNT(DISTINCT ua.user_id) AS cnt
                  FROM user_activity ua
                  JOIN students st ON st.user_id = ua.user_id
                  WHERE st.school_id = (SELECT id FROM school)
                    AND ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                ),
                teacher_mau AS (
                  SELECT COUNT(DISTINCT ua.user_id) AS cnt
                  FROM user_activity ua
                  JOIN teacher t ON t.user_id = ua.user_id
                  WHERE t.school_id = (SELECT id FROM school)
                    AND ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                )
                SELECT sm.cnt AS active_students, tm.cnt AS active_teachers
                FROM student_mau sm, teacher_mau tm
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new ResMauSummary(
                        rs.getLong("active_students"),
                        rs.getLong("active_teachers"),
                        null
                ), schoolUuid);
    }

    public List<ResMauTrend> findSchoolMauTrend(UUID schoolUuid, int months) {
        String sql = """
                WITH school AS (SELECT id FROM schools WHERE uuid = ? AND status = 'ACTIVE'),
                student_mau AS (
                  SELECT DATE_TRUNC('month', ua.date) AS month, COUNT(DISTINCT ua.user_id) AS active_students
                  FROM user_activity ua
                  JOIN students st ON st.user_id = ua.user_id
                  WHERE st.school_id = (SELECT id FROM school)
                    AND ua.date >= (NOW() - (? * INTERVAL '1 month'))::date
                  GROUP BY 1
                ),
                teacher_mau AS (
                  SELECT DATE_TRUNC('month', ua.date) AS month, COUNT(DISTINCT ua.user_id) AS active_teachers
                  FROM user_activity ua
                  JOIN teacher t ON t.user_id = ua.user_id
                  WHERE t.school_id = (SELECT id FROM school)
                    AND ua.date >= (NOW() - (? * INTERVAL '1 month'))::date
                  GROUP BY 1
                )
                SELECT
                  COALESCE(s.month, t.month) AS month,
                  COALESCE(s.active_students, 0) AS active_students,
                  COALESCE(t.active_teachers, 0) AS active_teachers
                FROM student_mau s
                FULL OUTER JOIN teacher_mau t ON t.month = s.month
                ORDER BY month ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResMauTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("active_students"),
                    rs.getLong("active_teachers")
            );
        }, schoolUuid, months, months);
    }

    public ResSchoolInfo findSchoolInfo(UUID schoolUuid) {
        return jdbcTemplate.queryForObject(
                "SELECT uuid, name, status FROM schools WHERE uuid = ?",
                (rs, rowNum) -> new ResSchoolInfo(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getString("status")
                ), schoolUuid);
    }

    public ResSchoolOverview findSchoolOverview(UUID schoolUuid) {
        String sql = """
                WITH school AS (SELECT id FROM schools WHERE uuid = ? AND status = 'ACTIVE'),
                student_count AS (
                  SELECT COUNT(*) AS cnt FROM students st
                  JOIN users u ON u.id = st.user_id
                  WHERE st.school_id = (SELECT id FROM school) AND u.status != 'BLOCKED'
                ),
                teacher_count AS (
                  SELECT COUNT(*) AS cnt FROM teacher t
                  JOIN users u ON u.id = t.user_id
                  WHERE t.school_id = (SELECT id FROM school) AND u.status != 'BLOCKED'
                ),
                lesson_stats AS (
                  SELECT
                    COUNT(DISTINCT cl.id) AS total_lessons,
                    ROUND(
                      COUNT(ar.id) FILTER (WHERE ar.attendance_status IN ('PRESENT','LATE')) * 100.0
                      / NULLIF(COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0), 1
                    ) AS attendance_rate
                  FROM teacher t
                  JOIN groups g ON g.teacher_id = t.id
                  JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                  JOIN course_lessons cl ON cl.course_id = c.id AND cl.status = 'FINISHED'
                  LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                  WHERE t.school_id = (SELECT id FROM school)
                ),
                mau_students AS (
                  SELECT COUNT(DISTINCT ua.user_id) AS cnt
                  FROM user_activity ua
                  JOIN students st ON st.user_id = ua.user_id
                  WHERE st.school_id = (SELECT id FROM school)
                    AND ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                ),
                mau_teachers AS (
                  SELECT COUNT(DISTINCT ua.user_id) AS cnt
                  FROM user_activity ua
                  JOIN teacher t ON t.user_id = ua.user_id
                  WHERE t.school_id = (SELECT id FROM school)
                    AND ua.date >= DATE_TRUNC('month', CURRENT_DATE)
                ),
                enrolls AS (
                  SELECT COUNT(*) AS cnt FROM enrollments e
                  JOIN groups g ON g.id = e.group_id JOIN teacher t ON t.id = g.teacher_id
                  WHERE t.school_id = (SELECT id FROM school) AND e.status != 'DELETED'
                ),
                payments AS (
                  SELECT COALESCE(SUM(ft.amount), 0) AS total_collected FROM finance_transactions ft
                  JOIN students st ON st.id = ft.student_id
                  WHERE st.school_id = (SELECT id FROM school) AND ft.type = 'PAYMENT'
                  AND ft.deleted = false
                )
                SELECT sc.cnt AS total_students, tc.cnt AS total_teachers,
                  ls.total_lessons, ls.attendance_rate,
                  ms.cnt AS active_students_this_month, mt.cnt AS active_teachers_this_month,
                  e.cnt AS total_enrolled, p.total_collected
                FROM student_count sc, teacher_count tc, lesson_stats ls,
                     mau_students ms, mau_teachers mt, enrolls e, payments p
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new ResSchoolOverview(
                        rs.getLong("total_students"),
                        rs.getLong("total_teachers"),
                        rs.getLong("total_lessons"),
                        rs.getDouble("attendance_rate"),
                        rs.getLong("active_students_this_month"),
                        rs.getLong("active_teachers_this_month"),
                        rs.getLong("total_enrolled"),
                        rs.getLong("total_collected")
                ), schoolUuid);
    }

    public ResSchoolGrowthSummary findSchoolGrowthSummary() {
        String sql = """
                SELECT
                  COUNT(*) AS total_registered,
                  COUNT(*) FILTER (WHERE status = 'ACTIVE') AS total_active,
                  COUNT(*) FILTER (WHERE status != 'ACTIVE') AS total_inactive,
                  COUNT(*) FILTER (WHERE created_at >= DATE_TRUNC('month', NOW())) AS new_this_month,
                  ROUND(COUNT(*) FILTER (WHERE status != 'ACTIVE') * 100.0 / NULLIF(COUNT(*), 0), 1) AS churn_rate
                FROM schools
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new ResSchoolGrowthSummary(
                        rs.getLong("total_registered"),
                        rs.getLong("total_active"),
                        rs.getLong("total_inactive"),
                        rs.getLong("new_this_month"),
                        rs.getDouble("churn_rate")
                ));
    }

    public List<ResSchoolGrowthByMonth> findSchoolsByMonth(String month, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT uuid, name, status, created_at
                FROM schools
                WHERE DATE_TRUNC('month', created_at) = DATE_TRUNC('month', ?::timestamptz)
                """);
        List<Object> params = new ArrayList<>();
        params.add(month);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_at ASC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String uuidStr = rs.getString("uuid");
            Timestamp createdAt = rs.getTimestamp("created_at");
            return new ResSchoolGrowthByMonth(
                    uuidStr != null ? UUID.fromString(uuidStr) : null,
                    rs.getString("name"),
                    rs.getString("status"),
                    createdAt != null ? createdAt.toInstant() : null
            );
        }, params.toArray());
    }

    public List<ResSchoolGrowthTrend> findSchoolGrowthTrend(int months) {
        String sql = """
                SELECT
                  DATE_TRUNC('month', created_at) AS month,
                  COUNT(*) AS new_schools,
                  COUNT(*) FILTER (WHERE status = 'ACTIVE')  AS active_schools,
                  COUNT(*) FILTER (WHERE status = 'FROZEN')  AS frozen_schools,
                  COUNT(*) FILTER (WHERE status = 'DELETED') AS deleted_schools
                FROM schools
                WHERE created_at >= NOW() - (? * INTERVAL '1 month')
                GROUP BY 1
                ORDER BY 1 ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResSchoolGrowthTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("new_schools"),
                    rs.getLong("active_schools"),
                    rs.getLong("frozen_schools"),
                    rs.getLong("deleted_schools")
            );
        }, months);
    }

    public ResEnrollmentSummary findEnrollmentSummaryBySchool(UUID schoolUuid) {
        String sql = """
                SELECT
                  COUNT(*) FILTER (WHERE e.status != 'DELETED') AS total_enrolled,
                  COUNT(*) FILTER (WHERE e.status != 'DELETED' AND e.created_at >= DATE_TRUNC('month', NOW())) AS new_this_month,
                  COUNT(*) FILTER (WHERE e.status = 'DELETED') AS total_deleted,
                  COUNT(*) FILTER (WHERE e.status = 'DELETED' AND e.updated_at >= DATE_TRUNC('month', NOW())) AS deleted_this_month
                FROM enrollments e
                JOIN groups g ON g.id = e.group_id
                JOIN teacher t ON t.id = g.teacher_id
                JOIN schools s ON s.id = t.school_id
                WHERE s.uuid = ? AND s.status = 'ACTIVE'
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new ResEnrollmentSummary(
                        rs.getLong("total_enrolled"),
                        rs.getLong("new_this_month"),
                        rs.getLong("total_deleted"),
                        rs.getLong("deleted_this_month")
                ), schoolUuid);
    }

    public List<ResEnrollmentTrend> findEnrollmentTrendBySchool(UUID schoolUuid, int months) {
        String sql = """
                WITH school AS (SELECT id FROM schools WHERE uuid = ? AND status = 'ACTIVE'),
                new_cte AS (
                  SELECT DATE_TRUNC('month', e.created_at) AS month, COUNT(*) AS cnt
                  FROM enrollments e
                  JOIN groups g ON g.id = e.group_id
                  JOIN teacher t ON t.id = g.teacher_id
                  WHERE t.school_id = (SELECT id FROM school)
                    AND e.status != 'DELETED'
                    AND e.created_at >= NOW() - (? * INTERVAL '1 month')
                  GROUP BY 1
                ),
                deleted_cte AS (
                  SELECT DATE_TRUNC('month', e.updated_at) AS month, COUNT(*) AS cnt
                  FROM enrollments e
                  JOIN groups g ON g.id = e.group_id
                  JOIN teacher t ON t.id = g.teacher_id
                  WHERE t.school_id = (SELECT id FROM school)
                    AND e.status = 'DELETED'
                    AND e.updated_at >= NOW() - (? * INTERVAL '1 month')
                  GROUP BY 1
                ),
                months_series AS (
                  SELECT DATE_TRUNC('month', NOW() - (generate_series(0, ? - 1) * INTERVAL '1 month')) AS month
                )
                SELECT
                  m.month,
                  COALESCE(n.cnt, 0) AS new_enrollments,
                  0 AS new_students,
                  COALESCE(d.cnt, 0) AS deleted_enrollments
                FROM months_series m
                LEFT JOIN new_cte n ON n.month = m.month
                LEFT JOIN deleted_cte d ON d.month = m.month
                ORDER BY m.month ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResEnrollmentTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("new_enrollments"),
                    rs.getLong("new_students"),
                    rs.getLong("deleted_enrollments")
            );
        }, schoolUuid, months, months, months);
    }

    public List<ResEnrollmentTrend> findPlatformEnrollmentTrend(int months) {
        String sql = """
                WITH enroll_trend AS (
                  SELECT DATE_TRUNC('month', created_at) AS month, COUNT(*) AS new_enrollments
                  FROM enrollments
                  WHERE status != 'DELETED' AND created_at >= NOW() - (? * INTERVAL '1 month')
                  GROUP BY 1
                ),
                student_trend AS (
                  SELECT DATE_TRUNC('month', created_at) AS month, COUNT(*) AS new_students
                  FROM students
                  WHERE created_at >= NOW() - (? * INTERVAL '1 month')
                  GROUP BY 1
                )
                SELECT
                  COALESCE(e.month, s.month) AS month,
                  COALESCE(e.new_enrollments, 0) AS new_enrollments,
                  COALESCE(s.new_students, 0) AS new_students
                FROM enroll_trend e
                FULL OUTER JOIN student_trend s ON s.month = e.month
                ORDER BY month ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp month = rs.getTimestamp("month");
            return new ResEnrollmentTrend(
                    month != null ? month.toInstant() : null,
                    rs.getLong("new_enrollments"),
                    rs.getLong("new_students"),
                    0L
            );
        }, months, months);
    }

    public ResSubscriptionOverview findSubscriptionOverview() {
        String sql = """
                SELECT
                  COUNT(*) AS total_schools,
                  COUNT(*) FILTER (WHERE ai_explanation_enabled = true) AS ai_explanation,
                  COUNT(*) FILTER (WHERE ai_exercise_enabled = true) AS ai_exercise,
                  COUNT(*) FILTER (WHERE ai_speaking_enabled = true) AS ai_speaking,
                  COUNT(*) FILTER (WHERE ai_writing_enabled = true) AS ai_writing,
                  COALESCE(SUM(tokens_used), 0) AS total_tokens_used,
                  COALESCE(SUM(token_limit), 0) AS total_token_limit,
                  ROUND(SUM(tokens_used) * 100.0 / NULLIF(SUM(token_limit), 0), 1) AS token_usage_pct
                FROM school_subscriptions_active
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new ResSubscriptionOverview(
                        rs.getLong("total_schools"),
                        rs.getLong("ai_explanation"),
                        rs.getLong("ai_exercise"),
                        rs.getLong("ai_speaking"),
                        rs.getLong("ai_writing"),
                        rs.getLong("total_tokens_used"),
                        rs.getLong("total_token_limit"),
                        rs.getDouble("token_usage_pct")
                ));
    }

    public ResSchoolSubscription findSchoolSubscription(UUID schoolUuid) {
        String sql = """
                SELECT
                  ssa.token_limit,
                  ssa.tokens_used,
                  ROUND(ssa.tokens_used * 100.0 / NULLIF(ssa.token_limit, 0), 1) AS token_usage_pct,
                  ssa.ai_explanation_enabled,
                  ssa.ai_exercise_enabled,
                  ssa.ai_speaking_enabled,
                  ssa.ai_writing_enabled,
                  ssa.expires_at,
                  ssa.overdue_days_to_freeze
                FROM school_subscriptions_active ssa
                JOIN schools s ON s.id = ssa.school_id
                WHERE s.uuid = ? AND s.status = 'ACTIVE'
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Timestamp expiresAt = rs.getTimestamp("expires_at");
            return new ResSchoolSubscription(
                    rs.getLong("token_limit"),
                    rs.getLong("tokens_used"),
                    rs.getDouble("token_usage_pct"),
                    rs.getBoolean("ai_explanation_enabled"),
                    rs.getBoolean("ai_exercise_enabled"),
                    rs.getBoolean("ai_speaking_enabled"),
                    rs.getBoolean("ai_writing_enabled"),
                    expiresAt != null ? expiresAt.toInstant() : null,
                    rs.getInt("overdue_days_to_freeze")
            );
        }, schoolUuid);
    }

    public List<ResUserLastLogin> findInactiveUsers(int days, String role) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                    u.uuid,
                    u.first_name,
                    u.last_name,
                    u.username,
                    u.role,
                    u.status,
                    u.last_active_at,
                    EXTRACT(DAY FROM NOW() - u.last_active_at)::BIGINT AS days_inactive,
                    COALESCE(sa_s.name, t_s.name, st_s.name) AS school_name,
                    u.created_at
                FROM users u
                LEFT JOIN school_admins sa ON sa.user_id = u.id
                LEFT JOIN schools sa_s ON sa_s.id = sa.school_id
                LEFT JOIN teacher t ON t.user_id = u.id
                LEFT JOIN schools t_s ON t_s.id = t.school_id
                LEFT JOIN students st ON st.user_id = u.id
                LEFT JOIN schools st_s ON st_s.id = st.school_id
                WHERE u.role NOT IN ('SYS_ADMIN', 'EXTERNAL_SELLO')
                  AND u.status != 'BLOCKED'
                  AND (u.last_active_at IS NULL OR u.last_active_at < NOW() - (? * INTERVAL '1 day'))
                """);
        params.add(days);
        if (role != null && !role.isBlank()) {
            sql.append(" AND u.role = ?");
            params.add(role);
        }
        sql.append(" ORDER BY u.last_active_at ASC NULLS LAST, u.created_at ASC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String uuidStr = rs.getString("uuid");
            Timestamp lastActive = rs.getTimestamp("last_active_at");
            Timestamp createdAt = rs.getTimestamp("created_at");
            Object daysInactive = rs.getObject("days_inactive");
            return new ResUserLastLogin(
                    uuidStr != null ? UUID.fromString(uuidStr) : null,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getString("status"),
                    rs.getString("school_name"),
                    lastActive != null ? lastActive.toInstant() : null,
                    daysInactive != null ? ((Number) daysInactive).longValue() : null,
                    createdAt != null ? createdAt.toInstant() : null
            );
        }, params.toArray());
    }

    public List<GroupAttendanceDto> findAttendanceByGroup(UUID schoolUuid, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        String base = """
                SELECT
                  g.id AS group_id,
                  g.name AS group_name,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate
                FROM groups g
                JOIN teacher t ON t.id = g.teacher_id
                JOIN schools s ON s.id = t.school_id
                JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED' AND cl.start_time <= NOW()
                JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                WHERE s.uuid = ?
                  AND s.status = 'ACTIVE'
                  AND g.group_status != 'DELETED'
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        sql.append(" GROUP BY g.id, g.name ORDER BY attendance_rate ASC NULLS LAST");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new GroupAttendanceDto(
                rs.getLong("group_id"),
                rs.getString("group_name"),
                rs.getLong("present"),
                rs.getLong("absent"),
                rs.getLong("late"),
                rs.getLong("not_marked"),
                rs.getDouble("attendance_rate")
        ), params.toArray());
    }

    public List<GroupStudentAttendanceDto> findGroupStudentAttendance(UUID schoolUuid, Long groupId, Instant from, Instant to) {
        List<Object> params = new ArrayList<>();
        params.add(schoolUuid);
        params.add(groupId);
        String base = """
                SELECT
                  st.uuid AS student_uuid,
                  u.first_name,
                  u.last_name,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') AS present,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'ABSENT')  AS absent,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')    AS late,
                  COUNT(*) FILTER (WHERE ar.attendance_status = 'NOT_MARKED') AS not_marked,
                  COUNT(*) AS total,
                  ROUND(
                    (COUNT(*) FILTER (WHERE ar.attendance_status = 'PRESENT') + COUNT(*) FILTER (WHERE ar.attendance_status = 'LATE')) * 100.0
                    / NULLIF(COUNT(*) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS rate
                FROM groups g
                JOIN teacher t ON t.id = g.teacher_id
                JOIN schools s ON s.id = t.school_id
                JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED' AND cl.start_time <= NOW()
                JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                JOIN students st ON st.id = ar.student_id
                JOIN users u ON u.id = st.user_id
                WHERE s.uuid = ?
                  AND g.id = ?
                  AND g.group_status != 'DELETED'
                """;
        StringBuilder sql = new StringBuilder(base);
        appendDateRange(sql, params, from, to);
        sql.append(" GROUP BY st.uuid, u.first_name, u.last_name ORDER BY rate ASC NULLS LAST");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String uuid = rs.getString("student_uuid");
            return new GroupStudentAttendanceDto(
                    uuid != null ? UUID.fromString(uuid) : null,
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getLong("present"),
                    rs.getLong("absent"),
                    rs.getLong("late"),
                    rs.getLong("not_marked"),
                    rs.getLong("total"),
                    rs.getDouble("rate")
            );
        }, params.toArray());
    }

    public boolean groupBelongsToSchool(UUID schoolUuid, Long groupId) {
        String sql = """
                SELECT COUNT(*) FROM groups g
                JOIN branches b ON b.id = g.branch_id
                JOIN schools s ON s.id = b.school_id
                WHERE s.uuid = ? AND g.id = ?
                """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, schoolUuid, groupId);
        return count != null && count > 0;
    }

    public List<ResGroupStudentSummary> findGroupStudentSummary(UUID schoolUuid, Long groupId) {
        String sql = """
                SELECT
                  st.uuid AS student_uuid,
                  u.first_name,
                  u.last_name,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE ar.attendance_status IN ('PRESENT', 'LATE')) * 100.0
                    / NULLIF(COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS attendance_rate,
                  ROUND(AVG(sa.score) FILTER (WHERE sa.score IS NOT NULL), 1) AS avg_grade,
                  COALESCE(SUM(po.total_amount) FILTER (WHERE po.status = 'SUCCESS'), 0) AS total_paid
                FROM enrollments e
                JOIN students st ON st.id = e.student_id
                JOIN users u ON u.id = st.user_id
                JOIN groups g ON g.id = e.group_id
                JOIN branches b ON b.id = g.branch_id
                JOIN schools s ON s.id = b.school_id
                LEFT JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                LEFT JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id AND ar.student_id = st.id
                LEFT JOIN student_answers sa ON sa.student_id = st.id
                LEFT JOIN payment_orders po ON po.course_id = c.id AND po.student_id = st.id
                WHERE s.uuid = ?
                  AND g.id = ?
                  AND e.status = 'ONGOING'
                GROUP BY st.uuid, u.first_name, u.last_name
                ORDER BY u.last_name, u.first_name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            double avgGradeRaw = rs.getDouble("avg_grade");
            Double avgGrade = rs.wasNull() ? null : avgGradeRaw;
            return new ResGroupStudentSummary(
                    rs.getString("student_uuid"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getDouble("attendance_rate"),
                    avgGrade,
                    rs.getLong("total_paid")
            );
        }, schoolUuid, groupId);
    }

    public boolean teacherBelongsToSchool(UUID schoolUuid, UUID teacherUuid) {
        String sql = """
                SELECT COUNT(*)
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                WHERE s.uuid = ? AND u.uuid = ?
                """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, schoolUuid, teacherUuid);
        return count != null && count > 0;
    }

    public List<TeacherGroupRetentionDto> findTeacherRetentionByGroup(UUID schoolUuid, UUID teacherUuid) {
        String sql = """
                SELECT
                  g.id AS group_id,
                  g.name AS group_name,
                  COUNT(DISTINCT e.student_id) AS enrolled,
                  COUNT(DISTINCT e.student_id) FILTER (WHERE e.status != 'DELETED') AS retained,
                  COUNT(DISTINCT e.student_id) FILTER (WHERE e.status = 'DELETED') AS dropped,
                  ROUND(
                    COUNT(DISTINCT e.student_id) FILTER (WHERE e.status != 'DELETED') * 100.0
                    / NULLIF(COUNT(DISTINCT e.student_id), 0),
                  1) AS retention_rate
                FROM teacher t
                JOIN users u ON u.id = t.user_id
                JOIN schools s ON s.id = t.school_id
                JOIN groups g ON g.teacher_id = t.id AND g.group_status != 'DELETED'
                JOIN enrollments e ON e.group_id = g.id
                WHERE s.uuid = ?
                  AND u.uuid = ?
                GROUP BY g.id, g.name
                HAVING COUNT(DISTINCT e.student_id) > 0
                ORDER BY g.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeacherGroupRetentionDto(
                rs.getLong("group_id"),
                rs.getString("group_name"),
                rs.getLong("enrolled"),
                rs.getLong("retained"),
                rs.getLong("dropped"),
                rs.getDouble("retention_rate")
        ), schoolUuid, teacherUuid);
    }

    public List<ResPaymentByStudent> findPaymentsByStudentBySchool(UUID schoolUuid) {
        String sql = """
                SELECT
                  u.uuid AS student_uuid,
                  u.first_name || ' ' || u.last_name AS student_name,
                  COALESCE(SUM(ABS(ft.amount)) FILTER (WHERE ft.type = 'CHARGE'), 0) AS total_charged,
                  COALESCE(SUM(ft.amount) FILTER (WHERE ft.type = 'PAYMENT'), 0) AS total_paid,
                  COALESCE(SUM(ABS(ft.amount)) FILTER (WHERE ft.type = 'CHARGE'), 0)
                    - COALESCE(SUM(ft.amount) FILTER (WHERE ft.type = 'PAYMENT'), 0) AS balance,
                  MAX(ft.created_at) FILTER (WHERE ft.type = 'PAYMENT') AS last_payment_at
                FROM finance_transactions ft
                JOIN students st ON st.id = ft.student_id
                JOIN users u ON u.id = st.user_id
                JOIN schools sc ON sc.id = st.school_id
                WHERE sc.uuid = ?
                AND ft.deleted = false
                GROUP BY u.uuid, u.first_name, u.last_name
                ORDER BY total_paid DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String uuidStr = rs.getString("student_uuid");
            Timestamp lastPay = rs.getTimestamp("last_payment_at");
            return new ResPaymentByStudent(
                    uuidStr != null ? UUID.fromString(uuidStr) : null,
                    rs.getString("student_name"),
                    rs.getLong("total_charged"),
                    rs.getLong("total_paid"),
                    rs.getLong("balance"),
                    lastPay != null ? lastPay.toInstant() : null
            );
        }, schoolUuid);
    }

    public ResLearningSummary findLearningSummaryBySchool(UUID schoolUuid) {
        String sql = """
                SELECT
                  (SELECT COUNT(DISTINCT cl.id)
                   FROM course_lessons cl
                   JOIN courses c ON c.id = cl.course_id
                   JOIN schools s ON s.id = c.school_id
                   WHERE s.uuid = ?
                     AND cl.status = 'FINISHED'
                     AND c.status != 'DELETED') AS lessons_completed,
                  (SELECT COUNT(*)
                   FROM student_unit_progress sup
                   JOIN students st ON st.id = sup.student_id
                   JOIN schools s ON s.id = st.school_id
                   WHERE s.uuid = ?
                     AND sup.status = 'COMPLETED') AS units_completed,
                  ROUND(
                    COUNT(DISTINCT CASE WHEN sa.ever_correct = true OR (sa.score IS NOT NULL AND sa.score > 0)
                                        THEN sa.student_id::text || '-' || sa.exercise_task_id::text END) * 100.0
                    / NULLIF(COUNT(DISTINCT sa.student_id::text || '-' || sa.exercise_task_id::text), 0),
                  1) AS homework_completion_rate,
                  ROUND(COALESCE(AVG(sa.score) FILTER (WHERE sa.score IS NOT NULL), 0), 1) AS average_homework_grade
                FROM student_answers sa
                JOIN students st ON st.id = sa.student_id
                JOIN schools s ON s.id = st.school_id
                WHERE s.uuid = ?
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new ResLearningSummary(
                        rs.getLong("lessons_completed"),
                        rs.getLong("units_completed"),
                        rs.getDouble("homework_completion_rate"),
                        rs.getDouble("average_homework_grade")
                ), schoolUuid, schoolUuid, schoolUuid);
    }

    public List<ResGroupAnalytics> findGroupAnalyticsBySchool(UUID schoolUuid) {
        String sql = """
                SELECT
                  g.id AS group_id,
                  g.name AS group_name,
                  CASE WHEN COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'ACTIVE') > 0
                       THEN 'ACTIVE' ELSE 'FINISHED' END AS course_status,
                  COUNT(DISTINCT e.id) FILTER (WHERE e.status = 'ONGOING') AS student_count,
                  ROUND(
                    COUNT(ar.id) FILTER (WHERE ar.attendance_status IN ('PRESENT', 'LATE')) * 100.0
                    / NULLIF(COUNT(ar.id) FILTER (WHERE ar.attendance_status != 'NOT_MARKED'), 0),
                  1) AS avg_attendance_rate,
                  ROUND(
                    COUNT(DISTINCT CASE WHEN ar.attendance_status != 'NOT_MARKED' THEN cl.id END) * 100.0
                    / NULLIF(COUNT(DISTINCT cl.id), 0),
                  1) AS avg_marking_rate,
                  ROUND(AVG(sa.score) FILTER (WHERE sa.score IS NOT NULL), 1) AS avg_grade,
                  COALESCE(SUM(po.total_amount) FILTER (WHERE po.status = 'SUCCESS'), 0) AS revenue_generated
                FROM groups g
                JOIN branches b ON b.id = g.branch_id
                JOIN schools s ON s.id = b.school_id
                LEFT JOIN enrollments e ON e.group_id = g.id
                LEFT JOIN courses c ON c.group_id = g.id AND c.status != 'DELETED'
                LEFT JOIN course_lessons cl ON cl.course_id = c.id AND cl.status != 'DELETED'
                LEFT JOIN attendance_records ar ON ar.course_lesson_id = cl.id
                LEFT JOIN students st_enrolled ON st_enrolled.id = ar.student_id
                LEFT JOIN student_answers sa ON sa.student_id = st_enrolled.id
                LEFT JOIN payment_orders po ON po.course_id = c.id
                  AND po.student_id IN (SELECT e2.student_id FROM enrollments e2 WHERE e2.group_id = g.id AND e2.status = 'ONGOING')
                WHERE s.uuid = ?
                  AND g.group_status != 'DELETED'
                GROUP BY g.id, g.name
                HAVING COUNT(DISTINCT c.id) FILTER (WHERE c.status != 'DELETED') > 0
                ORDER BY g.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            double avgGradeRaw = rs.getDouble("avg_grade");
            Double avgGrade = rs.wasNull() ? null : avgGradeRaw;
            return new ResGroupAnalytics(
                    rs.getLong("group_id"),
                    rs.getString("group_name"),
                    rs.getString("course_status"),
                    rs.getLong("student_count"),
                    rs.getDouble("avg_attendance_rate"),
                    rs.getDouble("avg_marking_rate"),
                    avgGrade,
                    rs.getLong("revenue_generated")
            );
        }, schoolUuid);
    }

}