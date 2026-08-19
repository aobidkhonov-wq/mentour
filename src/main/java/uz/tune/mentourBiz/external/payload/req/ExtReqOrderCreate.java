package uz.tune.mentourBiz.external.payload.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtReqOrderCreate {

    private String orderId;

    private String merchantId;

    private Long amount;

    private ExtReqParams params;
}
