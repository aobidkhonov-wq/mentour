package uz.tune.mentourBiz.rest.repository.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.payroll.PayrollEvent;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollEventRepository extends BaseRepository<PayrollEvent> {

    Optional<PayrollEvent> findByUuid(UUID uuid);

    /**
     * The History screen's feed. Every filter is optional; a null one drops out of the query rather
     * than matching nothing.
     */
    // Every association is LEFT JOINed on purpose. A bonus has no group, a system event has no
    // addedBy, and an implicit join through those paths would quietly drop exactly those rows —
    // which is most of the feed.
    @EntityGraph(attributePaths = {"teacher", "teacher.user", "group", "student", "student.user", "addedBy"})
    @Query("""
        SELECT e FROM PayrollEvent e
        LEFT JOIN e.school sc
        LEFT JOIN e.teacher t
        LEFT JOIN t.user tu
        LEFT JOIN e.group g
        LEFT JOIN e.student st
        LEFT JOIN st.user stu
        LEFT JOIN e.addedBy ab
        WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND (COALESCE(:teacherUuids, NULL) IS NULL OR tu.uuid IN :teacherUuids)
          AND (COALESCE(:eventTypes, NULL) IS NULL OR e.eventType IN :eventTypes)
          AND (COALESCE(:groupUuids, NULL) IS NULL OR g.uuid IN :groupUuids)
          AND (:studentUuid IS NULL OR stu.uuid = :studentUuid)
          AND (:addedByUuid IS NULL OR ab.uuid = :addedByUuid)
          AND (:year IS NULL OR e.periodYear = :year)
          AND (:month IS NULL OR e.periodMonth = :month)
          AND (CAST(:from AS timestamp) IS NULL OR e.occurredAt >= :from)
          AND (CAST(:toExclusive AS timestamp) IS NULL OR e.occurredAt < :toExclusive)
          AND (CAST(:search AS string) IS NULL
               OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(e.subtitle) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(e.note) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(CONCAT(tu.firstName, ' ', tu.lastName))
                  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY e.occurredAt DESC
    """)
    Page<PayrollEvent> findWithFilters(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("teacherUuids") Collection<UUID> teacherUuids,
            @Param("eventTypes") Collection<PayrollEnums.PayrollEventType> eventTypes,
            @Param("groupUuids") Collection<UUID> groupUuids,
            @Param("studentUuid") UUID studentUuid,
            @Param("addedByUuid") UUID addedByUuid,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("search") String search,
            Pageable pageable);

    /** Event counts per type for the History KPI cards. Row: [eventType, count (Long)]. */
    @Query("""
        SELECT e.eventType, COUNT(e) FROM PayrollEvent e
        LEFT JOIN e.school sc
        WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND (:year IS NULL OR e.periodYear = :year)
          AND (:month IS NULL OR e.periodMonth = :month)
        GROUP BY e.eventType
    """)
    List<Object[]> countByType(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /** A teacher's events for one period, used to build and to show their payslip. */
    @EntityGraph(attributePaths = {"group", "addedBy"})
    List<PayrollEvent> findAllByTeacher_User_UuidAndPeriodYearAndPeriodMonthOrderByOccurredAtDesc(
            UUID teacherUserUuid, Integer periodYear, Integer periodMonth);

    void deleteAllByTeacher_User_UuidAndPeriodYearAndPeriodMonthAndAddedByIsNull(
            UUID teacherUserUuid, Integer periodYear, Integer periodMonth);
}
