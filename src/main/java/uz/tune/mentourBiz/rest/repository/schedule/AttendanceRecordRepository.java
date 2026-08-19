package uz.tune.mentourBiz.rest.repository.schedule;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
//
//
//
    List<AttendanceRecord> findAllByStudent_User_Uuid(UUID uuid);
//

    @Query("""
    SELECT ar FROM AttendanceRecord ar
    WHERE ar.student.user.uuid = :studentUuid
    AND ar.lesson.endTime <= :now
    AND ar.lesson.status != 'DELETED'
    ORDER BY ar.lesson.endTime DESC
    LIMIT 1
""")
    Optional<AttendanceRecord> findTopByStudentUserUuidOrderByLessonEndTimeDesc(
            @Param("studentUuid") UUID studentUuid,
            @Param("now") Instant now
    );

    //'PRESENT', 'LATE'
    @Query("""
    SELECT AVG(CASE WHEN ar.status IN ('PRESENT','LATE') THEN 100.0 ELSE 0.0 END)
    FROM AttendanceRecord ar
    WHERE ar.lesson.course.school.uuid IN :uuids
    AND ar.isMarked = true
""")
    Double getAvgAttendanceIn(@Param("uuids") Collection<UUID> uuids);

    @Query("""
    SELECT AVG(CASE WHEN ar.status IN ('PRESENT','LATE') THEN 100.0 ELSE 0.0 END)
    FROM AttendanceRecord ar
    WHERE ar.lesson.course.school.uuid = :schoolUuid
    AND ar.isMarked = true
    AND ar.lesson.startTime >= :startOfDay
    AND ar.lesson.startTime < :endOfDay
""")
    Double getTodayAttendancePercentage(
            @Param("schoolUuid") UUID schoolUuid,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );

    @Query("""
    SELECT ar FROM AttendanceRecord ar 
    JOIN ar.lesson l 
    WHERE ar.isAttendanceNotified = false 
    AND ar.isMarked = true
    AND l.endTime <= :now 
    AND l.status != 'DELETED'
""")
    List<AttendanceRecord> findPendingAttendanceNotifications(@Param("now") java.time.Instant now);

    @Query("""
    SELECT ar FROM AttendanceRecord ar 
    JOIN ar.lesson l 
    WHERE (ar.isAttendanceNotified = false OR ar.isUnitNotified = false) 
    AND l.endTime <= :now 
    AND l.status != 'DELETED'
""")
    List<AttendanceRecord> findPendingNotifications(@Param("now") java.time.Instant now);

    /**
     * Teacher payroll (fixed-per-student): count of attendance records per group where the lesson was
     * effectively conducted by the given teacher (explicit substitute, or the group's default teacher)
     * within the period. Each record is one student who actually attended one of the teacher's lessons,
     * so this is the numerator for prorating the per-student fee by lessons attended.
     * Row: [groupUuid (UUID), count (Long)].
     *
     * <p>{@code statuses} decides which attendance counts as "taught" — payroll passes PRESENT and LATE,
     * so an absence never earns the teacher the per-student fee. Without that filter the payout would
     * depend on whether the front-end bothers to mark absentees at all.
     *
     * <p>The conductor joins are spelled out as LEFT JOINs on purpose: a path expression like
     * {@code ar.lesson.conductedByTeacher.user.uuid} is an implicit INNER JOIN and would drop every
     * lesson taught without a substitute, leaving the per-student fee at zero for everybody.
     *
     * <p>Lessons of a deleted course are excluded, matching the lesson counts this is divided by.
     */
    @Query("""
        SELECT g.uuid, COUNT(ar)
        FROM AttendanceRecord ar
            JOIN ar.lesson l
            JOIN l.course c
            JOIN c.group g
            LEFT JOIN l.conductedByTeacher ct
            LEFT JOIN ct.user ctu
            LEFT JOIN g.teacher gt
            LEFT JOIN gt.user gtu
        WHERE ar.isMarked = true
          AND ar.status IN :statuses
          AND l.status IN ('NEW', 'STARTED', 'STUDENT_APP', 'FINISHED')
          AND c.status <> 'DELETED'
          AND l.startTime >= :from AND l.startTime < :toExclusive
          AND g.uuid IN :groupUuids
          AND (ctu.uuid = :teacherUuid OR (ct IS NULL AND gtu.uuid = :teacherUuid))
        GROUP BY g.uuid
    """)
    List<Object[]> countAttendanceByGroupForTeacher(
            @Param("groupUuids") Collection<UUID> groupUuids,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("statuses") Collection<AttendanceStatus> statuses,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    Optional<AttendanceRecord> findByLessonAndStudent(CourseLesson lesson, Student student);
    // Eskidan qolgan dublikat yozuvlar borligi uchun Optional variant NonUniqueResult bilan yiqiladi.
    List<AttendanceRecord> findAllByLessonAndStudent(CourseLesson lesson, Student student);
    List<AttendanceRecord> findAllByLesson(CourseLesson lesson);
    List<AttendanceRecord> findAllByLessonIn(List<CourseLesson> lessons);
    List<AttendanceRecord> findAllByStudentAndLessonIn(Student student, List<CourseLesson> lessons);
    List<AttendanceRecord> findAllByLesson_IdInAndStudent_IdIn(List<Long> lessonIds, List<Long> studentIds);

    @Query("""
    SELECT ar FROM AttendanceRecord ar
    WHERE ar.student.uuid = :studentUuid
    AND ar.lesson.course.group.uuid = :groupUuid
    AND ar.lesson.status IN ('STUDENT_APP', 'FINISHED')
    AND ar.lesson.startTime <= :until
    AND ar.isMarked = true
""")
    List<AttendanceRecord> findActiveForStudentInGroup(
            @Param("groupUuid") UUID groupUuid,
            @Param("studentUuid") UUID studentUuid,
            @Param("until") Instant until);

    List<AttendanceRecord> findAllByStudentUuid(UUID studentUuid);
}