package uz.tune.mentourBiz.rest.admin.req.create.adminCreate;

import lombok.Data;
import java.util.UUID;

@Data
public class ReqLinkWordToSet {
    private UUID setUuid;
    private UUID wordUuid;
}