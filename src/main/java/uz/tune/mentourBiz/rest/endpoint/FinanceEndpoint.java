package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.ReqBulkInitialCharge;
import uz.tune.mentourBiz.rest.payload.req.ReqBulkTopUp;
import uz.tune.mentourBiz.rest.payload.res.ResFinanceHistoryWrapper;
import uz.tune.mentourBiz.rest.payload.res.ResFinanceSummary;
import uz.tune.mentourBiz.rest.payload.res.ResStudentFinanceDashboard;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.service.FinanceService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.FINANCE)
@RequiredArgsConstructor
public class FinanceEndpoint {

    private final FinanceService financeService;
    private final UserService userService;
    private final SchoolDirectorRepo schoolDirectorRepo;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DASHBOARD_GET)")
    public ResponseEntity<Page<ResStudentFinanceDashboard>> getDashboard(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam(required = false) UUID courseUuid,
            @RequestParam(required = false) UUID packageUuid,
            @RequestParam(required = false) UUID groupUuid,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) UUID teacherUuid,
            @RequestParam(required = false) FinanceEnums.FinanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "false") boolean onlyOverdue,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 1000;
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(financeService.getFinanceDashboard(
                schoolUuid, courseUuid, studentName, teacherUuid, status, fromDate, toDate, packageUuid, groupUuid, activeOnly, onlyOverdue, pageable));
    }

//    @GetMapping("/organization/summary")
//    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ORG_SUMMARY_GET)")
//    public ResponseEntity<ResOrganizationFinanceSummary> getOrgSummary(
//            @RequestParam(required = false) UUID organizationUuid,
//            @RequestParam(required = false) UUID groupUuid,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
//            @RequestParam(defaultValue = "true") boolean activeOnly) {
//        return ResponseEntity.ok(financeService.getOrganizationFinanceSummary(organizationUuid, groupUuid, fromDate, toDate, activeOnly));
//    }

//    @GetMapping("/organization/dashboard")
//    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ORG_DASHBOARD_GET)")
//    public ResponseEntity<Page<ResStudentFinanceList>> getOrgDashboard(
//            @RequestParam(required = false) UUID organizationUuid,
//            @RequestParam(required = false) FinanceEnums.FinanceStatus status,
//            @RequestParam(defaultValue = "true") boolean activeOnly,
//            @PageableDefault(sort = "id") Pageable pageable) {
//
//        User user = userService.getCurrentUser();
//        UUID targetOrgUuid;
//        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
//            targetOrgUuid = organizationUuid;
//        } else {
//            targetOrgUuid = schoolDirectorRepo.findByUser(user)
//                    .orElseThrow(() -> new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey()))
//                    .getOrganization().getUuid();
//        }
//
//        return ResponseEntity.ok(financeService.getOrgFinanceDashboard(targetOrgUuid, status, activeOnly, pageable));
//    }

    @PostMapping("/organization/activate")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ORG_ACTIVATE)")
    public ResponseEntity<Boolean> activateOrg(@RequestParam(required = false) UUID organizationUuid) {
        User user = userService.getCurrentUser();
        UUID targetOrgUuid;
        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            targetOrgUuid = organizationUuid;
        } else {
            targetOrgUuid = schoolDirectorRepo.findByUser(user)
                    .orElseThrow(() -> new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey()))
                    .getOrganization().getUuid();
        }
        return ResponseEntity.ok(financeService.activate(targetOrgUuid));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SUMMARY_GET)")
    public ResponseEntity<ResFinanceSummary> getSummary(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam(required = false) UUID groupUuid,
            @RequestParam(required = false) UUID courseUuid,
            @RequestParam(required = false) UUID teacherUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(financeService.getFinanceSummary(schoolUuid, groupUuid, courseUuid, teacherUuid, fromDate, toDate, activeOnly));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).HISTORY_GET)")
    public ResponseEntity<ResFinanceHistoryWrapper> getHistory(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam(required = false) FinanceEnums.PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(financeService.getFinanceHistory(schoolUuid, method, fromDate, toDate, pageable));
    }

    @GetMapping("/history/charges")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).HISTORY_GET)")
    public ResponseEntity<ResFinanceHistoryWrapper> getChargeHistory(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam(required = false) FinanceEnums.FinanceTransactionType type,
            @RequestParam(required = false) FinanceEnums.PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        Instant from = (fromDate != null) ? fromDate.atStartOfDay(zone).toInstant() : null;
        Instant to = (toDate != null) ? toDate.atTime(23, 59, 59, 999_999_999).atZone(zone).toInstant() : null;
        return ResponseEntity.ok(financeService.getHistoryByType(schoolUuid, type, method, from, to, pageable));
    }

    // Soft-deletes a transaction shown in the history lists. Reverses its balance effect on the student
    // and re-attributes acceptedBy to the caller. The uuid is the one returned by the history endpoints.
    @DeleteMapping("/transactions/{transactionUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).FINANCE_TRANSACTION_DELETE)")
    public ResponseEntity<ResponseMessage> deleteTransaction(@PathVariable UUID transactionUuid) {
        return ResponseEntity.ok(financeService.deleteTransaction(transactionUuid));
    }

    @PostMapping("/top-up/{studentUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).TOP_UP)")
    public ResponseEntity<ResponseMessage> topUp(
            @PathVariable UUID studentUuid,
            @RequestParam Long amount,
            @RequestParam FinanceEnums.PaymentMethod method,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]") LocalDateTime paymentDate,
            @RequestParam(required = false) UUID groupUuid) {
        return ResponseEntity.ok(financeService.topUp(studentUuid, amount, method, note, paymentDate, groupUuid));
    }

    @PostMapping("/top-up/bulk")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BULK_TOP_UP)")
    public ResponseEntity<ResponseMessage> bulkTopUp(@RequestBody ReqBulkTopUp request) {
        return ResponseEntity.ok(financeService.bulkTopUp(request));
    }

    @PostMapping("/initial-charge/bulk")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BULK_INITIAL_CHARGE)")
    public ResponseEntity<ResponseMessage> bulkInitialCharge(@RequestBody ReqBulkInitialCharge request) {
        return ResponseEntity.ok(financeService.bulkInitialCharge(request));
    }

    @PostMapping("/initial-charge/{studentUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).APPLY_MANUAL_CHARGE)")
    public ResponseEntity<ResponseMessage> applyManualCharge(
            @PathVariable UUID studentUuid,
            @RequestParam Long amount,
            @RequestParam String note,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]") LocalDateTime paymentDate) {
        return ResponseEntity.ok(financeService.applyInitialCharge(studentUuid, amount, note, paymentDate));
    }

    @PostMapping("/deactivate-payments/bulk")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BATCH_DEACTIVATE)")
    public ResponseEntity<ResponseMessage> batchDeactivate(@RequestBody List<UUID> studentUuids) {
        return ResponseEntity.ok(financeService.batchDeactivateStudentPayments(studentUuids));
    }
}