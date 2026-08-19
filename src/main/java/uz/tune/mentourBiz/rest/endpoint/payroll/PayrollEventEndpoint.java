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
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqPayrollEvent;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollEvent;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollEventsOverview;
import uz.tune.mentourBiz.rest.service.payroll.PayrollEventService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The History screen: every payroll event, filtered the way the sidebar filters it. */
@RestController
@RequestMapping(BaseURI.API1 + "/payroll/events")
@RequiredArgsConstructor
public class PayrollEventEndpoint {

    private final PayrollEventService payrollEventService;

    /** KPI cards and the event-type legend, optionally narrowed to one period. */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResPayrollEventsOverview> overview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(payrollEventService.overview(year, month));
    }

    /**
     * The feed, newest first. {@code fromDate}/{@code toDate} are inclusive calendar dates; the pay
     * period filter is {@code year}/{@code month}.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResPayrollEvent>> list(
            @RequestParam(required = false) List<UUID> teacherUuids,
            @RequestParam(required = false) List<PayrollEnums.PayrollEventType> eventTypes,
            @RequestParam(required = false) List<UUID> groupUuids,
            @RequestParam(required = false) UUID studentUuid,
            @RequestParam(required = false) UUID addedByUuid,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(payrollEventService.list(
                teacherUuids, eventTypes, groupUuids, studentUuid, addedByUuid,
                year, month, fromDate, toDate, search, PageRequest.of(page, size)));
    }

    /**
     * Book a bonus, deduction or correction by hand. It lands on the teacher's payslip the next time
     * that month's payslip is generated or refreshed.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResPayrollEvent> create(@RequestBody ReqPayrollEvent req) {
        return ResponseEntity.ok(payrollEventService.create(req));
    }

    /** Remove a manually booked event, as long as its payslip is still a draft. */
    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        return ResponseEntity.ok(payrollEventService.delete(uuid));
    }
}
