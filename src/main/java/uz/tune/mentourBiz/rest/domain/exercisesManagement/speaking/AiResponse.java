package uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "ai_response_pronunciation")
public class AiResponse extends BaseEntity {
    @OneToOne
    private SpeakingSubmission speakingSubmission;

    @Lob
    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;
}
