package uz.tune.mentourBiz.rest.admin.req.create.adminCreate;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.VocabularySetStatus;

import java.util.UUID;

@Data
public class ReqCreateVocabSet {
    private UUID unitUuid;
    private String title;
    private Integer sortOrder;
    private VocabularySetStatus status;
}