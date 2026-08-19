package uz.tune.mentourBiz.rest.payload.res.payments;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.SchoolPayoutAccount;

@Getter
@Setter
public class ResPayoutDetails {
    private String recipientFullName;
    private String recipientAccount;
    private String recipientPinfl;
    private String bankMfo;

    public ResPayoutDetails(SchoolPayoutAccount account) {
        this.recipientFullName = account.getRecipientFullName();
        this.recipientAccount = account.getRecipientAccount();
        this.recipientPinfl = account.getRecipientPinfl();
        this.bankMfo = account.getBankMfo();
    }
}