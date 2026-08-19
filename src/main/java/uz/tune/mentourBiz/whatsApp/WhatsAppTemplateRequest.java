package uz.tune.mentourBiz.whatsApp;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WhatsAppTemplateRequest {
    private final String messaging_product = "whatsapp";
    private String to;
    private final String type = "template";
    private Template template;

    @Data
    @Builder
    public static class Template {
        private String name;
        private Language language;
        private List<Component> components;
    }

    @Data
    @Builder
    public static class Language {
        private String code;
    }

    @Data
    @Builder
    public static class Component {
        private final String type = "body";
        private List<Parameter> parameters;
    }

    @Data
    @Builder
    public static class Parameter {
        private final String type = "text";
        private String text;
    }
}