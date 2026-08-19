package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.Notification;

import java.util.Collection;
import java.util.UUID;

public interface NotificationRepo extends BaseRepository<Notification> {
    @Query("""
        SELECT n FROM Notification n 
        WHERE (n.targetType = 'INDIVIDUAL_USER' AND n.targetUuid = :userUuid)
           OR (n.targetType = 'GROUP_WIDE' AND n.targetUuid = :groupUuid)
           OR (n.targetType = 'SCHOOL_WIDE' AND n.targetUuid = :schoolUuid)
           OR (n.targetType = 'BROADCAST_ALL')
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findMyNotifications(
            @Param("userUuid") UUID userUuid,
            @Param("groupUuid") UUID groupUuid,
            @Param("schoolUuid") UUID schoolUuid,
            Pageable pageable
    );

    @Query("""
    SELECT n FROM Notification n 
    WHERE (n.targetType = 'SCHOOL_WIDE' AND n.targetUuid IN :schoolUuids)
       OR (n.targetType = 'GROUP_WIDE' AND n.targetUuid IN (
           SELECT g.uuid FROM Group g WHERE g.branch.school.uuid IN :schoolUuids
       ))
       OR (n.targetType = 'ORGANIZATION_WIDE' AND n.targetUuid = :orgUuid)
    ORDER BY n.createdAt DESC
""")
    Page<Notification> findSentNotificationsForDirector(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("orgUuid") UUID orgUuid,
            Pageable pageable);

    @Query("""
    SELECT n FROM Notification n 
    WHERE 
        (n.targetType = 'SCHOOL_WIDE' AND n.targetUuid = :schoolUuid)
        OR 
        (n.targetType = 'GROUP_WIDE' AND n.targetUuid IN (
            SELECT g.uuid FROM Group g WHERE g.branch.school.uuid = :schoolUuid
        ))
    ORDER BY n.createdAt DESC
""")
    Page<Notification> findSentNotificationsBySchool(@Param("schoolUuid") UUID schoolUuid, Pageable pageable);

    @Query("""
                SELECT n FROM Notification n 
                WHERE (n.targetType = 'GROUP_WIDE' AND n.targetUuid IN (
                        SELECT g.uuid FROM Group g WHERE g.uuid=:groupUuid
                    ))
                ORDER BY n.createdAt DESC
            """)
    Page<Notification> findSentNotificationsByGroup(@Param("groupUuid") UUID groupUuid,
                                                    Pageable pageable);

    @Query("""
                SELECT n FROM Notification n 
                WHERE (n.targetType = 'INDIVIDUAL_USER' AND n.targetUuid IN (
                        SELECT u.uuid FROM User u WHERE u.uuid=:userUuid
                    ))
                ORDER BY n.createdAt DESC
            """)
    Page<Notification> findSentNotificationsByUser(@Param("userUuid") UUID groupUuid,
                                                    Pageable pageable);

    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC")
    Page<Notification> findAllSent(Pageable pageable);
}
