package uz.tune.mentourBiz.external.uzum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AcquiringCallbackData — payload Uzum Checkout POSTs to the merchant callback URL
 * (the URL is configured on the terminal by Uzum). Carries no amount by design.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UzumCallbackData {
    // order id on the Uzum Checkout side (matches uzumTransactionId)
    private String orderId;
    // SUCCESS | FAIL
    private String operationState;
    // AUTHORIZE | COMPLETE | REFUND | REVERSE | TOP_UP_COMPLETED
    private String operationType;
    // operation id on the merchant side, echoed back if it was sent
    private String merchantOperationId;
    // order id on the merchant side (our shopTransactionId)
    private String orderNumber;
    // unique id of the bank operation
    private String rrn;
    // id of the card binding created during the operation
    private String bindingId;
}
