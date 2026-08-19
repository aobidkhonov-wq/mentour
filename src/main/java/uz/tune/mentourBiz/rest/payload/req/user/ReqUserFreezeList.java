package uz.tune.mentourBiz.rest.payload.req.user;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqUserFreezeList {
    private List<UUID> userIds;
    private String note;
}
