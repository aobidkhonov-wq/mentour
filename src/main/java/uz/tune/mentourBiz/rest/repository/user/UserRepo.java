package uz.tune.mentourBiz.rest.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface UserRepo extends BaseRepository<User> {
    Optional<User> findByUsernameAndStatus(String username, UserStatus userStatus);

    @Query("""
        SELECT u FROM User u
        WHERE u.username = :username AND u.status = 'ACTIVE'
        AND (
            EXISTS (SELECT 1 FROM Student s WHERE s.user = u AND (s.school.uuid = :schoolUuid or s.school.organization.uuid = :schoolUuid )) OR
            EXISTS (SELECT 1 FROM Teacher t WHERE t.user = u AND t.school.uuid = :schoolUuid) OR
            EXISTS (SELECT 1 FROM SchoolAdmin sa WHERE sa.user = u AND sa.school.uuid = :schoolUuid) OR
            EXISTS (SELECT 1 FROM Moderator m WHERE m.user = u AND m.school.uuid = :schoolUuid)
        )
    """)
    Optional<User> findByUsernameAndSchoolUuid(@Param("username") String username, @Param("schoolUuid") UUID schoolUuid);

    @Query("""
        SELECT COUNT(u) > 0 FROM User u
        WHERE u.username = :username AND u.status != 'BLOCKED'
        AND (
            EXISTS (SELECT 1 FROM Student s WHERE s.user = u AND s.school.uuid = :schoolUuid) OR
            EXISTS (SELECT 1 FROM Teacher t WHERE t.user = u AND t.school.uuid = :schoolUuid) OR
            EXISTS (SELECT 1 FROM SchoolAdmin sa WHERE sa.user = u AND sa.school.uuid = :schoolUuid) OR
            EXISTS (SELECT 1 FROM Moderator m WHERE m.user = u AND m.school.uuid = :schoolUuid)
        )
    """)
    boolean existsByUsernameAndSchoolUuid(@Param("username") String username, @Param("schoolUuid") UUID schoolUuid);

    @Query("""
        SELECT DISTINCT u.telegramChatId FROM User u
        LEFT JOIN Student s ON s.user = u
        LEFT JOIN Teacher t ON t.user = u
        LEFT JOIN SchoolAdmin sa ON sa.user = u
        LEFT JOIN Moderator m ON m.user = u
        LEFT JOIN SchoolMentor sm ON sm.mentor.user = u
        WHERE u.telegramChatId IS NOT NULL 
        AND (
               s.school.organization.uuid = :orgUuid 
            OR t.school.organization.uuid = :orgUuid
            OR sa.school.organization.uuid = :orgUuid
            OR m.school.organization.uuid = :orgUuid
            OR sm.school.organization.uuid = :orgUuid
        )
    """)
    List<String> findChatIdsByOrganization(@Param("orgUuid") UUID orgUuid);

    Optional<User> findByUuid(UUID uuid);
    List<User> findAllByUuidIn(Collection<UUID> uuids);
    boolean existsByUsername(String username);

    Page<User> findAllByStatus(UserStatus status, Pageable pageable);
    List<User> findAllByStatusAndUpdatedAtBefore(UserStatus status, java.time.Instant timestamp);

    @Query("SELECT u.telegramChatId FROM User u WHERE u.telegramChatId IS NOT NULL AND u.status = 'ACTIVE'")
    List<String> findAllActiveTelegramChatIds();

    @Query("""
        SELECT u.telegramChatId FROM User u
        WHERE u.uuid = :uuid AND u.telegramChatId IS NOT NULL
    """)
    List<String> findChatIdByUserUuid(UUID uuid);

    @Query("""
        SELECT DISTINCT u.telegramChatId FROM User u
        LEFT JOIN Student s ON s.user = u
        LEFT JOIN Teacher t ON t.user = u
        LEFT JOIN SchoolAdmin sa ON sa.user = u
        LEFT JOIN Moderator m ON m.user = u
        WHERE u.telegramChatId IS NOT NULL 
        AND (s.school.uuid = :schoolUuid 
             OR t.school.uuid = :schoolUuid 
             OR sa.school.uuid = :schoolUuid 
             OR m.school.uuid = :schoolUuid)
    """)
    List<String> findChatIdsBySchool(@Param("schoolUuid") UUID schoolUuid);

    @Query("""
        SELECT DISTINCT u.telegramChatId FROM User u
        WHERE u.telegramChatId IS NOT NULL AND (
            u.id IN (SELECT e.student.user.id FROM Enrollment e WHERE e.group.uuid = :groupUuid AND e.status = 'ONGOING')
            OR 
            u.id IN (SELECT g.teacher.user.id FROM Group g WHERE g.uuid = :groupUuid)
        )
    """)
    List<String> findChatIdsByGroup(@Param("groupUuid") UUID groupUuid);

    @Query("""
                SELECT u FROM User u 
                JOIN SchoolDirector sd ON sd.user = u 
                JOIN sd.organization o 
                JOIN o.schools s 
                WHERE u.username = :username 
                AND (s.uuid = :schoolUuid or o.uuid = :schoolUuid) 
                AND u.status = 'ACTIVE'
            """)
    Optional<User> findDirectorByUsernameAndSchool(@Param("username") String username, @Param("schoolUuid") UUID schoolUuid);

    @Query("""
    SELECT DISTINCT u FROM User u
    LEFT JOIN SchoolAdmin sa ON sa.user = u
    LEFT JOIN sa.school saS
    LEFT JOIN Moderator m ON m.user = u
    LEFT JOIN m.school mS
    LEFT JOIN Student s ON s.user = u
    LEFT JOIN s.school sS
    LEFT JOIN Teacher t ON t.user = u
    LEFT JOIN t.school tS
    WHERE (:status IS NULL OR u.status = :status)
    AND (:role IS NULL OR u.role = :role)
    AND (CAST(:username AS string) IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:username AS string), '%')))
    AND (CAST(:fullName AS string) IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:fullName AS string), '%')))
    AND (CAST(:schoolName AS string) IS NULL OR
        LOWER(saS.name) LIKE LOWER(CONCAT('%', CAST(:schoolName AS string), '%')) OR
        LOWER(mS.name) LIKE LOWER(CONCAT('%', CAST(:schoolName AS string), '%')) OR
        LOWER(sS.name) LIKE LOWER(CONCAT('%', CAST(:schoolName AS string), '%')) OR
        LOWER(tS.name) LIKE LOWER(CONCAT('%', CAST(:schoolName AS string), '%')) 
    )
    AND (COALESCE(:scopeSchoolUuids, NULL) IS NULL OR 
        saS.uuid IN :scopeSchoolUuids OR mS.uuid IN :scopeSchoolUuids OR 
        sS.uuid IN :scopeSchoolUuids OR tS.uuid IN :scopeSchoolUuids
    )
    AND (CAST(:classId AS uuid) IS NULL OR EXISTS (
        SELECT 1 FROM Enrollment e
        WHERE e.student.user = u
        AND e.group.uuid = :classId
        AND e.status = 'ONGOING'
    ))
    AND (CAST(:courseId AS uuid) IS NULL OR EXISTS (
        SELECT 1 FROM Course cr JOIN cr.group g JOIN Enrollment e2 ON e2.group = g
        WHERE e2.student.user = u AND cr.uuid = :courseId AND e2.status = 'ONGOING'
    ))
    AND (CAST(:teacherUuid AS uuid) IS NULL OR 
        u.id = :currentUserId OR 
        (u.role = 'STUDENT' AND EXISTS (
            SELECT 1 FROM Enrollment e_t 
            WHERE e_t.student.user = u 
            AND e_t.group.teacher.user.uuid = :teacherUuid 
            AND e_t.status = 'ONGOING'
        ))
    )
    AND (
        u.role = 'SCHOOL_DIRECTOR'
        OR (
            u.role NOT IN ('STUDENT', 'TEACHER', 'SCHOOL_ADMIN', 'MODERATOR')
            OR (u.role = 'STUDENT' AND sS.status = 'ACTIVE')
            OR (u.role = 'TEACHER' AND tS.status = 'ACTIVE')
            OR (u.role = 'SCHOOL_ADMIN' AND saS.status = 'ACTIVE')
            OR (u.role = 'MODERATOR' AND mS.status = 'ACTIVE')
        )
    )
    AND u.id <> :currentUserId
    ORDER BY u.firstName ASC, u.lastName ASC
""")
    Page<User> findAllWithFilters(
            @Param("status") UserStatus status,
            @Param("role") UserRole role,
            @Param("fullName") String fullName,
            @Param("username") String username,
            @Param("schoolName") String schoolName,
            @Param("scopeSchoolUuids") Collection<UUID> scopeSchoolUuids,
            @Param("currentUserId") Long currentUserId,
            @Param("classId") UUID classId,
            @Param("courseId") UUID courseId,
            @Param("teacherUuid") UUID teacherUuid,
            Pageable pageable
    );

}