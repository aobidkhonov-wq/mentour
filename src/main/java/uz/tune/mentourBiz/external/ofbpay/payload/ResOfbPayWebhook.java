package uz.tune.mentourBiz.external.ofbpay.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResOfbPayWebhook {

    private boolean status;

    private String message;

    public static ResOfbPayWebhook success() {
        return new ResOfbPayWebhook(true, "success");
    }

    public static ResOfbPayWebhook failure(String message) {
        return new ResOfbPayWebhook(false, message);
    }
}
