package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.util.UUID;

/** One row of the History feed. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResPayrollEvent {

    private UUID uuid;
    private Instant occurredAt;

    private UUID teacherUuid;
    private String teacherName;
    private String teacherInitials;

    private PayrollEnums.PayrollEventType eventType;

    // "Lesson completed" / "Student Retention Bonus".
    private String title;
    // "General English B1" / "2 missing reports".
    private String subtitle;

    // Signed: positive earns, negative withholds.
    private Long amount;

    // The "Related to" column.
    private UUID groupUuid;
    private String groupName;
    private UUID studentUuid;
    private String studentName;
    private Long studentCount;
    private String note;

    private String period;   // "2026-06"

    // Who put the row there: the admin's name, or null when payroll generated it ("System").
    private String addedByName;
    private Boolean systemGenerated;
}
