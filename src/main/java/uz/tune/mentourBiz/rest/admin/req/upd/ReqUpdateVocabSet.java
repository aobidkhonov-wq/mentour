package uz.tune.mentourBiz.rest.admin.req.upd;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.VocabularySetStatus;

@Data
public class ReqUpdateVocabSet {
    private String title;
    private Integer sortOrder;
    private VocabularySetStatus status;
}