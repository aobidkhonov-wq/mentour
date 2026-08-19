package uz.tune.mentourBiz.rest.payload.res.shop;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.CoinTransaction;
import uz.tune.mentourBiz.rest.enums.TransactionType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ResCoinTransaction {
    private UUID uuid;
    private String studentName;
    private Integer amount;
    private TransactionType type;
    private String reason;
    private String givenBy;
    private Instant createdAt;

    public ResCoinTransaction(CoinTransaction ct) {
        this.uuid = ct.getUuid();
        this.studentName = ct.getStudent().getUser().getFirstName() + " " + ct.getStudent().getUser().getLastName();
        this.amount = ct.getAmount();
        this.type = ct.getType();
        this.reason = ct.getReason();
        this.givenBy = ct.getGivenBy();
        this.createdAt = ct.getCreatedAt();
    }
}