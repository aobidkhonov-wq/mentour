package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** One line of the balances screen: every teacher and what they are still owed. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResTeacherBalanceRow {

    private UUID teacherUuid;
    private String teacherName;
    private String teacherInitials;

    private Long balance;

    // How many months are still open behind that figure — 2 means a remainder was carried over.
    private Long openPeriodCount;
}
