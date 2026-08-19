package uz.tune.mentourBiz.rest.repository;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.SchoolExamSettings;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolExamSettingsRepo extends BaseRepository<SchoolExamSettings> {
    Optional<SchoolExamSettings> findBySchool_Uuid(UUID schoolUuid);
}
