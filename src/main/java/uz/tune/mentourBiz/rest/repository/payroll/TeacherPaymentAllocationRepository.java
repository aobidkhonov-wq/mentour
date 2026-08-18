package uz.tune.mentourBiz.rest.repository.payroll;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherPaymentAllocation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherPaymentAllocationRepository extends BaseRepository<TeacherPaymentAllocation> {

    /** How much has been settled against one payslip, reversed payments excluded. */
    @Query("""
        SELECT COALESCE(SUM(a.amount), 0) FROM TeacherPaymentAllocation a
        WHERE a.payslip.uuid = :payslipUuid AND a.payment.deleted = false
    """)
    long paidAmountOf(@Param("payslipUuid") UUID payslipUuid);

    /** The same figure for many payslips at once. Row: [payslipUuid (UUID), paid (Long)]. */
    @Query("""
        SELECT a.payslip.uuid, COALESCE(SUM(a.amount), 0)
        FROM TeacherPaymentAllocation a
        WHERE a.payslip.uuid IN :payslipUuids AND a.payment.deleted = false
        GROUP BY a.payslip.uuid
    """)
    List<Object[]> paidAmountsOf(@Param("payslipUuids") Collection<UUID> payslipUuids);

    /** The payslips one payment settled, for the payment detail row. */
    @EntityGraph(attributePaths = {"payslip"})
    List<TeacherPaymentAllocation> findAllByPayment_Uuid(UUID paymentUuid);
}
