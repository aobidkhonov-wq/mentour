package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.FinanceTransaction;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.time.Instant;
import java.util.UUID;

@Data
public class ResFinanceTransaction {
    private UUID uuid;
    private Instant dateTime;
    private String studentName;
    private String studentNickname;
    private ResAttachment attachment;
    private Long amount;
    private FinanceEnums.FinanceTransactionType type;
    private FinanceEnums.PaymentMethod method;
    private String acceptedBy;
    private String note;
    private Boolean isDeleted;


    public ResFinanceTransaction(FinanceTransaction tx) {
        this.uuid = tx.getUuid();
        this.isDeleted = Boolean.TRUE.equals(tx.getDeleted());
        this.dateTime = tx.getCreatedAt();
        this.type = tx.getType();
        this.studentName = tx.getStudent().getUser().getFirstName() + " " + tx.getStudent().getUser().getLastName();
        if(tx.getStudent().getUser().getAttachment() != null){
            this.attachment = new ResAttachment(tx.getStudent().getUser().getAttachment());
        }
        this.studentNickname = tx.getStudent().getUser().getUsername();
        // Always expose a positive magnitude; the sign/direction is conveyed by `type`
        // (CHARGE = money out, PAYMENT = money in). Charges are stored negative in the DB.
        this.amount = tx.getAmount() != null ? Math.abs(tx.getAmount()) : null;
        this.method = tx.getMethod();
        this.acceptedBy = tx.getAcceptedBy() != null ?
                tx.getAcceptedBy().getFirstName() + " " + tx.getAcceptedBy().getLastName() : "System";
        this.note = tx.getNote();
    }
}