package uz.tune.mentourBiz.rest.repository;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.SchoolRewardConfig;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRewardConfigRepo extends BaseRepository<SchoolRewardConfig> {
    Optional<SchoolRewardConfig> findBySchool_Uuid(UUID schoolUuid);
}