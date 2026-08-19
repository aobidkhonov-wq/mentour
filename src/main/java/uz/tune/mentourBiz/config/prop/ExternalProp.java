package uz.tune.mentourBiz.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "external")
public class ExternalProp {

    private Sello sello;
    private Uzum uzum;

    private OfbPay ofbPay;

    @Data
    public static class Sello {
        private String ecomUrl;
        private String ecomMerchantId;
        private String ecomUsername;
        private String ecomPassword;
        private String paymentUrl;
    }

    @Data
    public static class OfbPay {
        private String merchantId;
        private String salt;
        private String payUrl;
        private String callbackUrl;
        private String description;
    }

    @Data
    public static class Uzum {
        private String apiUrl;
        private String terminalId;
        private String apiKey;
        private String successUrl;
        private String failureUrl;
        private Integer sessionTimeoutSecs;
        private String spic;
        private String packageCode;
        private Integer vatPercent;
        private String tin;
        private String pinfl;
        private Long course;
        private String description;
    }
}
