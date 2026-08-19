package uz.tune.mentourBiz.rest.repository.student;

import com.google.common.io.Files;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepo extends BaseRepository<Student> {
    @Query("""
    SELECT s FROM Student s 
    JOIN s.user u 
    WHERE s.school.organization.uuid = :orgUuid 
    AND (:activeOnly = false OR u.status = 'ACTIVE')
    AND (u.status = 'ACTIVE' OR s.currentBalance < 0)
    AND EXISTS (SELECT 1 FROM Enrollment e_active WHERE e_active.student = s AND e_active.status IN ('ONGOING', 'DELETED'))
    AND (
        :statusName IS NULL OR 
        (:statusName = 'NEW' AND NOT EXISTS (SELECT ft FROM FinanceTransaction ft WHERE ft.student = s AND ft.type = 'CHARGE' AND ft.deleted = false)) OR
        (:statusName = 'PAID' AND s.currentBalance >= 0 AND EXISTS (SELECT ft FROM FinanceTransaction ft WHERE ft.student = s AND ft.type = 'CHARGE' AND ft.deleted = false)) OR
        (:statusName = 'UNPAID' AND s.currentBalance < 0 AND EXISTS (SELECT ft FROM FinanceTransaction ft WHERE ft.student = s AND ft.type = 'CHARGE' AND ft.deleted = false))
    )
    ORDER BY s.school.name ASC, u.firstName ASC
    """)
    Page<Student> findFinanceDashboardFilteredByOrg(@Param("orgUuid") UUID orgUuid, @Param("statusName") String statusName, @Param("activeOnly") boolean activeOnly, Pageable pageable);

    @Query("""
    SELECT s FROM Student s 
    JOIN s.user u 
    WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR s.school.uuid IN :schoolUuids) 
    AND (:activeOnly = false OR u.status = 'ACTIVE')
    AND (u.status = 'ACTIVE' OR s.currentBalance < 0)
    AND (:teacherUuid IS NULL OR EXISTS (
        SELECT 1 FROM Enrollment e_t 
        WHERE e_t.student = s 
        AND e_t.group.teacher.user.uuid = :teacherUuid 
        AND e_t.status = 'ONGOING'
    ))
    AND (:packageId IS NULL OR EXISTS (
        SELECT 1 FROM Enrollment e_p 
        JOIN Course c_pkg ON c_pkg.group = e_p.group 
        WHERE e_p.student = s 
        AND c_pkg.paymentPackage.uuid = :packageId 
        AND e_p.status = 'ONGOING'
    ))
    AND (:courseUuid IS NULL OR EXISTS (
        SELECT 1 FROM Enrollment e_c
        JOIN Course c_course ON c_course.group = e_c.group
        WHERE e_c.student = s
        AND c_course.uuid = :courseUuid
        AND e_c.status = 'ONGOING'
    ))
    AND (:groupUuid IS NULL OR EXISTS (
        SELECT 1 FROM Enrollment e_g
        WHERE e_g.student = s
        AND e_g.group.uuid = :groupUuid
        AND e_g.status = 'ONGOING'
    ))

    AND (:onlyOverdue = false OR s.currentBalance < 0)
    AND (CAST(:studentName AS string) IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', LOWER(CAST(:studentName AS string)), '%')))
    AND (CAST(:startDate AS timestamp) IS NULL OR s.createdAt >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR s.createdAt <= :endDate) 
    AND (
        :statusName IS NULL OR 
        (:statusName = 'NEW' AND NOT EXISTS (SELECT ft1 FROM FinanceTransaction ft1 WHERE ft1.student = s AND ft1.type = 'CHARGE' AND ft1.deleted = false)) OR
        (:statusName = 'PAID' AND s.currentBalance >= 0 AND EXISTS (SELECT ft2 FROM FinanceTransaction ft2 WHERE ft2.student = s AND ft2.type = 'CHARGE' AND ft2.deleted = false)) OR
        (:statusName = 'UNPAID' AND s.currentBalance < 0 AND EXISTS (SELECT ft3 FROM FinanceTransaction ft3 WHERE ft3.student = s AND ft3.type = 'CHARGE' AND ft3.deleted = false))
    )
    ORDER BY u.firstName ASC, u.lastName ASC
""")
    Page<Student> findFinanceDashboardFiltered(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("courseUuid") UUID courseUuid,
            @Param("studentName") String studentName,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("statusName") String statusName,
            @Param("packageId") UUID packageId,
            @Param("groupUuid") UUID groupUuid,
            @Param("startDate") java.time.Instant startDate,
            @Param("endDate") java.time.Instant endDate,
            @Param("activeOnly") boolean activeOnly,
            @Param("onlyOverdue") boolean onlyOverdue,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.currentBalance), 0) FROM Student s WHERE s.school.uuid = :schoolUuid AND s.currentBalance < 0 AND (:activeOnly = false OR s.user.status = 'ACTIVE')")
    Long sumOutstandingBalanceFiltered(@Param("schoolUuid") UUID schoolUuid, @Param("activeOnly") boolean activeOnly);

    // Count/Find Overdue

    // Count/Find Unassigned

    Optional<Student> getStudentByUuid(UUID uuid);
    Optional<Student> findByUser(User user);
    Optional<Student> findByUserUuid(UUID userUuid);
    List<Student> findAllBySchool_UuidAndUser_Status(UUID schoolUuid, UserStatus status);
    Page<Student> findAllBySchool_Uuid(UUID schoolUuid, Pageable pageable);
    List<Student> findAllByUserUuidIn(List<UUID> uuids);
    List<Student> findAllByUuidIn(List<UUID> studentUuids);

    Optional<Student> findByUuid(UUID studentUuid);

    @Query("SELECT COALESCE(SUM(s.currentBalance),0) FROM Student s WHERE s.school.uuid IN :uuids AND s.currentBalance < 0")
    Long sumTotalOutstandingBalanceIn(@Param("uuids") Collection<UUID> uuids);
    @Query("SELECT COALESCE(COUNT(s),0) FROM Student s WHERE s.school.uuid IN :uuids AND s.user.status = 'ACTIVE'")
    long countActiveStudentsIn(@Param("uuids") List<UUID> uuids);

    @Query("""
    SELECT COALESCE(COUNT(DISTINCT s.id),0) FROM Student s 
    JOIN s.user u 
    JOIN s.enrollments e 
    WHERE s.school.uuid IN :uuids 
    AND u.status = 'ACTIVE' 
    AND e.status = 'ONGOING'
""")
    long countActiveStudentsMulti(@Param("uuids") Collection<UUID> uuids);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.school.uuid IN :uuids AND s.user.status = 'ACTIVE'")
    long countActiveStudentsIn(@Param("uuids") Collection<UUID> uuids);

    @Query("SELECT COALESCE(COUNT(s),0) FROM Student s WHERE s.user.status = 'ACTIVE' " +
            "AND s.school.organization.uuid = :orgUuid " +
            "AND s.createdAt >= :start AND s.createdAt <= :end")
    long countNewActiveStudentsByOrg(@Param("orgUuid") UUID orgUuid, @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
    SELECT s FROM Student s 
    JOIN s.user u 
    WHERE s.school.organization.uuid = :orgUuid 
    AND s.user.status = 'ACTIVE'
    AND EXISTS (SELECT 1 FROM Enrollment e_active WHERE e_active.student = s AND e_active.status = 'ONGOING')
    AND (
        :statusName IS NULL OR 
        (:statusName = 'NEW' AND NOT EXISTS (SELECT ft FROM FinanceTransaction ft WHERE ft.student = s AND ft.type = 'CHARGE' AND ft.deleted = false)) OR
        (:statusName = 'PAID' AND s.currentBalance >= 0 AND EXISTS (SELECT ft FROM FinanceTransaction ft WHERE ft.student = s AND ft.type = 'CHARGE' AND ft.deleted = false)) OR
        (:statusName = 'UNPAID' AND s.currentBalance < 0 AND EXISTS (SELECT ft FROM FinanceTransaction ft WHERE ft.student = s AND ft.type = 'CHARGE' AND ft.deleted = false))
    )
    ORDER BY s.school.name ASC, u.firstName ASC
    """)
    Page<Student> findFinanceDashboardFilteredByOrg(@Param("orgUuid") UUID orgUuid, @Param("statusName") String statusName, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Student s " +
            "WHERE s.school.uuid = :schoolUuid " +
            "AND s.user.status = 'ACTIVE' " +
            "AND s.score > :score")
    Integer countStudentsWithHigherScoreInSchool(@Param("schoolUuid") UUID schoolUuid, @Param("score") Integer score);

    @Modifying
    @Transactional
    @Query("UPDATE Student s SET s.coins = s.coins - :amount WHERE s.uuid = :uuid AND s.coins >= :amount")
    int subtractCoinsAtomic(UUID uuid, Long amount);

    @Modifying
    @Transactional
    @Query("UPDATE Student s SET s.coins = s.coins + :amount, s.lifeTimeCoinBalance = s.lifeTimeCoinBalance + :amount WHERE s.uuid = :uuid")
    int addCoinsAtomic(UUID uuid, Integer amount);

    @Query("SELECT s FROM Student s " +
            "WHERE s.school.uuid = :schoolUuid " +
            "AND NOT EXISTS (SELECT 1 FROM Enrollment e " +
            "                WHERE e.student = s " +
            "                AND e.group.uuid = :groupUuid " +
            "                AND e.status = 'ONGOING') " +
            "ORDER BY s.user.firstName ASC, s.user.lastName ASC")
    List<Student> findAvailableStudentsForGroup(@Param("schoolUuid") UUID schoolUuid, @Param("groupUuid") UUID groupUuid);

    @Query("SELECT s FROM Student s WHERE s.user.status = :status AND s.school.uuid = :schoolUuid")
    List<Student> findAllByUserStatusAndSchool_Uuid(@Param("status") UserStatus status, @Param("schoolUuid") UUID schoolUuid);

    @Query(value = """
        SELECT EXTRACT(MONTH FROM s.created_at) as month, COUNT(s.id) as count 
        FROM students s 
        WHERE s.school_id = (SELECT id FROM schools WHERE uuid = :schoolUuid) 
        AND EXTRACT(YEAR FROM s.created_at) = :year
        GROUP BY month 
        ORDER BY month ASC
        """, nativeQuery = true)
    List<Object[]> countNewStudentsByMonth(@Param("schoolUuid") UUID schoolUuid, @Param("year") int year);

    void deleteAllByUserIdIn(List<Long> userId);
    Page<Student> findAllByUserStatus(UserStatus status, Pageable pageable);
    List<Student> findAllByUser_UuidIn(List<UUID> userUuids);
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.enrollments WHERE s.user IN :users")
    List<Student> findByUserInWithClasses(@Param("users") List<User> users);
    @EntityGraph(attributePaths = { "user", "school" })
    Optional<Student> findByUser_Uuid(UUID userId);
    @Query("SELECT COALESCE(COUNT(s),0) FROM Student s WHERE s.user.status = 'ACTIVE' " +
            "AND s.school.uuid IN :schoolUuids " +
            "AND s.createdAt >= :start AND s.createdAt <= :end")
    long countNewActiveStudentsMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("start") Instant start, @Param("end") Instant end);

    List<Student> findAllByUserStatus(UserStatus status);
    @Query("SELECT s FROM Student s " +
            "JOIN s.user u " +
            "WHERE s.school.status = 'ACTIVE' " +
            "AND (COALESCE(:schoolUuids, NULL) IS NULL OR s.school.uuid IN :schoolUuids) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (CAST(:fullName AS string) IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', LOWER(CAST(:fullName AS string)), '%'))) " +
            "AND (CAST(:classId AS uuid) IS NULL OR EXISTS ( " +
            "    SELECT 1 FROM Enrollment e " +
            "    WHERE e.student = s AND e.group.uuid = :classId AND e.status = 'ONGOING')) " +
            "AND (CAST(:courseId AS uuid) IS NULL OR EXISTS ( " +
            "    SELECT 1 FROM Course c JOIN Enrollment e2 ON e2.group = c.group " +
            "    WHERE e2.student = s AND c.uuid = :courseId AND e2.status = 'ONGOING')) " +
            "AND (CAST(:teacherUuid AS uuid) IS NULL OR EXISTS ( " +
            "    SELECT 1 FROM Enrollment e3 " +
            "    WHERE e3.student = s AND e3.group.teacher.user.uuid = :teacherUuid AND e3.status = 'ONGOING'))" +
            "ORDER BY u.firstName ASC, u.lastName ASC")
    Page<Student> findWithFilters(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("status") UserStatus status,
            @Param("fullName") String fullName,
            @Param("classId") UUID classId,
            @Param("courseId") UUID courseId,
            Pageable pageable);

    @Query("SELECT s FROM Student s JOIN s.enrollments e " +
            "WHERE e.group.uuid = :groupId AND s.user.status = 'REFERRED' AND s.school.uuid IN :authorizedUuids")
    List<Student> findReferredByGroupAndOrg(@Param("groupId") UUID groupId, @Param("authorizedUuids") Collection<UUID> authorizedUuids);

    @Query("""
    SELECT COALESCE(SUM(s.currentBalance), 0) FROM Student s 
    WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR s.school.uuid IN :schoolUuids) 
    AND s.currentBalance < 0 
    AND (:groupUuid IS NULL OR EXISTS (SELECT 1 FROM Enrollment e WHERE e.student = s AND e.group.uuid = :groupUuid AND e.status = 'ONGOING'))
    AND (:courseUuid IS NULL OR EXISTS (SELECT 1 FROM Enrollment ec JOIN Course c ON c.group = ec.group WHERE ec.student = s AND c.uuid = :courseUuid AND ec.status = 'ONGOING'))
    AND (:teacherUuid IS NULL OR EXISTS (SELECT 1 FROM Enrollment et WHERE et.student = s AND et.group.teacher.user.uuid = :teacherUuid AND et.status = 'ONGOING'))
    AND (:activeOnly = false OR s.user.status = 'ACTIVE')
""")
    Long sumOutstandingBalanceFiltered(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("groupUuid") UUID groupUuid,
            @Param("courseUuid") UUID courseUuid,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("activeOnly") boolean activeOnly);

    @Query("SELECT COALESCE(COUNT(s),0) FROM Student s WHERE s.school.uuid IN :schoolUuids AND s.currentBalance < 0 AND s.user.status = 'ACTIVE'")
    long countOverdueMulti(@Param("schoolUuids") Collection<UUID> schoolUuids);

    @Query("""
    SELECT COALESCE(COUNT(DISTINCT s), 0) FROM Student s
    JOIN s.enrollments e
    WHERE e.group.uuid = :groupUuid AND e.status = 'ONGOING'
    AND s.user.status = 'ACTIVE' AND s.currentBalance < 0
""")
    long countOverdueByGroup(@Param("groupUuid") UUID groupUuid);

    @Query("""
    SELECT COALESCE(SUM(s.currentBalance), 0) FROM Student s
    WHERE s.currentBalance < 0 AND s.user.status = 'ACTIVE'
    AND EXISTS (SELECT 1 FROM Enrollment e WHERE e.student = s AND e.group.uuid = :groupUuid AND e.status = 'ONGOING')
""")
    long sumOverdueByGroup(@Param("groupUuid") UUID groupUuid);

    @Query("""
    SELECT COALESCE(SUM(s.currentBalance), 0) FROM Student s
    WHERE s.user.status = 'ACTIVE'
    AND EXISTS (SELECT 1 FROM Enrollment e WHERE e.student = s AND e.group.uuid = :groupUuid AND e.status = 'ONGOING')
""")
    long sumBalanceByGroup(@Param("groupUuid") UUID groupUuid);

    @Query("SELECT s FROM Student s WHERE s.school.uuid IN :schoolUuids AND s.currentBalance < 0 AND s.user.status = 'ACTIVE'")
    Page<Student> findOverdueMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, Pageable pageable);

    @Query("SELECT COALESCE(COUNT(s),0) FROM Student s WHERE s.school.uuid IN :schoolUuids AND s.user.status = 'ACTIVE' AND NOT EXISTS (SELECT e FROM Enrollment e WHERE e.student = s AND e.status = 'ONGOING')")
    long countUnassignedMulti(@Param("schoolUuids") Collection<UUID> schoolUuids);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.school.uuid IN :ids AND s.user.status = 'ACTIVE' AND s.parentContacts IS NOT EMPTY")
    long countStudentsWithParentsIn(@Param("ids") Collection<UUID> ids);


    @Query("SELECT COUNT(s) FROM Student s WHERE s.school.uuid IN :uuids AND s.user.status = 'ACTIVE' AND s.createdAt <= :atTime")
    long countTotalActiveMulti(@Param("uuids") Collection<UUID> uuids, @Param("atTime") Instant atTime);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.school.organization.uuid = :orgUuid AND s.user.status = 'ACTIVE' AND s.createdAt <= :atTime")
    long countTotalActiveByOrg(@Param("orgUuid") UUID orgUuid, @Param("atTime") Instant atTime);

    @Query("SELECT s FROM Student s WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR s.school.uuid IN :schoolUuids) AND s.user.status = 'ACTIVE' AND NOT EXISTS (SELECT e FROM Enrollment e WHERE e.student = s AND e.status = 'ONGOING')")
    Page<Student> findUnassignedMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, Pageable pageable);

    @Query("""
    SELECT COUNT(s) FROM Student s 
    WHERE s.school.uuid IN :uuids 
    AND s.user.status = 'BLOCKED' 
    AND s.user.updatedAt >= :since
    """)
    long countLeftThisMonth(@Param("uuids") java.util.Collection<java.util.UUID> uuids, @Param("since") java.time.Instant since);

    // ---- Teacher-scoped dashboard counts (students enrolled in the teacher's own groups) ----

    @Query("""
    SELECT COUNT(DISTINCT s) FROM Student s
    JOIN s.enrollments e
    WHERE e.status = 'ONGOING' AND e.group.teacher.user.uuid = :teacherUuid
    AND s.user.status = 'ACTIVE'
    """)
    long countActiveStudentsByTeacher(@Param("teacherUuid") UUID teacherUuid);

    @Query("""
    SELECT COUNT(DISTINCT s) FROM Student s
    JOIN s.enrollments e
    WHERE e.status = 'ONGOING' AND e.group.teacher.user.uuid = :teacherUuid
    AND s.user.status = 'ACTIVE'
    AND s.createdAt >= :start AND s.createdAt <= :end
    """)
    long countNewActiveStudentsByTeacher(@Param("teacherUuid") UUID teacherUuid,
                                         @Param("start") Instant start,
                                         @Param("end") Instant end);

    @Query("""
    SELECT COUNT(DISTINCT s) FROM Student s
    JOIN s.enrollments e
    WHERE e.status = 'ONGOING' AND e.group.teacher.user.uuid = :teacherUuid
    AND s.user.status = 'ACTIVE' AND s.parentContacts IS NOT EMPTY
    """)
    long countStudentsWithParentsByTeacher(@Param("teacherUuid") UUID teacherUuid);

    @Query("""
    SELECT COUNT(DISTINCT s) FROM Student s
    JOIN s.enrollments e
    WHERE e.group.teacher.user.uuid = :teacherUuid
    AND s.user.status = 'BLOCKED' AND s.user.updatedAt >= :since
    """)
    long countLeftThisMonthByTeacher(@Param("teacherUuid") UUID teacherUuid, @Param("since") Instant since);

    Page<Student> findAllBySchool_UuidInAndUser_Status(Collection<UUID> uuids, UserStatus status, Pageable pageable);

    Page<Student> findAllBySchool_UuidInAndCreatedAtAfter(Collection<UUID> uuids, java.time.Instant since, Pageable pageable);

    @Query("""
    SELECT s FROM Student s 
    WHERE s.school.uuid IN :uuids 
    AND s.user.status = 'BLOCKED' 
    AND s.user.updatedAt >= :since
    """)
    Page<Student> findLeftMulti(@Param("uuids") Collection<UUID> uuids, @Param("since") java.time.Instant since, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.school.uuid IN :uuids AND s.user.status = 'ACTIVE' AND s.parentContacts IS NOT EMPTY")
    Page<Student> findWithParentsMulti(@Param("uuids") Collection<UUID> uuids, Pageable pageable);

    @Query("""
    SELECT DISTINCT s FROM Student s 
    JOIN s.user u 
    JOIN s.enrollments e 
    WHERE s.school.uuid IN :uuids 
    AND u.status = 'ACTIVE' 
    AND e.status = 'ONGOING'
    """)
    Page<Student> findActiveMulti(@Param("uuids") java.util.Collection<java.util.UUID> uuids, Pageable pageable);

    @Query("""
    SELECT COALESCE(COUNT(DISTINCT s), 0) FROM Student s
    JOIN s.enrollments e
    WHERE s.school.uuid IN :schoolUuids
    AND s.isAtRisk = true
    AND s.user.status = 'ACTIVE'
    AND e.status = 'ONGOING'
    AND (:teacherUuid IS NULL OR e.group.teacher.user.uuid = :teacherUuid)
    """)
    long countAtRiskMulti(@Param("schoolUuids") Collection<UUID> schoolUuids,
                          @Param("teacherUuid") UUID teacherUuid);

    @Query("""
    SELECT DISTINCT s FROM Student s
    JOIN s.enrollments e
    WHERE s.school.uuid IN :schoolUuids
    AND s.isAtRisk = true
    AND s.user.status = 'ACTIVE'
    AND e.status = 'ONGOING'
    AND (:teacherUuid IS NULL OR e.group.teacher.user.uuid = :teacherUuid)
    """)
    Page<Student> findAtRiskMulti(@Param("schoolUuids") Collection<UUID> schoolUuids,
                                  @Param("teacherUuid") UUID teacherUuid,
                                  Pageable pageable);

    long countBySchool(School school);

    int countBySchool_UuidIn(Collection<UUID> schoolUuid);

    // ---- Ranking (distinct students; a student may have several ONGOING enrollments in the same branch/school) ----

    @EntityGraph(attributePaths = {"user", "user.attachment"})
    @Query(value = """
        SELECT DISTINCT s FROM Student s
        JOIN s.enrollments e
        WHERE e.group.branch.uuid = :branchUuid
          AND e.status = 'ONGOING'
        """,
        countQuery = """
        SELECT COUNT(DISTINCT s) FROM Student s
        JOIN s.enrollments e
        WHERE e.group.branch.uuid = :branchUuid
          AND e.status = 'ONGOING'
        """)
    Page<Student> findDistinctRankingByBranch(@Param("branchUuid") UUID branchUuid, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.attachment"})
    @Query(value = """
        SELECT DISTINCT s FROM Student s
        JOIN s.enrollments e
        WHERE e.group.branch.school.uuid = :schoolUuid
          AND e.status = 'ONGOING'
        """,
        countQuery = """
        SELECT COUNT(DISTINCT s) FROM Student s
        JOIN s.enrollments e
        WHERE e.group.branch.school.uuid = :schoolUuid
          AND e.status = 'ONGOING'
        """)
    Page<Student> findDistinctRankingBySchool(@Param("schoolUuid") UUID schoolUuid, Pageable pageable);
}
