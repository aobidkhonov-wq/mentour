package uz.tune.mentourBiz.rest.repository.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolMentor;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolMentorRepo extends BaseRepository<SchoolMentor> {

    Page<SchoolMentor> findAllBySchool_Uuid(UUID schoolUuid, Pageable pageable);
    List<SchoolMentor> findAllBySchool_Uuid(UUID schoolUuid);

    Optional<SchoolMentor> findByUuid(UUID uuid);
    List<SchoolMentor> findAllByMentorUserUuid(UUID mentorUserUuid);

}