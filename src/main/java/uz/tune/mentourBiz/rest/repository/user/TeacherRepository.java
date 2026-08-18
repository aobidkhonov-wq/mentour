package uz.tune.mentourBiz.rest.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository extends BaseRepository<Teacher> {
    @EntityGraph(attributePaths = { "user", "school" })
    Optional<Teacher> findByUser_Uuid(UUID uuid);
    Optional<Teacher> findByUser(User user);

    Page<Teacher> findAllBySchool_UuidAndUserStatus(UUID schoolUuid, UserStatus status, Pageable pageable);
    List<Teacher> findAllByUser_UuidIn(List<UUID> userUuids);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.school.uuid IN :uuids AND t.user.status = 'ACTIVE'")
    long countActiveTeachersMulti(@Param("uuids") Collection<UUID> uuids);

    @Query("SELECT t FROM Teacher t JOIN t.user u " +
            "WHERE t.school.status = 'ACTIVE'" +
            "AND (COALESCE(:schoolUuids, NULL) IS NULL OR t.school.uuid IN :schoolUuids) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (CAST(:fullName AS string) IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:fullName AS string), '%')))")
    Page<Teacher> findWithFilters(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("status") UserStatus status,
            @Param("fullName") String fullName,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.user.status = 'ACTIVE' " +
            "AND t.school.uuid IN :schoolUuids " +
            "AND t.createdAt >= :start AND t.createdAt <= :end")
    long countNewActiveTeachersMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.user.status = 'ACTIVE' " +
            "AND t.school.organization.uuid = :orgUuid " +
            "AND t.createdAt >= :start AND t.createdAt <= :end")
    long countNewActiveTeachersByOrg(@Param("orgUuid") UUID orgUuid, @Param("start") Instant start, @Param("end") Instant end);

    @Modifying
    @Query(value = "UPDATE teacher SET monthly_coin_allowance = ac.teacher_monthly_coin_limit " +
            "FROM school_academic_configs ac WHERE teacher.school_id = ac.school_id", nativeQuery = true)
    void resetAllTeacherAllowances();

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.school.uuid IN :uuids AND t.user.status = 'ACTIVE' AND t.createdAt <= :atTime")
    long countTotalActiveMulti(@Param("uuids") Collection<UUID> uuids, @Param("atTime") Instant atTime);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.school.organization.uuid = :orgUuid AND t.user.status = 'ACTIVE' AND t.createdAt <= :atTime")
    long countTotalActiveByOrg(@Param("orgUuid") UUID orgUuid, @Param("atTime") Instant atTime);

    @Query("""
    SELECT t,
           COUNT(DISTINCT s.id),
           AVG(up.progressPercentage)
    FROM Teacher t
    JOIN t.user u
    JOIN Group g ON g.teacher = t
    JOIN Enrollment e ON e.group = g
    JOIN Student s ON e.student = s
    LEFT JOIN UnitProgress up ON up.student = s
    WHERE t.school.uuid = :schoolUuid
      AND e.status = 'ONGOING'
      AND u.status = 'ACTIVE'
    GROUP BY t.id, u.id
    ORDER BY AVG(up.progressPercentage) DESC
    """)
    List<Object[]> findTopTeachersBySchool(@Param("schoolUuid") UUID schoolUuid, Pageable pageable);

    /**
     * Locks the teacher row for the duration of the transaction. Paying reads the balance, checks the
     * amount against it and writes the payment; two admins paying the same teacher at the same moment
     * would otherwise both pass the check against the same balance and overdraw it between them.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Teacher t WHERE t.user.uuid = :uuid")
    Optional<Teacher> findByUserUuidForUpdate(@Param("uuid") UUID uuid);
}
