package uz.tune.mentourBiz.rest.repository.crm;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.crm.CrmLead;

import java.util.Optional;
import java.util.UUID;

public interface CrmLeadRepository extends BaseRepository<CrmLead> {
    Optional<CrmLead> findByUuid(UUID uuid);
}
