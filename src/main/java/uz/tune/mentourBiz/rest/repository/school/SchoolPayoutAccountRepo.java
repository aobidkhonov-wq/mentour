package uz.tune.mentourBiz.rest.repository.school;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.SchoolPayoutAccount;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SchoolPayoutAccountRepo extends BaseRepository<SchoolPayoutAccount> {
    Optional<SchoolPayoutAccount> findBySchool(School school);
    Optional<SchoolPayoutAccount> findBySchool_Uuid(UUID schoolId);

}