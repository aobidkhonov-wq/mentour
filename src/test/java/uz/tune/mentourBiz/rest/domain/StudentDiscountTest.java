package uz.tune.mentourBiz.rest.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uz.tune.mentourBiz.rest.domain.userManagement.user.StudentDiscount;
import uz.tune.mentourBiz.rest.enums.DiscountType;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a student actually pays once their discounts are applied. A student may hold one discount of
 * each type, so the arithmetic has to stay predictable when both are in play.
 */
class StudentDiscountTest {

    private static final long MONTHLY_FEE = 500_000L;

    @Test
    @DisplayName("a fixed discount comes straight off the fee")
    void fixedDiscount() {
        assertEquals(100_000L, fixed(100_000).discountOn(MONTHLY_FEE));
    }

    @Test
    @DisplayName("a percentage discount is worked out on the fee")
    void percentDiscount() {
        assertEquals(50_000L, percent(10).discountOn(MONTHLY_FEE));
    }

    @Test
    @DisplayName("both types apply together, each measured against the full fee")
    void bothTypesStack() {
        long off = StudentDiscount.totalDiscountOn(List.of(percent(10), fixed(100_000)), MONTHLY_FEE);

        assertEquals(150_000L, off);
        assertEquals(350_000L, MONTHLY_FEE - off, "the student pays 350 000 of a 500 000 fee");
    }

    @Test
    @DisplayName("stacked discounts never exceed the price and never pay the student")
    void discountIsCappedAtThePrice() {
        assertEquals(50_000L, StudentDiscount.totalDiscountOn(
                List.of(percent(50), fixed(100_000)), 50_000L));
        assertEquals(0L, StudentDiscount.totalDiscountOn(List.of(fixed(100_000)), 0L));
    }

    @Test
    @DisplayName("a fixed discount larger than a per-lesson charge just makes the lesson free")
    void fixedDiscountBiggerThanPrice() {
        assertEquals(40_000L, fixed(100_000).discountOn(40_000L));
    }

    @Test
    @DisplayName("a discount applies from its start date until its end date, end excluded")
    void windowIsHalfOpen() {
        StudentDiscount discount = fixed(100_000);
        discount.setStartDate(LocalDate.of(2026, 8, 10));
        discount.setDurationMonths(3);
        discount.setEndDate(LocalDate.of(2026, 11, 10));

        assertFalse(discount.appliesOn(LocalDate.of(2026, 8, 9)), "not started yet");
        assertTrue(discount.appliesOn(LocalDate.of(2026, 8, 10)));
        assertTrue(discount.appliesOn(LocalDate.of(2026, 11, 9)), "last day of the third month");
        assertFalse(discount.appliesOn(LocalDate.of(2026, 11, 10)), "the fourth month is no longer covered");
    }

    @Test
    @DisplayName("a permanent discount has no end, a switched-off one never applies")
    void permanentAndInactive() {
        StudentDiscount permanent = fixed(100_000);
        assertTrue(permanent.isPermanent());
        assertTrue(permanent.appliesOn(LocalDate.of(2030, 1, 1)));

        permanent.setIsActive(false);
        assertFalse(permanent.appliesOn(LocalDate.now()));
    }

    private static StudentDiscount fixed(long amount) {
        StudentDiscount discount = new StudentDiscount();
        discount.setType(DiscountType.FIXED);
        discount.setAmount(amount);
        discount.setStartDate(LocalDate.of(2026, 1, 1));
        return discount;
    }

    private static StudentDiscount percent(int percent) {
        StudentDiscount discount = new StudentDiscount();
        discount.setType(DiscountType.PERCENT);
        discount.setPercent(percent);
        discount.setStartDate(LocalDate.of(2026, 1, 1));
        return discount;
    }
}
