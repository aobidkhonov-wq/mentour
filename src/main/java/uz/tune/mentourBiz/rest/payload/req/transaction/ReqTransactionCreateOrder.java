package uz.tune.mentourBiz.rest.payload.req.transaction;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqTransactionCreateOrder {

    private String transactionId;

    private Long amount;

    private UUID courseId;

    private Integer lessonCount;
}
