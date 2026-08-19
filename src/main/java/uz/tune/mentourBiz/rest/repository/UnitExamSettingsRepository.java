package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.UnitExamSettings;

import java.util.Optional;
import java.util.UUID;

public interface UnitExamSettingsRepository extends BaseRepository<UnitExamSettings> {
    Optional<UnitExamSettings> findByUuid(UUID uuid);
}