package uz.tune.mentourBiz.rest.repository.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherBalanceEntry;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherBalanceEntryRepository extends BaseRepository<TeacherBalanceEntry> {

    /** What the school still owes this teacher: the whole ledger summed, carry-over included. */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM TeacherBalanceEntry e
        WHERE e.teacher.user.uuid = :teacherUuid
    """)
    long balanceOf(@Param("teacherUuid") UUID teacherUuid);

    /** The same figure for a whole list of teachers. Row: [teacherUserUuid (UUID), balance (Long)]. */
    @Query("""
        SELECT tu.uuid, COALESCE(SUM(e.amount), 0)
        FROM TeacherBalanceEntry e
        JOIN e.teacher t
        JOIN t.user tu
        WHERE tu.uuid IN :teacherUuids
        GROUP BY tu.uuid
    """)
    List<Object[]> balancesOf(@Param("teacherUuids") Collection<UUID> teacherUuids);

    /** Everything ever credited (or debited) to this teacher, split by entry type. Row: [type, sum]. */
    @Query("""
        SELECT e.entryType, COALESCE(SUM(e.amount), 0)
        FROM TeacherBalanceEntry e
        WHERE e.teacher.user.uuid = :teacherUuid
        GROUP BY e.entryType
    """)
    List<Object[]> sumByType(@Param("teacherUuid") UUID teacherUuid);

    /** The balance history feed for one teacher, newest first. */
    @EntityGraph(attributePaths = {"teacher", "teacher.user", "payslip", "payment", "createdBy"})
    Page<TeacherBalanceEntry> findAllByTeacher_User_UuidOrderByOccurredAtDescIdDesc(
            UUID teacherUserUuid, Pageable pageable);

    /**
     * The live accrual entries of one payslip — a payslip that was approved, reopened and approved again
     * has an older ACCRUAL already cancelled by a REVERSAL, so approval has to look at what is currently
     * outstanding rather than at whether an accrual has ever existed.
     */
    List<TeacherBalanceEntry> findAllByPayslip_UuidAndEntryTypeIn(
            UUID payslipUuid, Collection<PayrollEnums.BalanceEntryType> entryTypes);

    /**
     * The balances screen, ordered by what is owed, across the whole result rather than within one
     * page. Sorting a page after it has been fetched would put a teacher owed more on page two than
     * someone on page one, which is exactly backwards for a screen whose job is "who do we owe".
     *
     * <p>The join runs from Teacher outwards so that a teacher with no ledger entries still appears,
     * at zero. Row: [teacherUserUuid (UUID), firstName, lastName, balance (Long)].
     */
    @Query(value = """
        SELECT tu.uuid, tu.firstName, tu.lastName, COALESCE(SUM(e.amount), 0)
        FROM Teacher t
        JOIN t.user tu
        LEFT JOIN TeacherBalanceEntry e ON e.teacher = t
        WHERE tu.status = uz.tune.mentourBiz.rest.enums.UserStatus.ACTIVE
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR t.school.uuid IN :schoolUuids)
          AND (CAST(:search AS string) IS NULL
               OR LOWER(CONCAT(tu.firstName, ' ', tu.lastName))
                  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        GROUP BY tu.uuid, tu.firstName, tu.lastName
        ORDER BY COALESCE(SUM(e.amount), 0) DESC, tu.firstName ASC
    """,
            countQuery = """
        SELECT COUNT(t)
        FROM Teacher t
        JOIN t.user tu
        WHERE tu.status = uz.tune.mentourBiz.rest.enums.UserStatus.ACTIVE
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR t.school.uuid IN :schoolUuids)
          AND (CAST(:search AS string) IS NULL
               OR LOWER(CONCAT(tu.firstName, ' ', tu.lastName))
                  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
    """)
    Page<Object[]> findBalancesOrdered(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("search") String search,
            Pageable pageable);
}
