package uz.tune.mentourBiz.rest.repository.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherPayslip;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherPayslipRepository extends BaseRepository<TeacherPayslip> {

    @EntityGraph(attributePaths = {"teacher", "teacher.user", "salaryPlan", "approvedBy", "paidBy"})
    Optional<TeacherPayslip> findByUuid(UUID uuid);

    Optional<TeacherPayslip> findByTeacher_User_UuidAndPeriodYearAndPeriodMonth(
            UUID teacherUserUuid, Integer periodYear, Integer periodMonth);

    /** The payroll list for one month, filtered the way the Overview screen filters it. */
    @EntityGraph(attributePaths = {"teacher", "teacher.user"})
    @Query("""
        SELECT p FROM TeacherPayslip p
        LEFT JOIN p.school sc
        LEFT JOIN p.teacher t
        LEFT JOIN t.user tu
        WHERE p.periodYear = :year AND p.periodMonth = :month
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND (:status IS NULL OR p.status = :status)
          AND (CAST(:search AS string) IS NULL
               OR LOWER(CONCAT(tu.firstName, ' ', tu.lastName))
                  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY p.netPay DESC
    """)
    Page<TeacherPayslip> findForPeriod(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("status") PayrollEnums.PayslipStatus status,
            @Param("search") String search,
            Pageable pageable);

    /** Every payslip of a month, for the KPI cards. */
    @Query("""
        SELECT p FROM TeacherPayslip p
        LEFT JOIN p.school sc
        WHERE p.periodYear = :year AND p.periodMonth = :month
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
    """)
    List<TeacherPayslip> findAllForPeriod(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("schoolUuids") Collection<UUID> schoolUuids);

    /**
     * What each salary plan costs in a given month, from the payslips actually generated.
     * Row: [planUuid (UUID), netPaySum (Long), payslipCount (Long)].
     */
    @Query("""
        SELECT sp.uuid, COALESCE(SUM(p.netPay), 0), COUNT(p)
        FROM TeacherPayslip p
        JOIN p.salaryPlan sp
        WHERE sp.uuid IN :planUuids
          AND p.periodYear = :year AND p.periodMonth = :month
        GROUP BY sp.uuid
    """)
    List<Object[]> sumNetPayByPlan(
            @Param("planUuids") Collection<UUID> planUuids,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /** Uuids of teachers who already have a payslip for the period, so generation can skip them. */
    @Query("""
        SELECT tu.uuid FROM TeacherPayslip p
        LEFT JOIN p.school sc
        LEFT JOIN p.teacher t
        LEFT JOIN t.user tu
        WHERE p.periodYear = :year AND p.periodMonth = :month
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
    """)
    List<UUID> findTeacherUuidsWithPayslip(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("schoolUuids") Collection<UUID> schoolUuids);

    /**
     * The teacher's payslips that still owe money, oldest period first — the order a payment is
     * allocated in, so a remainder carried over from an earlier month is always cleared before the
     * current one.
     */
    @EntityGraph(attributePaths = {"teacher", "teacher.user"})
    @Query("""
        SELECT p FROM TeacherPayslip p
        WHERE p.teacher.user.uuid = :teacherUuid
          AND p.status IN (uz.tune.mentourBiz.rest.enums.PayrollEnums.PayslipStatus.APPROVED,
                           uz.tune.mentourBiz.rest.enums.PayrollEnums.PayslipStatus.PARTIALLY_PAID)
        ORDER BY p.periodYear ASC, p.periodMonth ASC, p.id ASC
    """)
    List<TeacherPayslip> findOpenForTeacher(@Param("teacherUuid") UUID teacherUuid);

    /** How many months are still owing per teacher, for the balances list. Row: [teacherUuid, count]. */
    @Query("""
        SELECT tu.uuid, COUNT(p)
        FROM TeacherPayslip p
        JOIN p.teacher t
        JOIN t.user tu
        WHERE tu.uuid IN :teacherUuids
          AND p.status IN (uz.tune.mentourBiz.rest.enums.PayrollEnums.PayslipStatus.APPROVED,
                           uz.tune.mentourBiz.rest.enums.PayrollEnums.PayslipStatus.PARTIALLY_PAID)
        GROUP BY tu.uuid
    """)
    List<Object[]> countOpenByTeacher(@Param("teacherUuids") Collection<UUID> teacherUuids);
}
