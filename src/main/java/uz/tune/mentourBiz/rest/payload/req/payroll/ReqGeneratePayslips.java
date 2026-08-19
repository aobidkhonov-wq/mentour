package uz.tune.mentourBiz.rest.payload.req.payroll;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * "Generate Paychecks" for a month. By default it only creates payslips for teachers who do not have
 * one yet, so pressing the button twice is harmless.
 */
@Getter
@Setter
public class ReqGeneratePayslips {

    private Integer year;
    private Integer month;

    // SYS_ADMIN may target a school; a school admin always gets their own.
    private UUID schoolUuid;

    // Limit generation to these teachers. Empty or null means every teacher in scope.
    private List<UUID> teacherUuids;

    // Rebuild payslips that already exist. Only ever touches DRAFT ones — an approved or paid payslip
    // is a record of what was handed over and is never regenerated.
    private Boolean regenerateDrafts;
}
