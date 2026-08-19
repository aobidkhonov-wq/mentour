package uz.tune.mentourBiz.rest.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.user.ResSchoolAdminOne;

import java.util.UUID;

public interface SchoolAdminService {
    ResSchoolAdminOne getOne(UUID uuid);
    Page<ResSchoolAdminOne> getAll(Pageable pageable, String schoolUuid);
    ResponseMessage deleteSchoolAdmin(UUID uuid);
}
