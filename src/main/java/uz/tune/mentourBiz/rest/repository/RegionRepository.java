package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.Region;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends BaseRepository<Region> {
    Optional<Region> findByUuid(UUID uuid);
}