package uz.tune.mentourBiz.external.payload.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtReqInvoicePay {

    private String merchantId;

    private List<ExtReqInvoiceTransaction> transactions;
}
