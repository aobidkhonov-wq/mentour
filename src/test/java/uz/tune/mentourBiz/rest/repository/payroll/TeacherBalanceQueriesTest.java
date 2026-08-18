package uz.tune.mentourBiz.rest.repository.payroll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the queries that decide how much a teacher is owed against a real database.
 *
 * <p>A malformed {@code @Query} is not a compile error — it surfaces when Hibernate parses it, which
 * for the application means at startup and for a deploy means after it has already gone out. Booting
 * the JPA slice at all proves every query in the module parses; the assertions below then execute the
 * ones the balance is actually computed from. They are deliberately thin: the point is that each query
 * runs, not what an empty schema returns.
 *
 * <p>The paginated filter queries — {@code findBalancesOrdered}, {@code findWithFilters} on payments
 * and expenses — are parsed here but not executed. They use this codebase's optional-filter idiom,
 * {@code COALESCE(:param, NULL) IS NULL}, which Postgres types and H2 rejects outright whatever is
 * bound to it; the expense one additionally loads {@code School}, whose {@code @Formula} columns query
 * a table H2 will not create because {@code groups} is a reserved word there. Both are limits of the
 * test database, not of the queries, and neither affects the arithmetic these tests cover.
 */
@DataJpaTest
class TeacherBalanceQueriesTest {

    private static final UUID ABSENT = UUID.randomUUID();

    @Autowired
    private TeacherBalanceEntryRepository balanceEntryRepository;

    @Autowired
    private TeacherPaymentAllocationRepository allocationRepository;

    @Autowired
    private TeacherPayslipRepository payslipRepository;

    @Test
    @DisplayName("the balance ledger queries run")
    void balanceLedgerQueries() {
        // A teacher with no ledger at all has to come back as zero rather than null: this figure is
        // summed straight into the ceiling on the next payment.
        assertEquals(0L, balanceEntryRepository.balanceOf(ABSENT));
        assertTrue(balanceEntryRepository.balancesOf(List.of(ABSENT)).isEmpty());
        assertTrue(balanceEntryRepository.sumByType(ABSENT).isEmpty());
        assertNotNull(balanceEntryRepository
                .findAllByTeacher_User_UuidOrderByOccurredAtDescIdDesc(ABSENT, PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("approval can find a payslip's live accruals")
    void accrualLookup() {
        // What keeps approval idempotent across reopen and re-approve.
        assertTrue(balanceEntryRepository.findAllByPayslip_UuidAndEntryTypeIn(
                ABSENT,
                List.of(PayrollEnums.BalanceEntryType.ACCRUAL,
                        PayrollEnums.BalanceEntryType.REVERSAL)).isEmpty());
    }

    @Test
    @DisplayName("the allocation queries run")
    void allocationQueries() {
        assertEquals(0L, allocationRepository.paidAmountOf(ABSENT));
        assertTrue(allocationRepository.paidAmountsOf(List.of(ABSENT)).isEmpty());
        assertTrue(allocationRepository.findAllByPayment_Uuid(ABSENT).isEmpty());
    }

    @Test
    @DisplayName("the settlement queries added to payslips run")
    void payslipSettlementQueries() {
        // The FIFO order a payment is allocated in, and the open-month count beside each balance.
        assertTrue(payslipRepository.findOpenForTeacher(ABSENT).isEmpty());
        assertTrue(payslipRepository.countOpenByTeacher(List.of(ABSENT)).isEmpty());
    }
}
