package uz.tune.mentourBiz.rest.endpoint.school.group;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqTeacherSalaryPlan;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherPayroll;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherSalaryPlan;
import uz.tune.mentourBiz.rest.service.TeacherPayrollService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/teacher-salary")
@RequiredArgsConstructor
public class TeacherPayrollEndpoint {

    private final TeacherPayrollService teacherPayrollService;

    /**
     * Set up a teacher's salary plan for the first time: a fixed monthly base, a percent of each group's
     * revenue, and a fixed amount per student. All three are optional and default to 0. A teacher may
     * only have one plan — if they already have one this is rejected, and the update call is used
     * instead. Returns the plan as saved.
     */
    @PostMapping("/plan")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResTeacherSalaryPlan> createPlan(@RequestBody ReqTeacherSalaryPlan req) {
        return ResponseEntity.ok(teacherPayrollService.createSalaryPlan(req));
    }

    /**
     * The teacher's stored salary plan. Teachers without one get a zeroed plan back with
     * {@code configured = false}.
     */
    @GetMapping("/{teacherUuid}/plan")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResTeacherSalaryPlan> getPlan(@PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(teacherPayrollService.getSalaryPlan(teacherUuid));
    }

    /**
     * Every configured salary plan the caller may see. {@code schoolUuid} narrows the result for admins
     * who cover more than one school; {@code activeOnly} hides plans switched off with
     * {@code isActive = false}. Teachers who were never configured are not listed.
     */
    @GetMapping("/plans")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<List<ResTeacherSalaryPlan>> listPlans(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(teacherPayrollService.listSalaryPlans(schoolUuid, activeOnly));
    }

    /**
     * Overwrite an existing plan. Same body as the setup call — omitted amounts are stored as 0 — but
     * the teacher is taken from the path and a teacher with no plan yet is rejected.
     */
    @PutMapping("/{teacherUuid}/plan")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResTeacherSalaryPlan> updatePlan(
            @PathVariable UUID teacherUuid,
            @RequestBody ReqTeacherSalaryPlan req) {
        return ResponseEntity.ok(teacherPayrollService.updateSalaryPlan(teacherUuid, req));
    }

    /** Remove the plan and its group overrides outright. */
    @DeleteMapping("/{teacherUuid}/plan")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> deletePlan(@PathVariable UUID teacherUuid) {
        return ResponseEntity.ok(teacherPayrollService.deleteSalaryPlan(teacherUuid));
    }

    /**
     * Monthly payroll for a teacher: per-group billed/collected revenue and salary, plus totals.
     * {@code year}/{@code month} default to the current month.
     */
    @GetMapping("/{teacherUuid}/payroll")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResTeacherPayroll> payroll(
            @PathVariable UUID teacherUuid,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(teacherPayrollService.computeMonthly(teacherUuid, year, month));
    }
}
