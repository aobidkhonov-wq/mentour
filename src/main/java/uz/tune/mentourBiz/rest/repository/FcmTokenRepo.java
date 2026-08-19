package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.FcmToken;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FcmTokenRepo extends BaseRepository<FcmToken> {

    Optional<FcmToken> findByToken(String token);

    // 1. BROADCAST_ALL
    @Query("SELECT t.token FROM FcmToken t WHERE t.user.status = 'ACTIVE'")
    List<String> findAllActiveTokens();

    @Query("""
        SELECT t.token FROM FcmToken t 
        LEFT JOIN Student s ON s.user = t.user
        LEFT JOIN Teacher teach ON teach.user = t.user
        WHERE s.school.organization.uuid = :orgUuid 
           OR teach.school.organization.uuid = :orgUuid
    """)
    List<String> findTokensByOrganization(@Param("orgUuid") UUID orgUuid);

    // 2. SCHOOL_WIDE
    @Query("""
        SELECT t.token FROM FcmToken t 
        LEFT JOIN Student s ON s.user = t.user
        LEFT JOIN Teacher teach ON teach.user = t.user
        LEFT JOIN SchoolAdmin sa ON sa.user = t.user
        WHERE s.school.uuid = :schoolUuid 
           OR teach.school.uuid = :schoolUuid 
           OR sa.school.uuid = :schoolUuid
    """)
    List<String> findTokensBySchool(@Param("schoolUuid") UUID schoolUuid);

    @Query("SELECT t.token FROM FcmToken t WHERE t.user.uuid IN :userUuids")
    List<String> findTokensByUserUuids(@Param("userUuids") List<UUID> userUuids);

    @Query("SELECT t FROM FcmToken t WHERE t.user.uuid IN :userUuids")
    List<FcmToken> findFcmTokensByUserUuids(@Param("userUuids") List<UUID> userUuids);

    // 3. GROUP_WIDE
    @Query("""
        SELECT t.token FROM FcmToken t
        WHERE t.user.id IN (
            SELECT e.student.user.id FROM Enrollment e WHERE e.group.uuid = :groupUuid AND e.status = 'ONGOING'
        ) OR t.user.id IN (
            SELECT g.teacher.user.id FROM Group g WHERE g.uuid = :groupUuid
        )
    """)
    List<String> findTokensByGroup(@Param("groupUuid") UUID groupUuid);

    // 4. INDIVIDUAL_USER

    @Modifying
    @Transactional
    void deleteByToken(String token);
}
