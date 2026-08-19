package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResPaymentInitiation {

    private String payUrl;

    private String formAction;

    private Map<String, String> formFields;

    public static ResPaymentInitiation ofPayUrl(String payUrl) {
        return new ResPaymentInitiation(payUrl, null, null);
    }

    public static ResPaymentInitiation ofForm(String formAction, Map<String, String> formFields) {
        return new ResPaymentInitiation(null, formAction, formFields);
    }
}
