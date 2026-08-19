package uz.tune.mentourBiz.rest.repository.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.payroll.SalaryPlan;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryPlanRepository extends BaseRepository<SalaryPlan> {

    Optional<SalaryPlan> findByUuid(UUID uuid);

    /**
     * Plans visible to the caller, newest first. A null {@code schoolUuids} means "every school", which
     * only SYS_ADMIN ever gets.
     */
    @Query("""
        SELECT p FROM SalaryPlan p
        LEFT JOIN p.school sc
        WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND (:status IS NULL OR p.status = :status)
          AND (:planType IS NULL OR p.planType = :planType)
          AND (CAST(:search AS string) IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY p.createdAt DESC
    """)
    Page<SalaryPlan> findWithFilters(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("status") PayrollEnums.SalaryPlanStatus status,
            @Param("planType") PayrollEnums.SalaryPlanType planType,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT p FROM SalaryPlan p
        LEFT JOIN p.school sc
        WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND p.status = 'ACTIVE'
    """)
    List<SalaryPlan> findActive(@Param("schoolUuids") Collection<UUID> schoolUuids);

    /** Teachers assigned to each of the given plans. Row: [planUuid (UUID), count (Long)]. */
    @Query("""
        SELECT sp.uuid, COUNT(tsp)
        FROM TeacherSalaryPlan tsp
        JOIN tsp.salaryPlan sp
        WHERE sp.uuid IN :planUuids
        GROUP BY sp.uuid
    """)
    List<Object[]> countTeachersByPlan(@Param("planUuids") Collection<UUID> planUuids);
}
