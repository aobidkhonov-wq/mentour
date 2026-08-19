package uz.tune.mentourBiz.rest.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a student's wallet movements are attributed to the groups their teachers are paid on.
 *
 * <p>The case that matters is a student in two groups: payments arrive on one shared wallet, so
 * without an earmark the only thing deciding which teacher is credited is the order the charges
 * happen to be in.
 */
class TeacherPayrollSettlementTest {

    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID GROUP_A = UUID.randomUUID();
    private static final UUID GROUP_B = UUID.randomUUID();
    private static final Set<UUID> BOTH = Set.of(GROUP_A, GROUP_B);

    private static final Instant MONTH_START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant MONTH_END = Instant.parse("2026-08-01T00:00:00Z");

    private Instant clock = MONTH_START;
    private final List<Object[]> ledger = new ArrayList<>();

    @Test
    @DisplayName("an unearmarked payment still settles oldest-first, across group boundaries")
    void unearmarkedPaymentKeepsOldestFirst() {
        charge(GROUP_A, 100_000);
        charge(GROUP_B, 200_000);
        payment(null, 150_000);

        TeacherPayrollService.GroupRevenue out = settle();

        // The whole payment walks the queue from the oldest charge: A is cleared, B gets the rest.
        assertEquals(100_000L, collected(out, GROUP_A));
        assertEquals(50_000L, collected(out, GROUP_B));
    }

    @Test
    @DisplayName("an earmarked payment clears the named group first, not the oldest charge")
    void earmarkedPaymentGoesToItsGroup() {
        charge(GROUP_A, 100_000);
        charge(GROUP_B, 200_000);
        payment(GROUP_B, 150_000);

        TeacherPayrollService.GroupRevenue out = settle();

        // Without the earmark this 150 000 would have cleared A entirely. It was paid for B.
        assertEquals(0L, collected(out, GROUP_A));
        assertEquals(150_000L, collected(out, GROUP_B));
    }

    @Test
    @DisplayName("what the named group cannot absorb falls back to oldest-first")
    void earmarkOverflowFallsBack() {
        charge(GROUP_A, 100_000);
        charge(GROUP_B, 200_000);
        payment(GROUP_B, 250_000);

        TeacherPayrollService.GroupRevenue out = settle();

        // B's 200 000 is settled from the earmark; the surplus 50 000 is ordinary wallet money.
        assertEquals(50_000L, collected(out, GROUP_A));
        assertEquals(200_000L, collected(out, GROUP_B));
    }

    @Test
    @DisplayName("an earmark for a group with no debt leaves the other group's charges alone")
    void earmarkAheadOfTheCharge() {
        charge(GROUP_A, 100_000);
        payment(GROUP_B, 80_000);

        TeacherPayrollService.GroupRevenue out = settle();

        // B has nothing outstanding, so the money falls through to A — the student does owe it.
        assertEquals(80_000L, collected(out, GROUP_A));
        assertEquals(0L, collected(out, GROUP_B));
    }

    @Test
    @DisplayName("billed is unaffected by the earmark: each charge stays on its own group")
    void billedStaysPerGroup() {
        charge(GROUP_A, 100_000);
        charge(GROUP_B, 200_000);
        payment(GROUP_B, 300_000);

        TeacherPayrollService.GroupRevenue out = settle();

        assertEquals(100_000L, billed(out, GROUP_A));
        assertEquals(200_000L, billed(out, GROUP_B));
    }

    // ---- helpers ---------------------------------------------------------------------------------

    /** Charges are stored as negative amounts, oldest first. */
    private void charge(UUID group, long amount) {
        ledger.add(new Object[]{STUDENT, group, FinanceEnums.FinanceTransactionType.CHARGE, -amount, tick(), 0L});
    }

    private void payment(UUID group, long amount) {
        ledger.add(new Object[]{STUDENT, group, FinanceEnums.FinanceTransactionType.PAYMENT, amount, tick(), 0L});
    }

    private Instant tick() {
        clock = clock.plus(1, ChronoUnit.DAYS);
        return clock;
    }

    private TeacherPayrollService.GroupRevenue settle() {
        TeacherPayrollService.GroupRevenue out = TeacherPayrollService.GroupRevenue.mutable();
        TeacherPayrollService.settleStudent(STUDENT, ledger, BOTH, MONTH_START, MONTH_END, out);
        return out;
    }

    private static long collected(TeacherPayrollService.GroupRevenue out, UUID group) {
        return out.collected().getOrDefault(group, 0L);
    }

    private static long billed(TeacherPayrollService.GroupRevenue out, UUID group) {
        return out.billed().getOrDefault(group, 0L);
    }
}
