package uz.tune.mentourBiz.external.uzum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subset of the /api/v1/payment/getOrderStatus result used to confirm callbacks.
 * Statuses: REGISTERED, AUTHORIZED, COMPLETED, TOP_UP_COMPLETED, REFUNDED, REVERSED, DECLINED.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UzumOrderStatus {
    // Uzum-side order id
    private String orderId;
    // MerchantPaymentStatus enum value
    private String status;
    // our shopTransactionId as registered
    private String merchantOrderId;
    // amount sent at registration, in minimal currency units (tiyin)
    private Long amount;
    // final payment amount in minimal currency units (tiyin) — may differ from the registered amount
    private Long totalAmount;
    // amount actually captured, in minimal currency units (tiyin)
    private Long completedAmount;
}
