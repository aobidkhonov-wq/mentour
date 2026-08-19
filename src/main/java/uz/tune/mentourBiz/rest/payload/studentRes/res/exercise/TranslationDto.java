package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TranslationDto {
    private String lang;      // "UZ", "RU", "TJK", "KAA"
    private String value;
    private boolean primary;
}
