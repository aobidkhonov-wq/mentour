package uz.tune.mentourBiz.rest.service.payroll;

import uz.tune.mentourBiz.rest.domain.payroll.PayrollEvent;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResPayrollEvent;

import java.time.YearMonth;
import java.util.List;

/** Shared shaping of payroll events, used by both the History feed and the payslip detail tabs. */
final class PayrollEventMapper {

    private PayrollEventMapper() {
    }

    static List<ResPayrollEvent> toResponses(List<PayrollEvent> events) {
        return events.stream().map(PayrollEventMapper::toResponse).toList();
    }

    static ResPayrollEvent toResponse(PayrollEvent event) {
        String teacherName = event.getTeacher() != null
                ? TeacherPayslipService.userName(event.getTeacher().getUser()) : null;
        Student student = event.getStudent();

        return ResPayrollEvent.builder()
                .uuid(event.getUuid())
                .occurredAt(event.getOccurredAt())
                .teacherUuid(event.getTeacher() != null && event.getTeacher().getUser() != null
                        ? event.getTeacher().getUser().getUuid() : null)
                .teacherName(teacherName)
                .teacherInitials(TeacherPayslipService.initials(teacherName))
                .eventType(event.getEventType())
                .title(event.getTitle())
                .subtitle(event.getSubtitle())
                .amount(event.getAmount() != null ? event.getAmount() : 0L)
                .groupUuid(event.getGroup() != null ? event.getGroup().getUuid() : null)
                .groupName(event.getGroup() != null ? event.getGroup().getName() : null)
                .studentUuid(student != null && student.getUser() != null ? student.getUser().getUuid() : null)
                .studentName(student != null ? TeacherPayslipService.userName(student.getUser()) : null)
                .studentCount(event.getStudentCount())
                .note(event.getNote())
                .period(event.getPeriodYear() != null && event.getPeriodMonth() != null
                        ? YearMonth.of(event.getPeriodYear(), event.getPeriodMonth()).toString() : null)
                .addedByName(TeacherPayslipService.userName(event.getAddedBy()))
                .systemGenerated(event.getAddedBy() == null)
                .build();
    }
}
