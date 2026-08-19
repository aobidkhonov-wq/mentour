package uz.tune.mentourBiz.rest.endpoint.users;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.student.ReqStudentDiscount;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.student.ResStudentDiscount;
import uz.tune.mentourBiz.rest.service.student.StudentDiscountService;

import java.util.List;
import java.util.UUID;

/**
 * Per-student discounts: a fixed som amount and/or a percentage off the student's billing-plan charges,
 * for a set number of months or permanently. Optional — a student without one pays the full price, and
 * they may hold one discount of each type at a time. Discounts never reach teacher payroll, which
 * always settles on the undiscounted price.
 */
@RestController
@RequestMapping(BaseURI.API1 + "/student-discounts")
@RequiredArgsConstructor
public class StudentDiscountEndpoint {

    private final StudentDiscountService studentDiscountService;

    /** Every discount ever granted to the student, newest first. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<List<ResStudentDiscount>> list(@RequestParam UUID studentUuid) {
        return ResponseEntity.ok(studentDiscountService.listForStudent(studentUuid));
    }

    /** The discounts in force today — one per type, so 0, 1 or 2 of them. */
    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<List<ResStudentDiscount>> active(@RequestParam UUID studentUuid) {
        return ResponseEntity.ok(studentDiscountService.getActive(studentUuid));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResStudentDiscount> create(@RequestBody ReqStudentDiscount req) {
        return ResponseEntity.ok(studentDiscountService.create(req));
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResStudentDiscount> update(@PathVariable UUID uuid, @RequestBody ReqStudentDiscount req) {
        return ResponseEntity.ok(studentDiscountService.update(uuid, req));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN','SCHOOL_DIRECTOR')")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        return ResponseEntity.ok(studentDiscountService.delete(uuid));
    }
}
