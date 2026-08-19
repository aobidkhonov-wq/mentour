package uz.tune.mentourBiz.external.ofbpay;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.config.prop.ExternalProp;
import uz.tune.mentourBiz.rest.enums.Currency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExtOfbPayService {

    private final ExternalProp prop;

    public String getPayFormAction() {
        return prop.getOfbPay().getPayUrl();
    }

    public Map<String, String> buildPaymentFormFields(String orderId, Long amountUzs, String description, Currency currency) {
        long amountInTiyins = amountUzs * 100;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchantId", prop.getOfbPay().getMerchantId());
        fields.put("amount", String.valueOf(amountInTiyins));
        fields.put("description", prop.getOfbPay().getDescription());
        fields.put("callback", prop.getOfbPay().getCallbackUrl());
        fields.put("order_id", orderId);
        fields.put("currency", currency.name());
        return fields;
    }

    public String computeHashKey(String method, String orderId, String transactionId, Long amount) {
        String raw = method + prop.getOfbPay().getSalt() + orderId + transactionId + amount;
        return sha256Hex(raw);
    }

    public boolean verifyHashKey(String method, String orderId, String transactionId, Long amount, String providedHashKey) {
        if (providedHashKey == null) {
            return false;
        }
        String expected = computeHashKey(method, orderId, transactionId, amount);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                providedHashKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            Logger.exception("SHA-256 not available", e);
            throw new IllegalStateException(e);
        }
    }
}
