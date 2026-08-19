package uz.tune.mentourBiz.rest.admin.req;

import lombok.Data;
import java.util.UUID;

@Data
public class ReqUpdateWordTranslation {
    private UUID wordUuid;
    private String translationUz;
    private String translationRu;
    private String translationTjk;
    private String translationKaa;
    private String translationKrg;
}
