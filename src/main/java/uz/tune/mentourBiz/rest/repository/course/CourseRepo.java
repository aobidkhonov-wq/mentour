package uz.tune.mentourBiz.rest.repository.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Mentor;
import uz.tune.mentourBiz.rest.enums.CourseStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepo extends BaseRepository<Course> {

    @EntityGraph(attributePaths = {"school", "mentor.user", "moderator.user", "lessons"})
    Optional<Course> findByUuid(UUID courseId);

    List<Course> findAllBySchoolUuid(UUID schoolUuid);

    @Query("SELECT c FROM Course c WHERE c.school.uuid = :schoolUuid AND c.group.level.uuid = :levelUuid AND c.status = 'ACTIVE'")
    List<Course> findAllBySchoolAndLevel(@Param("schoolUuid") UUID schoolUuid, @Param("levelUuid") UUID levelUuid);

    @Query("SELECT c FROM Course c WHERE c.school.organization.uuid = :orgUuid AND c.group.level.uuid = :levelUuid AND c.status = 'ACTIVE'")
    List<Course> findAllByOrganizationAndLevel(@Param("orgUuid") UUID orgUuid, @Param("levelUuid") UUID levelUuid);

    Page<Course> findAllByGroup_UuidAndStatusIn(UUID groupUuid, List<CourseStatus> statuses, Pageable pageable);

    List<Course> findAllByStatusAndUpdatedAtBefore(CourseStatus status, Instant timestamp);
    List<Course> findAllByGroupAndStatus(Group group, CourseStatus status);
    List<Course> findAllByPaymentPackage_UuidAndStatus(UUID packageUuid, CourseStatus status);

    @EntityGraph(attributePaths = {"school", "mentor.user", "moderator.user", "books"})
    Page<Course> findAll(Pageable pageable);
    List<Course> findAllBySchool_Uuid(UUID schoolUuid);
    @EntityGraph(attributePaths = {"school", "mentor.user", "moderator.user"})
    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    @Query("SELECT c FROM Course c " +
            "LEFT JOIN c.school s " +
            "LEFT JOIN c.group g " +
            "LEFT JOIN c.linkedBooks b " +
            "LEFT JOIN c.mentor m " +
            "LEFT JOIN c.paymentPackage pp " +
            "WHERE (:status IS NULL OR c.status = :status) " +
            "AND (COALESCE(:schoolUuids, NULL) IS NULL OR s.uuid IN :schoolUuids) " +
            "AND (:mentorId IS NULL OR m.user.id = :mentorId) " +
            "AND (COALESCE(:groupIds, NULL) IS NULL OR g.uuid IN :groupIds) " +
            "AND (:courseName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:courseName AS string), '%'))) " +
            "AND (:className IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:className AS string), '%'))) " +
            "AND (:packageUuid IS NULL OR pp.uuid = :packageUuid) " +
            "AND (:bookName IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', CAST(:bookName AS string), '%')))")
    Page<Course> findWithFilters(
            @Param("status") CourseStatus status,
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("mentorId") Long mentorId,
            @Param("groupIds") List<UUID> groupIds,
            @Param("courseName") String courseName,
            @Param("className") String className,
            @Param("bookName") String bookName,
            @Param("packageUuid") UUID packageUuid,
            Pageable pageable);

    // "Overdue" courses: still ACTIVE but running longer than the school's configured course
    // length (same soft rule as CourseServiceImpl.computeCourseDuration - only counts when the
    // school has defaultCourseLengthInstalled, limit = defaultCourseLengthDays, fallback 90).
    @Query("""
    SELECT COUNT(c) FROM Course c
    JOIN SchoolAcademicConfig cfg ON cfg.school = c.school
    LEFT JOIN c.group g
    LEFT JOIN g.teacher t
    LEFT JOIN t.user tu
    WHERE c.school.uuid IN :schoolUuids
    AND c.status = 'ACTIVE'
    AND cfg.defaultCourseLengthInstalled = true
    AND (:teacherUuid IS NULL OR tu.uuid = :teacherUuid)
    AND timestampadd(day,
            CASE WHEN cfg.defaultCourseLengthDays IS NULL OR cfg.defaultCourseLengthDays <= 0 THEN 90 ELSE cfg.defaultCourseLengthDays END,
            COALESCE(c.courseStartDate, c.createdAt)) < :now
""")
    long countOverdueCoursesMulti(@Param("schoolUuids") Collection<UUID> schoolUuids,
                                  @Param("teacherUuid") UUID teacherUuid,
                                  @Param("now") Instant now);

    @Query("""
    SELECT c FROM Course c
    JOIN SchoolAcademicConfig cfg ON cfg.school = c.school
    LEFT JOIN c.group g
    LEFT JOIN g.teacher t
    LEFT JOIN t.user tu
    WHERE c.school.uuid IN :schoolUuids
    AND c.status = 'ACTIVE'
    AND cfg.defaultCourseLengthInstalled = true
    AND (:teacherUuid IS NULL OR tu.uuid = :teacherUuid)
    AND timestampadd(day,
            CASE WHEN cfg.defaultCourseLengthDays IS NULL OR cfg.defaultCourseLengthDays <= 0 THEN 90 ELSE cfg.defaultCourseLengthDays END,
            COALESCE(c.courseStartDate, c.createdAt)) < :now
    ORDER BY COALESCE(c.courseStartDate, c.createdAt) ASC
""")
    Page<Course> findOverdueCoursesMulti(@Param("schoolUuids") Collection<UUID> schoolUuids,
                                         @Param("teacherUuid") UUID teacherUuid,
                                         @Param("now") Instant now,
                                         Pageable pageable);

    @Query("""
    SELECT c FROM Course c
    JOIN SchoolAcademicConfig cfg ON cfg.school = c.school
    WHERE c.school.uuid IN :schoolUuids
    AND c.status = 'ACTIVE'
    AND cfg.defaultCourseLengthInstalled = true
    AND c.group.uuid = :groupUuid
    AND timestampadd(day,
            CASE WHEN cfg.defaultCourseLengthDays IS NULL OR cfg.defaultCourseLengthDays <= 0 THEN 90 ELSE cfg.defaultCourseLengthDays END,
            COALESCE(c.courseStartDate, c.createdAt)) < :now
    ORDER BY COALESCE(c.courseStartDate, c.createdAt) ASC
""")
    List<Course> findOverdueCoursesByGroup(@Param("schoolUuids") Collection<UUID> schoolUuids,
                                           @Param("groupUuid") UUID groupUuid,
                                           @Param("now") Instant now);
}