package uz.tune.mentourBiz.rest.endpoint.finance;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.ExpenseEnums;
import uz.tune.mentourBiz.rest.payload.req.finance.ReqSchoolExpense;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.finance.ResExpenseSummary;
import uz.tune.mentourBiz.rest.payload.res.finance.ResSchoolExpense;
import uz.tune.mentourBiz.rest.service.finance.SchoolExpenseService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The school expense ledger: what the school spent, on what, and when. */
@RestController
@RequestMapping(BaseURI.API1 + "/finance/expenses")
@RequiredArgsConstructor
public class SchoolExpenseEndpoint {

    private final SchoolExpenseService expenseService;

    /**
     * The expense list, newest first. Every filter is optional; {@code categories} may be repeated or
     * comma-separated. Teacher salaries appear here alongside hand-entered rows.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResSchoolExpense>> list(
            @RequestParam(required = false) List<ExpenseEnums.ExpenseCategory> categories,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(expenseService.list(
                categories, fromDate, toDate, search, PageRequest.of(page, size)));
    }

    /** Totals by category for the window; defaults to the current month. */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResExpenseSummary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(expenseService.summary(fromDate, toDate));
    }

    /** Book an expense. TEACHER_SALARY is rejected — pay the teacher instead and the row writes itself. */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResSchoolExpense> create(@RequestBody ReqSchoolExpense req) {
        return ResponseEntity.ok(expenseService.create(req));
    }

    /** Soft-delete a hand-entered expense; rows written by payroll are refused. */
    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        return ResponseEntity.ok(expenseService.delete(uuid));
    }
}
