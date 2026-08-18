package uz.tune.mentourBiz.rest.endpoint.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqTeacherPayment;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResBalanceEntry;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResTeacherBalance;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResTeacherBalanceRow;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResTeacherPayment;
import uz.tune.mentourBiz.rest.service.payroll.TeacherBalanceService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Teacher balances and the payments made against them.
 *
 * <p>Admin-only throughout: a teacher cannot see their own balance here, by design.
 */
@RestController
@RequestMapping(BaseURI.API1 + "/payroll/balances")
@RequiredArgsConstructor
public class TeacherBalanceEndpoint {

    private final TeacherBalanceService balanceService;

    /** Every teacher in scope with what they are owed, largest balance first. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResTeacherBalanceRow>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(balanceService.list(search, PageRequest.of(page, size)));
    }

    /** One teacher's balance and the months still owing behind it. */
    @GetMapping("/{teacherUuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResTeacherBalance> detail(@PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(balanceService.detail(teacherUuid));
    }

    /** The balance history: accruals, payments and withdrawn approvals, newest first. */
    @GetMapping("/{teacherUuid}/history")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResBalanceEntry>> history(
            @PathVariable UUID teacherUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(balanceService.history(teacherUuid, PageRequest.of(page, size)));
    }

    /**
     * Pay a teacher out of their balance — an advance mid-month or the final settlement. The amount is
     * applied to the oldest month still owing and may not exceed the balance; send
     * {@code payFullBalance} instead of an amount to clear everything.
     */
    @PostMapping("/{teacherUuid}/payments")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResTeacherPayment> pay(
            @PathVariable UUID teacherUuid,
            @RequestBody ReqTeacherPayment req) {
        return ResponseEntity.ok(balanceService.pay(teacherUuid, req));
    }

    /** The payments feed. Leave {@code teacherUuid} off for every teacher in scope. */
    @GetMapping("/payments")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResTeacherPayment>> payments(
            @RequestParam(required = false) UUID teacherUuid,
            @RequestParam(required = false) PayrollEnums.TeacherPaymentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(balanceService.payments(
                teacherUuid, type, fromDate, toDate, PageRequest.of(page, size)));
    }

    /** Undo a payment: the amount goes back on the balance and the months it settled reopen. */
    @PostMapping("/payments/{paymentUuid}/reverse")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> reverse(@PathVariable UUID paymentUuid) {
        return ResponseEntity.ok(balanceService.reversePayment(paymentUuid));
    }
}
