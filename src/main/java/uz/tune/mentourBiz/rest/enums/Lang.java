package uz.tune.mentourBiz.rest.enums;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum Lang {
    UZB("uz"),   // Uzbekistan
    RUS("ru"),   // Russia
    ENG("en"),   // English
    KAA("kaa"),  // Karakalpakstan
    TJK("tg"),   // Tajikistan
    KRG("ky");   // Kyrgyzstan

    private final String frontFormat;

    Lang(String frontFormat) {
        this.frontFormat = frontFormat;
    }

    public static Lang getByName(final String name) {
        if (name == null || name.isBlank()) {
            return UZB;
        }

        return Arrays.stream(Lang.values())
                .filter(lang ->
                        lang.name().equalsIgnoreCase(name) ||
                                lang.getFrontFormat().equalsIgnoreCase(name)
                )
                .findFirst()
                .orElse(UZB);
    }
}