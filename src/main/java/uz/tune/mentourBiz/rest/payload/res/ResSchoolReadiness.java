package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

@Data
@AllArgsConstructor
public class ResSchoolReadiness {
    private ResSchoolInfo schoolInfo;

    // Payment stats
    private boolean isPaymentActive;
    private long packageCount;
    private String paymentStatusNote; // "Inactive & No Packages"

    // Module counts
    private long libraryItemCount;
    private long shopItemCount;

    // Total setup score for sorting
    private long totalSetupScore;
}
