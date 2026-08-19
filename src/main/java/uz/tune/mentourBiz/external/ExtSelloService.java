package uz.tune.mentourBiz.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.config.prop.ExternalProp;
import uz.tune.mentourBiz.exception.ExternalServiceException;
import uz.tune.mentourBiz.external.payload.model.ResInvoicePayModel;
import uz.tune.mentourBiz.external.payload.req.*;
import uz.tune.mentourBiz.external.payload.res.ExtResBase;
import uz.tune.mentourBiz.external.payload.res.ExtResInvoicePay;
import uz.tune.mentourBiz.external.payload.res.ExtResOrderCreate;
import uz.tune.mentourBiz.external.payload.res.ExtResOrderCreateResult;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtSelloService {

    private final RestTemplate restTemplate;
    private final ExternalProp prop;
    private final ObjectMapper objectMapper;

    public ExtResOrderCreateResult createOrder(String orderId, Long amount, String courseId, Integer lessonCount) {
        try {
            var request = new ExtReqOrderCreate();
            request.setOrderId(orderId);
            request.setAmount(amount);
            request.setMerchantId(prop.getSello().getEcomMerchantId());
            request.setParams(new ExtReqParams(courseId, 1));

            ResponseEntity<ExtResOrderCreate> response = restTemplate.exchange(
                    prop.getSello().getEcomUrl() + "/api/v1/invoice/create",
                    HttpMethod.POST,
                    new HttpEntity<>(request, getEcomHeaders()),
                    ExtResOrderCreate.class

            );

            if (!response.getStatusCode().is2xxSuccessful() || CoreUtils.isEmpty(response.getBody())) {
                throw new ExternalServiceException("SELLO_ECOM_EXCEPTION");
            }

            return response.getBody().getData();
        } catch (Throwable th) {
            Logger.exception(th);
            return null;
        }
    }

    public ResInvoicePayModel executeTeacherPayment(List<ExtReqInvoiceTransaction> reqInvoiceTransactionList) {
        ResInvoicePayModel result = new ResInvoicePayModel();

        ExtReqInvoicePay request = new ExtReqInvoicePay();
        request.setMerchantId(prop.getSello().getEcomMerchantId());
        request.setTransactions(reqInvoiceTransactionList);

        String url = prop.getSello().getEcomUrl() + "/api/v1/invoice/pay";
        HttpMethod method = HttpMethod.POST;
        HttpHeaders headers = getEcomHeaders();
        HttpEntity<ExtReqInvoicePay> httpEntity = new HttpEntity<>(request, headers);

        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            requestBodyJson = "Error serializing request: " + e.getMessage();
        }

        Logger.reqMain(method, url, headers.toString(), requestBodyJson);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<ExtResInvoicePay> response = restTemplate.exchange(
                    url,
                    method,
                    httpEntity,
                    ExtResInvoicePay.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Logger.resMain(
                        HttpStatus.valueOf(response.getStatusCode().value()),
                        System.currentTimeMillis() - startTime,
                        response.getHeaders().toString(),
                        "N/A");

                result.setSuccess(true);
            } else {
                Logger.resMain(
                        HttpStatus.valueOf(response.getStatusCode().value()),
                        System.currentTimeMillis() - startTime,
                        response.getHeaders().toString(),
                        CoreUtils.isPresent(response.getBody()) ? response.getBody().getErrorMessage() : "Unknown error");

                result.setErrorMessage(CoreUtils.isPresent(response.getBody()) ? response.getBody().getErrorMessage() : "Unknown error");
            }

            return result;
        } catch (Exception e) {
            Logger.resMain(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    System.currentTimeMillis() - startTime,
                    "N/A",
                    e.getMessage()
            );

            return null;
        }
    }

    public String getOfdUrl(String transactionId) {
        Logger.logInfo(">> [ExtSelloService] Requesting OFD URL for Transaction ID: " + transactionId);
        try {
            String url = prop.getSello().getEcomUrl() + "/api/v1/invoice";

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("transactionId", transactionId)
                    .queryParam("merchantId", prop.getSello().getEcomMerchantId());

            String fullUrl = builder.toUriString();
            Logger.logInfo(">> [ExtSelloService] Calling URL: " + fullUrl);

            ResponseEntity<ExtResBase<ExtReqOfdUrl>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(getEcomHeaders()),
                    new ParameterizedTypeReference<ExtResBase<ExtReqOfdUrl>>() {}
            );

            Logger.logInfo(">> [ExtSelloService] Response Code: " + response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {

                String fetchedUrl = response.getBody().getData().getOfdUrl();
                Logger.logInfo(">> [ExtSelloService] Successfully found OFD URL: " + fetchedUrl);
                return fetchedUrl;
            } else {
                Logger.logWarn(">> [ExtSelloService] Response body was empty or data was null. Body: " + response.getBody());
            }

        } catch (Exception e) {
            Logger.exception(">> [ExtSelloService] Failed  for transaction: " + transactionId + ". Error: " + e.getMessage(), e);
        }
        return null;
    }

    public ExtResOrderCreateResult createSubscriptionInvoice(String orderId, Long amount, String currency) {
        try {
            long amountInTiyins = amount * 100;
            var request = new HashMap<String, Object>();
            request.put("orderId", orderId);
            request.put("amount", amountInTiyins); // Send 100,000 instead of 1,000
            request.put("merchantId", prop.getSello().getEcomMerchantId());


            if (currency != null && !currency.equalsIgnoreCase("UZS")) {
                request.put("currency", currency);
            }

            ResponseEntity<ExtResOrderCreate> response = restTemplate.exchange(
                    prop.getSello().getEcomUrl() + "/api/v1/invoice/create",
                    HttpMethod.POST,
                    new HttpEntity<>(request, getEcomHeaders()),
                    ExtResOrderCreate.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                Logger.logWarn("Sello Subscription Invoice Failed: " + response.getStatusCode());
                return null;
            }

            return response.getBody().getData();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            Logger.logWarn("Sello API Error: " + e.getResponseBodyAsString());
            return null;
        } catch (Throwable th) {
            Logger.exception("Sello Subscription Invoice Create Critical Failure", th);
            return null;
        }
    }




    private HttpHeaders getEcomHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(prop.getSello().getEcomUsername(), prop.getSello().getEcomPassword());
        Logger.logInfo(headers.toString());
        return headers;
    }

}
