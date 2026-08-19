package uz.tune.mentourBiz.rest.endpoint.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqGeneratePayslips;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqPayslipPayment;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollEvent;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollOverview;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayslipDetail;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayslipSummary;
import uz.tune.mentourBiz.rest.service.payroll.TeacherPayslipService;

import java.util.List;
import java.util.UUID;

/** The payroll Overview screen: monthly totals, the teacher list and the paycheck detail panel. */
@RestController
@RequestMapping(BaseURI.API1 + "/payroll/payslips")
@RequiredArgsConstructor
public class TeacherPayslipEndpoint {

    private final TeacherPayslipService payslipService;

    /** KPI cards for a month. {@code year}/{@code month} default to the current month. */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResPayrollOverview> overview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(payslipService.overview(year, month));
    }

    /** The Teachers Payroll List, highest paid first. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResPayslipSummary>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) PayrollEnums.PayslipStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(payslipService.list(year, month, status, search, PageRequest.of(page, size)));
    }

    /** Everything behind the Paycheck Details panel and its tabs, bar the History tab. */
    @GetMapping("/{payslipUuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResPayslipDetail> detail(@PathVariable UUID payslipUuid) {
        return ResponseEntity.ok(payslipService.detail(payslipUuid));
    }

    /**
     * The History tab: this payslip's payroll events, newest first. {@code eventTypes} narrows the feed
     * (repeat the parameter or comma-separate); leaving it off returns every type. The teacher and the
     * period come from the payslip — use {@code /payroll/events} for a feed that spans teachers.
     */
    @GetMapping("/{payslipUuid}/history")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResPayrollEvent>> history(
            @PathVariable UUID payslipUuid,
            @RequestParam(required = false) List<PayrollEnums.PayrollEventType> eventTypes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(payslipService.history(payslipUuid, eventTypes, PageRequest.of(page, size)));
    }

    /**
     * "Generate Paychecks". Creates a draft payslip for every teacher in scope who does not have one
     * for the month; pass {@code regenerateDrafts} to also refresh existing drafts.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> generate(@RequestBody(required = false) ReqGeneratePayslips req) {
        return ResponseEntity.ok(payslipService.generate(req));
    }

    @PostMapping("/{payslipUuid}/submit")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResPayslipDetail> submit(@PathVariable UUID payslipUuid) {
        return ResponseEntity.ok(payslipService.submitForApproval(payslipUuid));
    }

    /** Sign off the figures; from here on they are frozen. */
    @PostMapping("/{payslipUuid}/approve")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResPayslipDetail> approve(@PathVariable UUID payslipUuid) {
        return ResponseEntity.ok(payslipService.approve(payslipUuid));
    }

    /** Record that the money has gone out. Only allowed once the payslip is approved. */
    @PostMapping("/{payslipUuid}/pay")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResPayslipDetail> markPaid(
            @PathVariable UUID payslipUuid,
            @RequestBody(required = false) ReqPayslipPayment req) {
        return ResponseEntity.ok(payslipService.markPaid(payslipUuid, req));
    }

    /** Send an approved payslip back to draft when a mistake is spotted before payment. */
    @PostMapping("/{payslipUuid}/reopen")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResPayslipDetail> reopen(@PathVariable UUID payslipUuid) {
        return ResponseEntity.ok(payslipService.reopen(payslipUuid));
    }
}
