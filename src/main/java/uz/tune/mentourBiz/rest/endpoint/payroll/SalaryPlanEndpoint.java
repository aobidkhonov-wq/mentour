package uz.tune.mentourBiz.rest.endpoint.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqSalaryPlan;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResSalaryPlan;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResSalaryPlansOverview;
import uz.tune.mentourBiz.rest.service.payroll.SalaryPlanService;

import java.util.List;
import java.util.UUID;

/** The Plans screen: reusable salary schemes and the teachers assigned to them. */
@RestController
@RequestMapping(BaseURI.API1 + "/salary-plans")
@RequiredArgsConstructor
public class SalaryPlanEndpoint {

    private final SalaryPlanService salaryPlanService;

    /** KPI cards above the plan list. */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResSalaryPlansOverview> overview() {
        return ResponseEntity.ok(salaryPlanService.overview());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<Page<ResSalaryPlan>> list(
            @RequestParam(required = false) PayrollEnums.SalaryPlanStatus status,
            @RequestParam(required = false) PayrollEnums.SalaryPlanType planType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(salaryPlanService.list(status, planType, search, PageRequest.of(page, size)));
    }

    /** Full plan with its earning structure, bonuses and deductions. */
    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResSalaryPlan> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(salaryPlanService.get(uuid));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResSalaryPlan> create(@RequestBody ReqSalaryPlan req) {
        return ResponseEntity.ok(salaryPlanService.create(req));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResSalaryPlan> update(@PathVariable UUID uuid, @RequestBody ReqSalaryPlan req) {
        return ResponseEntity.ok(salaryPlanService.update(uuid, req));
    }

    @PostMapping("/{uuid}/duplicate")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResSalaryPlan> duplicate(@PathVariable UUID uuid) {
        return ResponseEntity.ok(salaryPlanService.duplicate(uuid));
    }

    /** Stop the plan being handed out. Teachers already on it keep the salary they were given. */
    @PostMapping("/{uuid}/archive")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> archive(@PathVariable UUID uuid) {
        return ResponseEntity.ok(salaryPlanService.archive(uuid));
    }

    /** The "Assigned Teachers" tab. */
    @GetMapping("/{uuid}/teachers")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<List<SalaryPlanService.AssignedTeacher>> teachers(@PathVariable UUID uuid) {
        return ResponseEntity.ok(salaryPlanService.assignedTeachers(uuid));
    }

    /** Put teachers on the plan, replacing whatever they had configured personally. */
    @PostMapping("/{uuid}/teachers")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> assign(
            @PathVariable UUID uuid,
            @RequestBody List<UUID> teacherUuids) {
        return ResponseEntity.ok(salaryPlanService.assignTeachers(uuid, teacherUuids));
    }
}
