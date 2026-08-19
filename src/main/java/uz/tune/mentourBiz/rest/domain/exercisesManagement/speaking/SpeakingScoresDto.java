package uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeakingScoresDto {
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer coherenceScore;
    private Integer overallScore;
}