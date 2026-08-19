package uz.tune.mentourBiz.rest.repository.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.enums.LessonStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseLessonRepo extends BaseRepository<CourseLesson> {

    List<CourseLesson> findAllByCourse_Group_UuidAndStatusIn(UUID groupUuid, Collection<LessonStatus> statuses);

    long countByCourse_Group_UuidAndStatusIn(UUID groupUuid, Collection<LessonStatus> statuses);

    @Query("""
    SELECT COUNT(l) FROM CourseLesson l
    WHERE l.course.school.uuid IN :schoolUuids AND l.endTime < :now AND l.status != 'DELETED' AND l.status != 'FINISHED'
    AND (:teacherUuid IS NULL OR l.course.group.teacher.user.uuid = :teacherUuid)
    AND (l.course.group.groupStatus = 'ACTIVE')
    AND EXISTS (SELECT ar FROM AttendanceRecord ar WHERE ar.lesson = l AND ar.isMarked = false)
""")
    long countMissingAttendanceMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("teacherUuid") UUID teacherUuid, @Param("now") Instant now);

    @Query("""
    SELECT l FROM CourseLesson l 
    WHERE l.course.school.uuid IN :schoolUuids AND l.endTime < :now AND l.status != 'DELETED' AND l.status != 'FINISHED' 
    AND (:teacherUuid IS NULL OR l.course.group.teacher.user.uuid = :teacherUuid)
    AND (l.course.group.groupStatus = 'ACTIVE')
    AND EXISTS (SELECT ar FROM AttendanceRecord ar WHERE ar.lesson = l AND ar.isMarked = false)
""")
    Page<CourseLesson> findMissingAttendanceMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("teacherUuid") UUID teacherUuid, @Param("now") Instant now, Pageable pageable);


    @Query("""
    SELECT l FROM CourseLesson l 
    WHERE l.course.school.uuid IN :schoolUuids AND l.endTime < :now AND l.status != 'DELETED' AND l.status != 'FINISHED'
    AND (:groupUuid IS NULL OR l.course.group.uuid = :groupUuid
         AND l.course.group.groupStatus = 'ACTIVE') 
    AND EXISTS (SELECT ar FROM AttendanceRecord ar WHERE ar.lesson = l AND ar.isMarked = false)
""")
    Page<CourseLesson> findMissingByGroup(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("groupUuid") UUID groupUuid, @Param("now") Instant now, Pageable pageable);
    Page<CourseLesson> findAllByCourse_School_UuidInAndStatusIn(Collection<UUID> schoolUuids, List<LessonStatus> statuses, Pageable pageable);
    @Query("SELECT DISTINCT cl FROM CourseLesson cl " +
            "LEFT JOIN cl.course c " +
            "LEFT JOIN c.school s " +
            "LEFT JOIN c.mentor m " +
            "WHERE (:name IS NULL OR LOWER(cl.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
            "AND (:statuses IS NULL OR cl.status IN :statuses) " +
            "AND (c.status <> 'DELETED') " +
            "AND (COALESCE(:schoolUuids, NULL) IS NULL OR s.uuid IN :schoolUuids) " +
            "AND (:mentorId IS NULL OR m.user.id = :mentorId) " +
            "AND (:courseUuids IS NULL OR c.uuid IN :courseUuids) " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR cl.startTime >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR cl.startTime <= :endDate) " +
            "ORDER BY cl.startTime ASC")
    Page<CourseLesson> findWithFilters(
            @Param("name") String name,
            @Param("statuses") List<LessonStatus> statuses,
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("mentorId") Long mentorId,
            @Param("courseUuids") List<UUID> courseUuids,
            @Param("startDate") java.time.Instant startDate,
            @Param("endDate") java.time.Instant endDate,
            Pageable pageable);

    @Query("SELECT MAX(cl.createdAt) FROM CourseLesson cl WHERE cl.course.school.uuid = :schoolUuid AND cl.status <> 'DELETED'")
    java.time.Instant findLastLessonCreatedAtBySchool(@Param("schoolUuid") java.util.UUID schoolUuid);

//    boolean existsByUnitAndCourse_GroupAndStatusNotAndIdNot(Unit unit, Group group, LessonStatus status, Long lessonId);

    // dashboard
//    @EntityGraph(attributePaths = {"course", "course.school"})
//    Page<CourseLesson> findAllByStatusNotIn(List<LessonStatus> status, Pageable pageable);
    @EntityGraph(attributePaths = {"course", "course.school"})
    Page<CourseLesson> findAllByStatusIn(List<LessonStatus> status, Pageable pageable);
//    @EntityGraph(attributePaths = {"course", "course.school"})
//

//    @EntityGraph(attributePaths = {"course", "course.school"})
//

    List<CourseLesson> findAllByCourse(Course course);

//
//
//
//

    Page<CourseLesson> findAllByCourse_UuidInAndStatusIn(List<UUID> courseUuid, List<LessonStatus> lessonStatus, Pageable pageable);

//
//

//    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (cl.end_time - cl.start_time))), 0) / 3600 " +
//            "FROM course_lessons cl WHERE cl.course_id = (SELECT id FROM courses WHERE uuid = :courseUuid) " +
//            "AND cl.status != 'DELETED'", nativeQuery = true)
//

    Optional<CourseLesson> findByUuid(UUID uuid);

    // ---- Teacher payroll: per-lesson conductor attribution ----------------------------------------

    // A lesson counts towards payroll once it has taken place. FINISHED is only stamped when the whole
    // course is finalized, so a lesson that has already been taught normally still carries the status it
    // was created with (STUDENT_APP) — filtering on FINISHED alone reports zero lessons for every group
    // of a running course. Every status except DELETED is therefore in scope, and the caller's
    // toExclusive is what keeps lessons that have not happened yet out of the count.
    //
    // The course has to be checked too, not just the lesson: deleting a course leaves its lessons with
    // their original status, so they went on being counted. That inflated totalLessons — the denominator
    // the fixed-per-student fee is prorated by — with lessons nobody teaches and nobody marks attendance
    // for, quietly shrinking that part of the salary towards zero.

    /**
     * Distinct group uuids in which a teacher conducted at least one lesson in the period — as the
     * group's default teacher (no substitute set) or as an explicit substitute. Used to discover groups
     * where the teacher earns even though they are not the group's regular teacher.
     */
    // The conductor joins have to be spelled out as LEFT JOINs. A path expression like
    // l.conductedByTeacher.user.uuid is an implicit INNER JOIN, which drops every lesson with no
    // substitute set before the OR is even evaluated — so the "no substitute, credit the group's regular
    // teacher" branch could never match and every teacher was credited with zero lessons.

    @Query("""
        SELECT DISTINCT g.uuid FROM CourseLesson l
            JOIN l.course c
            JOIN c.group g
            LEFT JOIN l.conductedByTeacher ct
            LEFT JOIN ct.user ctu
            LEFT JOIN g.teacher gt
            LEFT JOIN gt.user gtu
        WHERE l.status IN ('NEW', 'STARTED', 'STUDENT_APP', 'FINISHED')
          AND c.status <> 'DELETED'
          AND l.startTime >= :from AND l.startTime < :toExclusive
          AND (ctu.uuid = :teacherUuid OR (ct IS NULL AND gtu.uuid = :teacherUuid))
    """)
    List<UUID> findGroupsConductedByTeacher(
            @Param("teacherUuid") UUID teacherUuid,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    /**
     * Count of conducted lessons per group in the period. Row: [groupUuid (UUID), count (Long)].
     */
    @Query("""
        SELECT g.uuid, COUNT(l) FROM CourseLesson l
            JOIN l.course c
            JOIN c.group g
        WHERE l.status IN ('NEW', 'STARTED', 'STUDENT_APP', 'FINISHED')
          AND c.status <> 'DELETED'
          AND l.startTime >= :from AND l.startTime < :toExclusive
          AND g.uuid IN :groupUuids
        GROUP BY g.uuid
    """)
    List<Object[]> countLessonsByGroup(
            @Param("groupUuids") Collection<UUID> groupUuids,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    /**
     * Count of conducted lessons per group that were effectively conducted by the given teacher
     * (explicit substitute, or the group's default teacher when no substitute is set) in the period.
     * Row: [groupUuid (UUID), count (Long)].
     */
    @Query("""
        SELECT g.uuid, COUNT(l) FROM CourseLesson l
            JOIN l.course c
            JOIN c.group g
            LEFT JOIN l.conductedByTeacher ct
            LEFT JOIN ct.user ctu
            LEFT JOIN g.teacher gt
            LEFT JOIN gt.user gtu
        WHERE l.status IN ('NEW', 'STARTED', 'STUDENT_APP', 'FINISHED')
          AND c.status <> 'DELETED'
          AND l.startTime >= :from AND l.startTime < :toExclusive
          AND g.uuid IN :groupUuids
          AND (ctu.uuid = :teacherUuid OR (ct IS NULL AND gtu.uuid = :teacherUuid))
        GROUP BY g.uuid
    """)
    List<Object[]> countLessonsByGroupForTeacher(
            @Param("groupUuids") Collection<UUID> groupUuids,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);
//
//
//
//

    // PLUGIN

    // mentor
//
//    // school admin/moderator
//    @Query("SELECT cl FROM CourseLesson cl WHERE cl.name = :name AND cl.course.school.uuid = :schoolUuid ORDER BY cl.startTime DESC")
//
//    // sys admin
//

}