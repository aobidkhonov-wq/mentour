package uz.tune.mentourBiz.external.uzum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UzumInvoiceResult {
    // URL the user is redirected to in order to pay
    private String payLink;
    // Uzum-side transaction / payment id (stored for reconciliation)
    private String transactionId;
}
