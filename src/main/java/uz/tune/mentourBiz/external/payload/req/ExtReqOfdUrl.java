package uz.tune.mentourBiz.external.payload.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtReqOfdUrl {
    private String transactionId;
    private String merchantId;
    @JsonProperty("ofdUrl")
    private String ofdUrl;
}
