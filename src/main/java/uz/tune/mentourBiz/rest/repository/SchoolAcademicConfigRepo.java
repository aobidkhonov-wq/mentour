package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;

import java.util.Optional;
import java.util.UUID;

public interface SchoolAcademicConfigRepo extends BaseRepository<SchoolAcademicConfig> {
    Optional<SchoolAcademicConfig> findBySchool_Uuid(UUID schoolUuid);
}