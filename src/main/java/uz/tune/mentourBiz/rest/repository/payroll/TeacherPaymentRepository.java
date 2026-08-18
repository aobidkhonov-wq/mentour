package uz.tune.mentourBiz.rest.repository.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherPayment;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherPaymentRepository extends BaseRepository<TeacherPayment> {

    @EntityGraph(attributePaths = {"teacher", "teacher.user", "school", "createdBy", "expense"})
    Optional<TeacherPayment> findByUuid(UUID uuid);

    /** The payments feed. Every filter is optional; reversed payments never appear. */
    @EntityGraph(attributePaths = {"teacher", "teacher.user", "createdBy"})
    @Query("""
        SELECT p FROM TeacherPayment p
        LEFT JOIN p.school sc
        LEFT JOIN p.teacher t
        LEFT JOIN t.user tu
        WHERE p.deleted = false
          AND (COALESCE(:schoolUuids, NULL) IS NULL OR sc.uuid IN :schoolUuids)
          AND (COALESCE(:teacherUuids, NULL) IS NULL OR tu.uuid IN :teacherUuids)
          AND (:type IS NULL OR p.type = :type)
          AND (CAST(:fromDate AS date) IS NULL OR p.paymentDate >= :fromDate)
          AND (CAST(:toDate AS date) IS NULL OR p.paymentDate <= :toDate)
        ORDER BY p.paymentDate DESC, p.id DESC
    """)
    Page<TeacherPayment> findWithFilters(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("teacherUuids") Collection<UUID> teacherUuids,
            @Param("type") PayrollEnums.TeacherPaymentType type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
