package uz.tune.mentourBiz.rest.repository.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.enums.SchoolBookStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolBookRepository extends BaseRepository<SchoolBook> {
    Optional<SchoolBook> findByUuid(UUID uuid);
    List<SchoolBook> findAllByStatus(SchoolBookStatus status);
    List<SchoolBook> findAllByUuidIn(List<UUID> schoolBookUuids);

    @Query("""
    SELECT b FROM SchoolBook b
    LEFT JOIN b.school directSchool
    WHERE directSchool.uuid = :schoolUuid
    OR (b.isGlobal = true AND b.id IN (
        SELECT ab.id FROM School s JOIN s.allowedBooks ab WHERE s.uuid = :schoolUuid
    ))
    OR (b.organization.id = (SELECT sch.organization.id FROM School sch WHERE sch.uuid = :schoolUuid) 
        AND b.school IS NULL)
""")
    List<SchoolBook> findAvailableForSchool(@Param("schoolUuid") UUID schoolUuid);

    @Query(value = """
    SELECT DISTINCT b.* FROM school_book b
    WHERE b.school_book_status = 'ACTIVE'
    AND (
        (b.is_global = true AND EXISTS (
            SELECT 1 FROM school_allowed_books sab
            JOIN schools s2 ON s2.id = sab.schools_id
            WHERE s2.uuid IN :schoolUuids AND sab.school_book_id = b.id
        ))
        OR 
        (b.organization_id IN (SELECT organization_id FROM schools WHERE uuid IN :schoolUuids) 
         AND b.school_id IS NULL) 
        OR
        (EXISTS (SELECT 1 FROM schools s3 WHERE s3.id = b.school_id AND s3.uuid IN :schoolUuids))
    )
    AND (:isGlobal IS NULL OR b.is_global = :isGlobal)
    ORDER BY b.name ASC
    """, nativeQuery = true)
    List<SchoolBook> findAvailableWithFilterMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("isGlobal") Boolean isGlobal
    );

    @Query("""
    SELECT DISTINCT b FROM SchoolBook b
    LEFT JOIN b.school s
    LEFT JOIN b.organization org
    WHERE b.status = 'ACTIVE'
    AND (
        (b.isGlobal = true AND EXISTS (
            SELECT 1 FROM School sch JOIN sch.allowedBooks ab 
            WHERE sch.uuid IN :schoolUuids 
            AND ab.id = b.id
        ))
        OR 
        (CAST(:orgUuid AS uuid) IS NOT NULL AND org.uuid = :orgUuid AND b.school IS NULL)
        OR
        (s.uuid IN :schoolUuids)
    )
    AND (:isGlobal IS NULL OR b.isGlobal = :isGlobal)
    ORDER BY b.name ASC
""")
    Page<SchoolBook> findBooksFiltered(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("orgUuid") UUID orgUuid,
            @Param("isGlobal") Boolean isGlobal,
            Pageable pageable);

    @Query(value = """
        WITH exercise_totals AS (
            
            SELECT u.school_book_id, SUM(q.coin_reward) as exercise_coins
            FROM units u
            JOIN exercise_tasks et ON u.id = et.unit_id
            JOIN exercise_questions eq ON et.id = eq.exercise_task_id
            JOIN questions q ON eq.questions_id = q.id
            WHERE et.status = 'ACTIVE' AND u.unit_status = 'ACTIVE'
            GROUP BY u.school_book_id
        ),
        vocabulary_totals AS (
            SELECT u.school_book_id, SUM(vq.coin_reward) as vocab_coins
            FROM units u
            JOIN vocabulary_sets vs ON u.id = vs.unit_id
            JOIN vocabulary_questions vq ON vs.id = vq.vocabulary_set_id
            WHERE vs.status = 'ACTIVE' AND u.unit_status = 'ACTIVE'
            GROUP BY u.school_book_id
        )

        SELECT 
            CAST(sb.uuid AS VARCHAR) as bookUuid, 
            sb.name as bookName,
            COALESCE(et.exercise_coins, 0) as exerciseCoins,
            COALESCE(vt.vocab_coins, 0) as vocabCoins,
            (COALESCE(et.exercise_coins, 0) + COALESCE(vt.vocab_coins, 0)) as totalCoins
        FROM school_book sb
        LEFT JOIN exercise_totals et ON sb.id = et.school_book_id
        LEFT JOIN vocabulary_totals vt ON sb.id = vt.school_book_id
        WHERE sb.uuid IN :bookUuids
        ORDER BY totalCoins DESC
        """, nativeQuery = true)
    List<Object[]> getBookCoinStatsNative(@Param("bookUuids") List<UUID> bookUuids);

}
