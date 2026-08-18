package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.util.UUID;

/** One movement on the balance history: a month credited, a payment made, an approval taken back. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResBalanceEntry {

    private UUID uuid;
    private PayrollEnums.BalanceEntryType entryType;

    // Signed: positive credits the teacher, negative pays them down.
    private Long amount;

    private String title;
    private String note;

    private UUID payslipUuid;
    private String period;              // "2026-08", when the entry came from a payslip
    private UUID paymentUuid;

    private Instant occurredAt;

    // Null for entries payroll wrote itself.
    private String createdByName;
}
