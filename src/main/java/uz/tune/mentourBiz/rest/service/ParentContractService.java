package uz.tune.mentourBiz.rest.service;

import uz.tune.mentourBiz.rest.payload.req.ReqAddParents;
import uz.tune.mentourBiz.rest.payload.req.ReqUpdateParent;
import uz.tune.mentourBiz.rest.payload.res.ResParentDetail;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.List;
import java.util.UUID;

public interface ParentContractService {
    ResponseMessage addParentsBatch(ReqAddParents req);
    List<ResParentDetail> getParentsByStudent(UUID studentUuid);
    ResParentDetail updateParent(UUID contactUuid, ReqUpdateParent req);
    ResponseMessage removeParentLink(UUID contactUuid);
}
