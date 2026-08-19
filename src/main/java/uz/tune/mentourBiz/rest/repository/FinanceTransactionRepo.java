package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.FinanceTransaction;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceTransactionRepo extends BaseRepository<FinanceTransaction> {

    // "Has this student ever been charged" for status. Soft-deleted charges don't count.
    boolean existsByStudentAndTypeAndDeletedFalse(Student student, FinanceEnums.FinanceTransactionType type);

    // Loads a still-live transaction for the delete endpoint; a row already soft-deleted is not found,
    // so it cannot be deleted (and its balance reversed) twice.
    Optional<FinanceTransaction> findByUuidAndDeletedFalse(UUID uuid);

    /**
     * Back-dates an already saved transaction. Every finance report, history query and sort keys off
     * created_at, so a payment the front-end registers for an earlier day must move that column rather
     * than carry a second date field. Native because created_at is {@code updatable = false} and is
     * managed by {@code @CreationTimestamp}, which would otherwise overwrite anything set in Java.
     */
    @Modifying
    @Query(value = "UPDATE finance_transactions SET created_at = :paymentDate WHERE id = :id", nativeQuery = true)
    void updateTransactionDate(@Param("id") Long id, @Param("paymentDate") Instant paymentDate);

    // ---- Teacher payroll: per-group revenue attribution -------------------------------------------
    //
    // Payments land on the student's shared wallet and carry no group of their own, so payroll cannot
    // read a group's collected revenue straight out of a SUM. Instead it replays the student's whole
    // ledger and settles payments against charges oldest-first; a payment is then credited to the group
    // whose charge it actually covered. That needs the full history, not just the payroll month, because
    // a payment in August routinely settles a charge from July.

    /**
     * Students ever billed by any of the given groups up to {@code toExclusive} — the set whose ledgers
     * payroll has to replay.
     */
    @Query("""
        SELECT DISTINCT ft.student.uuid
        FROM FinanceTransaction ft
        WHERE ft.type IN ('CHARGE', 'ADJUSTMENT')
          AND ft.group.uuid IN :groupUuids
          AND ft.createdAt < :toExclusive
          AND ft.deleted = false
    """)
    List<UUID> findStudentsBilledByGroups(
            @Param("groupUuids") Collection<UUID> groupUuids,
            @Param("toExclusive") Instant toExclusive);

    /**
     * Every wallet movement of the given students up to {@code toExclusive}, oldest first.
     * Row: [studentUuid (UUID), groupUuid (UUID, null for wallet-level movements), type
     * (FinanceTransactionType), amount (Long, negative for charges), createdAt (Instant),
     * discountAmount (Long, positive, the part of the price the student was let off)].
     *
     * <p>The group is joined with LEFT JOIN on purpose: initial charges, package charges and write-offs
     * have no group, and dropping them would make the settlement over-credit the groups that remain.
     *
     * <p>The discount comes along because payroll pays the teacher on the full price: the settlement
     * adds it back onto the charge and treats it as settled the moment it is made.
     */
    @Query("""
        SELECT ft.student.uuid, g.uuid, ft.type, ft.amount, ft.createdAt, ft.discountAmount
        FROM FinanceTransaction ft
        LEFT JOIN ft.group g
        WHERE ft.student.uuid IN :studentUuids
          AND ft.createdAt < :toExclusive
          AND ft.deleted = false
        ORDER BY ft.createdAt, ft.id
    """)
    List<Object[]> findLedgerForStudents(
            @Param("studentUuids") Collection<UUID> studentUuids,
            @Param("toExclusive") Instant toExclusive);

    @Query("""
    SELECT COALESCE(SUM(ft.amount), 0) FROM FinanceTransaction ft
    WHERE ft.student.school.uuid = :schoolUuid
    AND ft.type = 'PAYMENT'
    AND ft.deleted = false
    AND ft.createdAt >= :since
    AND (:activeOnly = false OR ft.student.user.status = 'ACTIVE')
""")
    Long sumCollectedRevenueFiltered(@Param("schoolUuid") UUID schoolUuid, @Param("since") Instant since, @Param("activeOnly") boolean activeOnly);

    @Query("""
        SELECT ft FROM FinanceTransaction ft
        WHERE ft.student.user.uuid = :userUuid
        AND (:method IS NULL OR ft.method = :method)
        AND ft.type = 'PAYMENT'
        AND ft.deleted = false
        AND (CAST(:fromDate AS timestamp) IS NULL OR ft.createdAt >= :fromDate)
        AND (CAST(:toDate AS timestamp) IS NULL OR ft.createdAt <= :toDate)
    """)
    Page<FinanceTransaction> findAllHistoryByStudentUserUuid(
            @Param("userUuid") UUID userUuid,
            @Param("method") FinanceEnums.PaymentMethod method,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);

    @Query("""
    SELECT COALESCE(SUM(ft.amount), 0) FROM FinanceTransaction ft
    WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR ft.student.school.uuid IN :schoolUuids)
    AND ft.type = :type
    AND ft.deleted = false
    AND (:method IS NULL OR ft.method = :method)
    AND (:excludeBonus = false OR ft.method != 'BONUS')
    AND (:groupUuid IS NULL OR EXISTS (SELECT 1 FROM Enrollment e WHERE e.student = ft.student AND e.group.uuid = :groupUuid AND e.status = 'ONGOING'))
    AND (:courseUuid IS NULL OR EXISTS (SELECT 1 FROM Enrollment ec JOIN Course c ON c.group = ec.group WHERE ec.student = ft.student AND c.uuid = :courseUuid AND ec.status = 'ONGOING'))
    AND (:teacherUuid IS NULL OR EXISTS (SELECT 1 FROM Enrollment et WHERE et.student = ft.student AND et.group.teacher.user.uuid = :teacherUuid AND et.status = 'ONGOING'))
    AND (:activeOnly = false OR ft.student.user.status = 'ACTIVE')
    AND (CAST(:fromDate AS timestamp) IS NULL OR ft.createdAt >= :fromDate)
    AND (CAST(:toDate AS timestamp) IS NULL OR ft.createdAt <= :toDate)
""")
    Long sumFilteredTransactions(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("type") FinanceEnums.FinanceTransactionType type,
            @Param("method") FinanceEnums.PaymentMethod method,
            @Param("groupUuid") UUID groupUuid,
            @Param("courseUuid") UUID courseUuid,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("activeOnly") boolean activeOnly,
            @Param("excludeBonus") boolean excludeBonus);

    @Query("""
    SELECT COALESCE(SUM(ft.amount), 0) FROM FinanceTransaction ft
    WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR ft.student.school.uuid IN :schoolUuids)
    AND ft.type = 'PAYMENT'
    AND ft.deleted = false
    AND (:activeOnly = false OR ft.student.user.status = 'ACTIVE')
    AND (CAST(:fromDate AS timestamp) IS NULL OR ft.createdAt >= :fromDate)
    AND (CAST(:toDate AS timestamp) IS NULL OR ft.createdAt <= :toDate)
""")
    Long sumRevenueInRangeFilteredMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("fromDate") java.time.Instant fromDate,
            @Param("toDate") java.time.Instant toDate,
            @Param("activeOnly") boolean activeOnly);
    @Query("""
    SELECT ft FROM FinanceTransaction ft
    WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR ft.student.school.uuid IN :schoolUuids)
    AND (:method IS NULL OR ft.method = :method)
    AND ft.type = 'PAYMENT'
    AND ft.deleted = false
    AND ft.student.user.status = 'ACTIVE'
    AND (CAST(:fromDate AS timestamp) IS NULL OR ft.createdAt >= :fromDate)
    AND (CAST(:toDate AS timestamp) IS NULL OR ft.createdAt <= :toDate)
""")
    Page<FinanceTransaction> findHistoryMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("method") FinanceEnums.PaymentMethod method,
            @Param("fromDate") java.time.Instant fromDate,
            @Param("toDate") java.time.Instant toDate,
            Pageable pageable);

    // ---- Type-filtered history --------------------------------------------------------------------
    // Generic history keyed by transaction type, chosen by the caller (front-end). Unlike the
    // PAYMENT-only queries above, :type is optional: pass CHARGE to see money auto-deducted from
    // students (billing-plan renewals, per-lesson charges, initial/monthly charges), PAYMENT for
    // top-ups, ADJUSTMENT for refunds/write-offs, or null for every movement. No student-status
    // filter: a row stays visible even after the student is deactivated.

    // This one list is the exception that deliberately keeps soft-deleted rows: the admin history view
    // shows them flagged (ResFinanceTransaction.deleted). Their money is already reversed and left out
    // of sumHistoryByTypeMulti below, so they never inflate the revenue total shown beside the list.
    @Query("""
        SELECT ft FROM FinanceTransaction ft
        WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR ft.student.school.uuid IN :schoolUuids)
        AND (:type IS NULL OR ft.type = :type)
        AND (:method IS NULL OR ft.method = :method)
        AND (CAST(:fromDate AS timestamp) IS NULL OR ft.createdAt >= :fromDate)
        AND (CAST(:toDate AS timestamp) IS NULL OR ft.createdAt <= :toDate)
    """)
    Page<FinanceTransaction> findHistoryByTypeMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("type") FinanceEnums.FinanceTransactionType type,
            @Param("method") FinanceEnums.PaymentMethod method,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);

    @Query("""
        SELECT ft FROM FinanceTransaction ft
        WHERE ft.student.user.uuid = :userUuid
        AND (:type IS NULL OR ft.type = :type)
        AND (:method IS NULL OR ft.method = :method)
        AND ft.deleted = false
        AND (CAST(:fromDate AS timestamp) IS NULL OR ft.createdAt >= :fromDate)
        AND (CAST(:toDate AS timestamp) IS NULL OR ft.createdAt <= :toDate)
    """)
    Page<FinanceTransaction> findHistoryByTypeByStudentUserUuid(
            @Param("userUuid") UUID userUuid,
            @Param("type") FinanceEnums.FinanceTransactionType type,
            @Param("method") FinanceEnums.PaymentMethod method,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);

    // Signed sum of amounts over the same filter (CHARGE amounts are negative, PAYMENT positive).
    // Excludes soft-deleted rows so the revenue total drops the moment a transaction is deleted, even
    // though the deleted row itself still shows in findHistoryByTypeMulti above.
    @Query("""
        SELECT COALESCE(SUM(ft.amount), 0) FROM FinanceTransaction ft
        WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR ft.student.school.uuid IN :schoolUuids)
        AND (:type IS NULL OR ft.type = :type)
        AND (:method IS NULL OR ft.method = :method)
        AND ft.deleted = false
        AND (CAST(:fromDate AS timestamp) IS NULL OR ft.createdAt >= :fromDate)
        AND (CAST(:toDate AS timestamp) IS NULL OR ft.createdAt <= :toDate)
    """)
    Long sumHistoryByTypeMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("type") FinanceEnums.FinanceTransactionType type,
            @Param("method") FinanceEnums.PaymentMethod method,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);
}