package uz.tune.mentourBiz.external.uzum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.config.prop.ExternalProp;
import uz.tune.mentourBiz.rest.enums.Currency;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uzum Checkout integration (OpenAPI v1.10.x, https://www.inplat-tech.ru/docs/checkout/).
 * <p>
 * Auth: X-Terminal-Id + X-API-Key headers; mTLS is mandatory on the transport level and is
 * configured on the RestTemplate/JVM keystore once Uzum issues the client certificate.
 * Every response uses the envelope {errorCode, message, result} where errorCode 0 = success.
 */
@Service
@RequiredArgsConstructor
public class ExtUzumService {

    private static final int UZS_ISO_NUMERIC = 860;
    private static final int DEFAULT_SESSION_TIMEOUT_SECS = 1800;

    private final RestTemplate restTemplate;
    private final ExternalProp prop;

    /**
     * Amount Uzum actually charges, in tiyin. USD plans are settled in UZS through the configured
     * course, so the webhook must confirm the payment against this very same value — both the
     * register call and the callback cross-check go through this method.
     */
    public long toChargedAmount(Long amount, Currency currency) {
        long amountUzs = (currency == Currency.USD) ? amount * prop.getUzum().getCourse() : amount;
        return amountUzs * 100L;
    }

    public UzumInvoiceResult createSubscriptionInvoice(String orderNumber, String clientId, Long amount, Currency currency) {
        try {
            ExternalProp.Uzum uzum = prop.getUzum();
            long amountLong = toChargedAmount(amount, currency);

            Map<String, Object> paymentParams = new HashMap<>();
            paymentParams.put("operationType", "PAYMENT");
            paymentParams.put("payType", "ONE_STEP");

            Map<String, Object> request = new HashMap<>();
            request.put("viewType", "REDIRECT");
            request.put("clientId", clientId);
            request.put("currency", UZS_ISO_NUMERIC); // settlement is always UZS, USD plans are converted above
            request.put("orderNumber", orderNumber);
            request.put("sessionTimeoutSecs",
                    uzum.getSessionTimeoutSecs() != null ? uzum.getSessionTimeoutSecs() : DEFAULT_SESSION_TIMEOUT_SECS);
            request.put("amount", amountLong); // minimal currency units (tiyin)
            request.put("paymentParams", paymentParams);
            request.put("merchantParams", Map.of("cart", buildFiscalizationCart(uzum, orderNumber, prop.getUzum().getDescription(), amountLong)));
            if (CoreUtils.isPresent(uzum.getSuccessUrl())) {
                request.put("successUrl", uzum.getSuccessUrl());
            }
            if (CoreUtils.isPresent(uzum.getFailureUrl())) {
                request.put("failureUrl", uzum.getFailureUrl());
            }

            Logger.logInfo(">> [ExtUzumService] Registering Uzum payment for order: " + orderNumber);

            ResponseEntity<Map> response = restTemplate.exchange(
                    uzum.getApiUrl() + "/api/v1/payment/register",
                    HttpMethod.POST,
                    new HttpEntity<>(request, getHeaders()),
                    Map.class
            );

            Map<String, Object> result = extractResult(response, "register");
            if (result == null) return null;

            String payLink = asString(result.get("paymentRedirectUrl"));
            String uzumOrderId = asString(result.get("orderId"));
            if (CoreUtils.isEmpty(payLink)) {
                Logger.logWarn("Uzum register response had no paymentRedirectUrl: " + result);
                return null;
            }
            return new UzumInvoiceResult(payLink, uzumOrderId);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            Logger.logWarn("Uzum API Error: " + e.getResponseBodyAsString());
            return null;
        } catch (Throwable th) {
            Logger.exception("Uzum invoice create critical failure", th);
            return null;
        }
    }

    /**
     * POST /api/v1/payment/getOrderStatus — the acquiring callback carries no amount,
     * so the webhook confirms every payment server-to-server through this method.
     */
    public UzumOrderStatus getOrderStatus(String uzumOrderId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("orderId", uzumOrderId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    prop.getUzum().getApiUrl() + "/api/v1/payment/getOrderStatus",
                    HttpMethod.POST,
                    new HttpEntity<>(request, getHeaders()),
                    Map.class
            );

            Map<String, Object> result = extractResult(response, "getOrderStatus");
            if (result == null) return null;

            UzumOrderStatus status = new UzumOrderStatus();
            status.setOrderId(asString(result.get("orderId")));
            status.setStatus(asString(result.get("status")));
            status.setMerchantOrderId(asString(result.get("merchantOrderId")));
            status.setAmount(asLong(result.get("amount")));
            status.setTotalAmount(asLong(result.get("totalAmount")));
            status.setCompletedAmount(asLong(result.get("completedAmount")));
            return status;

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            Logger.logWarn("Uzum getOrderStatus API Error: " + e.getResponseBodyAsString());
            return null;
        } catch (Throwable th) {
            Logger.exception("Uzum getOrderStatus critical failure", th);
            return null;
        }
    }

    /**
     * merchantParams.cart for AUTOFISCALIZATION terminals (error 3045 without it).
     * The whole subscription goes as a single cart item whose total must equal the
     * payment amount; receiptParams carries the tax data (IKPU/packageCode from
     * tasnif.soliq.uz, VAT rate, INN or PINFL — exactly one of the two).
     */
    private Map<String, Object> buildFiscalizationCart(ExternalProp.Uzum uzum, String orderNumber, String itemTitle, long totalTiyin) {
        Map<String, Object> receiptParams = new HashMap<>();
        receiptParams.put("spic", uzum.getSpic());
        receiptParams.put("packageCode", uzum.getPackageCode());
        receiptParams.put("vatPercent", uzum.getVatPercent() != null ? uzum.getVatPercent() : 0);
        if (CoreUtils.isPresent(uzum.getTin())) {
            receiptParams.put("TIN", uzum.getTin());
        } else if (CoreUtils.isPresent(uzum.getPinfl())) {
            receiptParams.put("PINFL", uzum.getPinfl());
        }

        String title = CoreUtils.isPresent(itemTitle) ? itemTitle : "Mentour Biz subscription";
        if (title.length() > 63) title = title.substring(0, 63);

        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("productId", "school-subscription");
        item.put("quantity", 1);
        item.put("unitPrice", totalTiyin);
        item.put("total", totalTiyin);
        item.put("receiptParams", receiptParams);

        Map<String, Object> cart = new HashMap<>();
        cart.put("cartId", orderNumber);
        cart.put("receiptType", "PURCHASE");
        cart.put("items", List.of(item));
        cart.put("total", totalTiyin);
        return cart;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResult(ResponseEntity<Map> response, String operation) {
        if (!response.getStatusCode().is2xxSuccessful() || CoreUtils.isEmpty(response.getBody())) {
            Logger.logWarn("Uzum " + operation + " failed: " + response.getStatusCode());
            return null;
        }
        Map<String, Object> body = response.getBody();
        Long errorCode = asLong(body.get("errorCode"));
        if (errorCode == null || errorCode != 0L) {
            Logger.logWarn("Uzum " + operation + " error " + errorCode + ": " + body.get("message"));
            return null;
        }
        Object result = body.get("result");
        if (!(result instanceof Map)) {
            Logger.logWarn("Uzum " + operation + " response had no result: " + body);
            return null;
        }
        return (Map<String, Object>) result;
    }

    private HttpHeaders getHeaders() {
        ExternalProp.Uzum uzum = prop.getUzum();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Content-Language", "uz-UZ");
        headers.set("X-Terminal-Id", uzum.getTerminalId());
        if (CoreUtils.isPresent(uzum.getApiKey())) {
            headers.set("X-API-Key", uzum.getApiKey());
        }
        return headers;
    }

    private String asString(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
