package uz.tune.mentourBiz.rest.repository.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.finance.SchoolExpense;
import uz.tune.mentourBiz.rest.enums.ExpenseEnums;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolExpenseRepository extends BaseRepository<SchoolExpense> {

    @EntityGraph(attributePaths = {"school", "createdBy"})
    Optional<SchoolExpense> findByUuid(UUID uuid);

    /** The expense list. Every filter is optional; deleted rows never appear. */
    @EntityGraph(attributePaths = {"school", "createdBy"})
    @Query("""
        SELECT e FROM SchoolExpense e
        LEFT JOIN e.school sc
        WHERE e.deleted = false
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND (COALESCE(:categories, NULL) IS NULL OR e.category IN :categories)
          AND (CAST(:fromDate AS date) IS NULL OR e.expenseDate >= :fromDate)
          AND (CAST(:toDate AS date) IS NULL OR e.expenseDate <= :toDate)
          AND (CAST(:search AS string) IS NULL
               OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY e.expenseDate DESC, e.id DESC
    """)
    Page<SchoolExpense> findWithFilters(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("categories") Collection<ExpenseEnums.ExpenseCategory> categories,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search,
            Pageable pageable);

    /** Totals per category for the summary cards. Row: [category, amountSum (Long), count (Long)]. */
    @Query("""
        SELECT e.category, COALESCE(SUM(e.amount), 0), COUNT(e)
        FROM SchoolExpense e
        LEFT JOIN e.school sc
        WHERE e.deleted = false
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND (CAST(:fromDate AS date) IS NULL OR e.expenseDate >= :fromDate)
          AND (CAST(:toDate AS date) IS NULL OR e.expenseDate <= :toDate)
        GROUP BY e.category
    """)
    List<Object[]> sumByCategory(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
