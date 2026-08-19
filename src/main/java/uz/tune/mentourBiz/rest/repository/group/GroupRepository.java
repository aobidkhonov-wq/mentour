package uz.tune.mentourBiz.rest.repository.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.enums.GroupStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends BaseRepository<Group> {
    Optional<Group> findByUuid(UUID uuid);

    List<Group> findAllByBranch_School_Uuid(UUID schoolUuid);

    Optional<Group> findByReferralCode(UUID referralCode);
    List<Group> findAllByUuidIn(List<UUID> ids);
    List<Group> findAllByTeacher_User_Uuid(UUID teacherUuid);

    boolean existsByUuid(UUID uuid);
    long countByTeacher_User_UuidAndGroupStatus(UUID teacherUuid, GroupStatus status);

    @Query("SELECT COUNT(g) FROM Group g WHERE g.branch.school.uuid IN :uuids AND g.groupStatus = 'ACTIVE'")
    long countActiveGroupsIn(@Param("uuids") List<UUID> uuids);

    @Query("SELECT COUNT(g) FROM Group g WHERE g.groupStatus = 'ACTIVE' " +
            "AND g.branch.school.organization.uuid = :orgUuid " +
            "AND g.createdAt >= :start AND g.createdAt <= :end")
    long countNewActiveGroupsByOrg(@Param("orgUuid") UUID orgUuid, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(g) FROM Group g WHERE g.groupStatus = 'ACTIVE' " +
            "AND g.branch.school.uuid IN :schoolUuids " +
            "AND g.createdAt >= :start AND g.createdAt <= :end")
    long countNewActiveGroupsMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("start") Instant start, @Param("end") Instant end);

    Page<Group> findAllByBranch_School_UuidInAndGroupStatus(Collection<UUID> schoolUuids, Pageable pageable, GroupStatus status);

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN g.teacher t " +
            "LEFT JOIN t.user u " +
            "WHERE g.branch.school.uuid IN :schoolUuids AND g.groupStatus = 'ACTIVE' " +
            "AND (:groupName IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:groupName AS string), '%'))) " +
            "AND (:teacherName IS NULL OR " +
            "     LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:teacherName AS string), '%')) OR " +
            "     LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:teacherName AS string), '%'))) " +
            "ORDER BY g.name ASC")
    List<Group> findActiveBySchoolWithFilters(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("groupName") String groupName,
            @Param("teacherName") String teacherName);

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN g.level l " +
            "LEFT JOIN g.teacher t " +
            "LEFT JOIN t.user u " +
            "WHERE g.branch.school.status = 'ACTIVE' " +
            "AND (:status IS NULL OR g.groupStatus = :status) " +
            "AND t.school.status = 'ACTIVE'" +
            "AND (COALESCE(:schoolUuids, NULL) IS NULL OR g.branch.school.uuid IN :schoolUuids) " +
            "AND (:branchId IS NULL OR g.branch.uuid = :branchId) " +
            "AND (:teacherUuid IS NULL OR u.uuid = :teacherUuid) " +
            "AND (:className IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:className AS string), '%'))) " +
            "AND (:levelName IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:levelName AS string), '%'))) " +
            "AND (:teacherName IS NULL OR " +
            "     LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:teacherName AS string), '%')) OR " +
            "     LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:teacherName AS string), '%')))")
    Page<Group> findWithFilters(
            @Param("status") GroupStatus status,
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("branchId") UUID branchId,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("className") String className,
            @Param("levelName") String levelName,
            @Param("teacherName") String teacherName,
            Pageable pageable);

    // 1. Total student count for all schools in the Org
    @Query("SELECT COUNT(DISTINCT s.id) FROM Student s WHERE s.school.uuid IN :uuids AND s.user.status = 'ACTIVE'")
    long countActiveStudentsMulti(@Param("uuids") Collection<UUID> uuids);

    // 2. Total active classes count for all schools in the Org
    @Query("SELECT COUNT(g) FROM Group g WHERE g.branch.school.uuid IN :uuids AND g.groupStatus = 'ACTIVE'")
    long countActiveGroupsMulti(@Param("uuids") Collection<UUID> uuids);

    // 3. Total teacher count for all schools in the Org
    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.school.uuid IN :uuids AND t.user.status = 'ACTIVE'")
    long countActiveTeachersMulti(@Param("uuids") Collection<UUID> uuids);

    @Query("SELECT COUNT(g) FROM Group g WHERE g.branch.school.uuid IN :uuids AND g.groupStatus = 'ACTIVE' AND g.createdAt <= :atTime")
    long countTotalActiveMulti(@Param("uuids") Collection<UUID> uuids, @Param("atTime") Instant atTime);

    @Query("SELECT COUNT(g) FROM Group g WHERE g.branch.school.organization.uuid = :orgUuid AND g.groupStatus = 'ACTIVE' AND g.createdAt <= :atTime")
    long countTotalActiveByOrg(@Param("orgUuid") UUID orgUuid, @Param("atTime") Instant atTime);

    @Query("""
    SELECT COUNT(g) FROM Group g 
    WHERE g.branch.school.uuid IN :schoolUuids AND g.groupStatus = 'ACTIVE' 
    AND (:teacherUuid IS NULL OR g.teacher.user.uuid = :teacherUuid)
    AND NOT EXISTS (SELECT gs FROM GroupSchedule gs WHERE gs.group = g AND gs.dueDate > :now AND gs.status = 'ACTIVE')
""")
    long countGroupsWithoutHwMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("teacherUuid") UUID teacherUuid, @Param("now") Instant now);

    @Query("""
    SELECT g FROM Group g 
    WHERE g.branch.school.uuid IN :schoolUuids AND g.groupStatus = 'ACTIVE' 
    AND (:teacherUuid IS NULL OR g.teacher.user.uuid = :teacherUuid)
    AND NOT EXISTS (SELECT gs FROM GroupSchedule gs WHERE gs.group = g AND gs.dueDate > :now AND gs.status = 'ACTIVE')
""")
    Page<Group> findGroupsWithoutHwMulti(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("teacherUuid") UUID teacherUuid, @Param("now") Instant now, Pageable pageable);
}
