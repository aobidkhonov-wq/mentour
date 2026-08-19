package uz.tune.mentourBiz.external.payload.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtReqInvoiceTransaction {

    private String actionId;

    private String transactionId;

    private Integer lessonCount;

    private String teacherFio;

    private String teacherAccount;

    private String teacherContractNumber;

    private String teacherPinfl;

    private String teacherMfo;

    private String description;
}
