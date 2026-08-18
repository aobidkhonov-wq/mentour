package uz.tune.mentourBiz.rest.service.finance;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.finance.SchoolExpense;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.ExpenseEnums;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.payload.req.finance.ReqSchoolExpense;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.finance.ResExpenseSummary;
import uz.tune.mentourBiz.rest.payload.res.finance.ResSchoolExpense;
import uz.tune.mentourBiz.rest.repository.finance.SchoolExpenseRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Everything a school spends, in one ledger: rent, utilities, salaries.
 *
 * <p>Salary rows are not entered here. {@code TeacherBalanceService} calls {@link #recordTeacherPayment}
 * when it pays a teacher, and the row it writes is marked auto-generated so nobody can delete the
 * expense out from under the payment that owns it. Everything else is typed in through {@link #create}.
 */
@Service
@RequiredArgsConstructor
public class SchoolExpenseService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    private final SchoolExpenseRepository expenseRepository;
    private final SchoolRepository schoolRepository;
    private final UserScopeService userScopeService;
    private final UserService userService;

    // ---- Reading ---------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ResSchoolExpense> list(List<ExpenseEnums.ExpenseCategory> categories,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       String search,
                                       Pageable pageable) {
        return expenseRepository.findWithFilters(
                        userScopeService.getAuthorizedSchoolUuids(),
                        emptyToNull(categories),
                        fromDate, toDate,
                        blankToNull(search),
                        pageable)
                .map(SchoolExpenseService::toResponse);
    }

    /**
     * Totals for the window. Defaults to the current month, which is what the screen opens on — an
     * unbounded total across every year a school has operated is not a number anyone reads.
     */
    @Transactional(readOnly = true)
    public ResExpenseSummary summary(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now(UZ_ZONE);
        LocalDate from = fromDate != null ? fromDate : today.withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : today.withDayOfMonth(today.lengthOfMonth());
        if (from.isAfter(to)) {
            throw new ValidationException("fromDate cannot be after toDate.");
        }

        List<Object[]> rows = expenseRepository.sumByCategory(
                userScopeService.getAuthorizedSchoolUuids(), from, to);

        long total = 0L;
        long count = 0L;
        for (Object[] row : rows) {
            total += ((Number) row[1]).longValue();
            count += ((Number) row[2]).longValue();
        }

        long teacherSalary = 0L;
        List<ResExpenseSummary.CategoryTotal> categories = new ArrayList<>();
        for (Object[] row : rows) {
            ExpenseEnums.ExpenseCategory category = (ExpenseEnums.ExpenseCategory) row[0];
            long amount = ((Number) row[1]).longValue();
            if (category == ExpenseEnums.ExpenseCategory.TEACHER_SALARY) teacherSalary = amount;
            categories.add(ResExpenseSummary.CategoryTotal.builder()
                    .category(category)
                    .amount(amount)
                    .count(((Number) row[2]).longValue())
                    .percent(share(amount, total))
                    .build());
        }
        categories.sort((a, b) -> Long.compare(nz(b.getAmount()), nz(a.getAmount())));

        return ResExpenseSummary.builder()
                .fromDate(from)
                .toDate(to)
                .totalAmount(total)
                .totalCount(count)
                .teacherSalaryAmount(teacherSalary)
                .categories(categories)
                .build();
    }

    // ---- Writing ---------------------------------------------------------------------------------

    /** Book an expense by hand. */
    @Transactional
    public ResSchoolExpense create(ReqSchoolExpense req) {
        if (req == null) throw new ValidationException("Request body is required.");
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new ValidationException("amount is required and must be positive.");
        }
        if (req.getCategory() == ExpenseEnums.ExpenseCategory.TEACHER_SALARY) {
            throw new ValidationException(
                    "Teacher salaries are booked by paying the teacher, not as a manual expense.");
        }

        UUID schoolUuid = userScopeService.resolveSchoolUuid(req.getSchoolUuid());
        School school = schoolRepository.findByUuid(schoolUuid)
                .orElseThrow(() -> new EntityNotFoundException("School not found."));

        SchoolExpense expense = new SchoolExpense();
        expense.setSchool(school);
        expense.setCategory(req.getCategory() != null ? req.getCategory() : ExpenseEnums.ExpenseCategory.OTHER);
        expense.setAmount(req.getAmount());
        expense.setMethod(req.getMethod());
        expense.setExpenseDate(req.getExpenseDate() != null ? req.getExpenseDate() : LocalDate.now(UZ_ZONE));
        expense.setTitle(blankToNull(req.getTitle()));
        expense.setNote(req.getNote());
        expense.setAutoGenerated(Boolean.FALSE);
        expense.setCreatedBy(userService.getCurrentUser());

        return toResponse(expenseRepository.save(expense));
    }

    /** Soft-delete a hand-entered expense. */
    @Transactional
    public ResponseMessage delete(UUID uuid) {
        SchoolExpense expense = loadInScope(uuid);
        if (Boolean.TRUE.equals(expense.getAutoGenerated())) {
            throw new ValidationException(
                    "This expense belongs to a teacher payment; reverse the payment instead.");
        }
        if (Boolean.TRUE.equals(expense.getDeleted())) {
            return new ResponseMessage("Expense already deleted.");
        }
        expense.setDeleted(Boolean.TRUE);
        expenseRepository.save(expense);
        return new ResponseMessage("Expense deleted.");
    }

    // ---- Called by payroll -----------------------------------------------------------------------

    /**
     * The cash-flow row behind a teacher payment. Returned rather than only saved so the payment can
     * hold on to it and reverse it later.
     */
    @Transactional
    public SchoolExpense recordTeacherPayment(School school, String title, long amount,
                                              FinanceEnums.PaymentMethod method, LocalDate date,
                                              String note, User createdBy) {
        SchoolExpense expense = new SchoolExpense();
        expense.setSchool(school);
        expense.setCategory(ExpenseEnums.ExpenseCategory.TEACHER_SALARY);
        expense.setAmount(amount);
        expense.setMethod(method);
        expense.setExpenseDate(date != null ? date : LocalDate.now(UZ_ZONE));
        expense.setTitle(title);
        expense.setNote(note);
        expense.setAutoGenerated(Boolean.TRUE);
        expense.setCreatedBy(createdBy);
        return expenseRepository.save(expense);
    }

    /** Take an auto-generated expense back out of the totals when its payment is reversed. */
    @Transactional
    public void reverse(SchoolExpense expense) {
        if (expense == null || Boolean.TRUE.equals(expense.getDeleted())) return;
        expense.setDeleted(Boolean.TRUE);
        expenseRepository.save(expense);
    }

    // ---- internals -------------------------------------------------------------------------------

    private SchoolExpense loadInScope(UUID uuid) {
        SchoolExpense expense = expenseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found."));
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        boolean visible = authorized == null
                || (expense.getSchool() != null && authorized.contains(expense.getSchool().getUuid()));
        if (!visible) throw new EntityNotFoundException("Expense not found.");
        return expense;
    }

    static ResSchoolExpense toResponse(SchoolExpense expense) {
        User createdBy = expense.getCreatedBy();
        return ResSchoolExpense.builder()
                .uuid(expense.getUuid())
                .schoolUuid(expense.getSchool() != null ? expense.getSchool().getUuid() : null)
                .category(expense.getCategory())
                .amount(nz(expense.getAmount()))
                .method(expense.getMethod())
                .expenseDate(expense.getExpenseDate())
                .title(expense.getTitle())
                .note(expense.getNote())
                .autoGenerated(Boolean.TRUE.equals(expense.getAutoGenerated()))
                .createdByName(createdBy != null
                        ? (createdBy.getFirstName() + " " + createdBy.getLastName()) : null)
                .createdAt(expense.getCreatedAt())
                .build();
    }

    private static Double share(long part, long total) {
        return total > 0 ? Math.round(part * 1000.0 / total) / 10.0 : 0.0;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static <T> Collection<T> emptyToNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list;
    }
}
