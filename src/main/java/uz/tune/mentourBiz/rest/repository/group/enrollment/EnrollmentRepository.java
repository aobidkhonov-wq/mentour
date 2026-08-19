package uz.tune.mentourBiz.rest.repository.group.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.GroupStatus;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.util.*;

@Repository
public interface EnrollmentRepository extends BaseRepository<Enrollment> {

    long countByGroup_UuidAndStatusAndStudent_User_Status(UUID groupUuid, EnrollmentStatus status, UserStatus userStatus);

    /**
     * Whether the student is (or has been) in this group at all. Used to reject a payment earmarked for
     * a group the student never belonged to, which would otherwise credit that group's teacher.
     */
    boolean existsByStudent_UuidAndGroup_Uuid(UUID studentUuid, UUID groupUuid);

    Page<Enrollment> findAllByGroup_Branch_School_UuidIn(Collection<UUID> schoolUuids, Pageable pageable);

    Page<Enrollment> findAllByGroup_UuidAndGroup_Branch_School_UuidIn(UUID groupId, Collection<UUID> schoolUuids, Pageable pageable);

    Page<Enrollment> findAllByStudent_User_UuidAndGroup_Branch_School_UuidIn(UUID studentId, Collection<UUID> schoolUuids, Pageable pageable);

    //ranking
    @EntityGraph(attributePaths = {"student.user", "student.user.attachment"})
    Page<Enrollment> findAllByGroup_UuidAndStatus(UUID groupId, EnrollmentStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"student.user", "student.user.attachment"})
    @Query("SELECT e FROM Enrollment e WHERE e.student.user.status = 'ACTIVE' AND e.group.uuid = :groupId AND e.status = :status AND e.student.user.status = :userStatus ORDER BY e.student.user.firstName ASC, e.student.user.lastName ASC")
    List<Enrollment> findAllByGroup_UuidAndStatusAndStudent_User_Status(
            @Param("groupId") UUID groupId,
            @Param("status") EnrollmentStatus status,
            @Param("userStatus") UserStatus userStatus);

    @EntityGraph(attributePaths = {"student.user", "student.user.attachment"})
    Page<Enrollment> findAllByGroup_Branch_UuidAndStatus(UUID branchUuid, EnrollmentStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"student.user", "student.user.attachment"})
    Page<Enrollment> findAllByGroup_Branch_School_UuidAndStatus(UUID schoolId, EnrollmentStatus status, Pageable pageable);
    @Query("SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e WHERE e.group.teacher.user.uuid = :teacherUuid AND e.status = 'ONGOING'")
    long countTotalStudentsByTeacher(@Param("teacherUuid") UUID teacherUuid);
    long countByGroup_UuidAndStatusAndStudent_UserStatus(UUID groupUuid, EnrollmentStatus status, UserStatus userStatus);

    List<Enrollment>  findAllByGroup_GroupStatusAndStatus(GroupStatus groupStatus, EnrollmentStatus enrollmentStatus);

    List<Enrollment> findAllByStudent_User_UuidAndStatus(UUID userUuid, EnrollmentStatus status);

    @Query("""
    SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e 
    WHERE e.group.branch.school.uuid IN :ids 
    AND e.status = 'ONGOING' 
    AND e.student.user.status = 'ACTIVE'
""")
    long countActiveStudentsMulti(@Param("ids") Collection<UUID> ids);

    @Query("""
    SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e 
    WHERE e.group.branch.school.uuid IN :ids 
    AND e.status IN ('FINISHED', 'DELETED') 
    AND e.updatedAt >= :since
""")
    long countLeftThisMonth(@Param("ids") Collection<UUID> ids, @Param("since") java.time.Instant since);

    @EntityGraph(attributePaths = {"student.user", "student.user.attachment"})
    @Query("SELECT e FROM Enrollment e WHERE e.group.uuid = :groupId AND e.status = :status ORDER BY e.student.user.firstName ASC, e.student.user.lastName ASC")
    List<Enrollment> findAllByGroup_UuidAndStatus(
            @Param("groupId") UUID groupId,
            @Param("status") EnrollmentStatus status);

    List<Enrollment> findAllByStudent_User_UuidInAndStatusIn(List<UUID> userUuids, List<EnrollmentStatus> statuses);

    @Query("SELECT e.student.uuid FROM Enrollment e WHERE e.group.uuid = :groupUuid AND e.status =:status AND e.student.uuid IN :studentUuids")
    Set<UUID> findExistingStudentUuidsInGroupAndStatus(@Param("groupUuid") UUID groupUuid, @Param("studentUuids") List<UUID> studentUuids, EnrollmentStatus status);

    @Query("""
    SELECT COUNT(DISTINCT e.student.id) 
    FROM Enrollment e 
    JOIN Course c ON c.group = e.group 
    WHERE c.paymentPackage = :pkg 
    AND c.status = 'ACTIVE' 
    AND e.status = 'ONGOING' 
    AND e.student.user.status = 'ACTIVE'
    """)
    long countActiveStudentsByPaymentPackage(@Param("pkg") PaymentPackage pkg);

    @Query("SELECT e FROM Enrollment e " +
            "JOIN e.student s " +
            "JOIN s.user u " +
            "WHERE e.group.uuid = :groupUuid AND u.status = 'ACTIVE' AND e.status = 'ONGOING' " +
            "AND (CAST(:fullName AS string) IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:fullName AS string), '%'))) " +
            "ORDER BY u.firstName ASC, u.lastName ASC")
    List<Enrollment> findByGroupAndNameList(UUID groupUuid, String fullName);

    boolean existsByStudentAndGroup_Teacher_User_Uuid(Student student, UUID teacherUserUuid);

    Optional<Enrollment> findByUuid(UUID uuid);

    boolean existsByStudentAndGroup(Student student, Group group);

    Page<Enrollment> findAllByStudent_User_Uuid(UUID studentId, Pageable pageable);
    Enrollment findByStudent_UuidAndStatus(UUID studentId, EnrollmentStatus enrollmentStatus);

    Optional<Enrollment> findTopByStudent_User_UuidAndStatus(UUID studentUuid, EnrollmentStatus status);
    Page<Enrollment> findAllByGroup_Uuid(UUID groupId, Pageable pageable);
    Page<Enrollment> findAllByGroup_UuidAndStatus(UUID groupId, Pageable pageable, EnrollmentStatus status);

    /**
     * One row per student: a student who left and rejoined the group has several enrollment rows,
     * and listing all of them makes the same person appear repeatedly — usually with a stale
     * FINISHED row on top of the real ONGOING one. The correlated subquery keeps the ONGOING row
     * when there is one and the most recent row otherwise (within the requested status filter).
     */
    @Query(value = "SELECT e FROM Enrollment e " +
            "JOIN e.student s " +
            "JOIN s.user u " +
            "LEFT JOIN e.billingPlan bp " +
            "WHERE e.group.uuid = :groupUuid " +
            "AND (COALESCE(:schoolUuids, NULL) IS NULL OR e.group.branch.school.uuid IN :schoolUuids) " +
            "AND (:status IS NULL OR e.status = :status) " +
            "AND (:onlyOverdue = false OR s.currentBalance < 0) " +
            "AND (:billingPlanUuid IS NULL OR bp.uuid = :billingPlanUuid) " +
            "AND (CAST(:studentName AS string) IS NULL OR " +
            "     LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:studentName AS string), '%'))) " +
            "AND e.id = (SELECT MAX(e2.id) FROM Enrollment e2 " +
            "            WHERE e2.student = e.student AND e2.group = e.group " +
            "            AND (:status IS NULL OR e2.status = :status) " +
            "            AND (e2.status = 'ONGOING' OR NOT EXISTS (" +
            "                 SELECT e3.id FROM Enrollment e3 " +
            "                 WHERE e3.student = e.student AND e3.group = e.group " +
            "                 AND e3.status = 'ONGOING' " +
            "                 AND (:status IS NULL OR e3.status = :status))))",
            countQuery = "SELECT COUNT(e) FROM Enrollment e " +
            "JOIN e.student s " +
            "JOIN s.user u " +
            "LEFT JOIN e.billingPlan bp " +
            "WHERE e.group.uuid = :groupUuid " +
            "AND (COALESCE(:schoolUuids, NULL) IS NULL OR e.group.branch.school.uuid IN :schoolUuids) " +
            "AND (:status IS NULL OR e.status = :status) " +
            "AND (:onlyOverdue = false OR s.currentBalance < 0) " +
            "AND (:billingPlanUuid IS NULL OR bp.uuid = :billingPlanUuid) " +
            "AND (CAST(:studentName AS string) IS NULL OR " +
            "     LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:studentName AS string), '%'))) " +
            "AND e.id = (SELECT MAX(e2.id) FROM Enrollment e2 " +
            "            WHERE e2.student = e.student AND e2.group = e.group " +
            "            AND (:status IS NULL OR e2.status = :status) " +
            "            AND (e2.status = 'ONGOING' OR NOT EXISTS (" +
            "                 SELECT e3.id FROM Enrollment e3 " +
            "                 WHERE e3.student = e.student AND e3.group = e.group " +
            "                 AND e3.status = 'ONGOING' " +
            "                 AND (:status IS NULL OR e3.status = :status))))")
    Page<Enrollment> findGroupStudentsFiltered(
            @Param("groupUuid") UUID groupUuid,
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("status") EnrollmentStatus status,
            @Param("onlyOverdue") boolean onlyOverdue,
            @Param("billingPlanUuid") UUID billingPlanUuid,
            @Param("studentName") String studentName,
            Pageable pageable);

    Optional<Enrollment> findByStudentAndGroupAndStatus(Student student, Group group, EnrollmentStatus status);

    Optional<Enrollment> findByStudentAndGroupAndStatusAndBillingPlanNotNull(Student student, Group group, EnrollmentStatus status);


    List<Enrollment> findAllByGroup_UuidAndStatusAndBillingType(
            UUID groupUuid, EnrollmentStatus status, BillingPlanType billingType);

    List<Enrollment> findAllByStatusAndBillingTypeAndPaidUntilBefore(
            EnrollmentStatus status, BillingPlanType billingType, java.time.Instant cutoff);
}