package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.SchoolDirector;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolDirectorRepo extends BaseRepository<SchoolDirector> {
    Optional<SchoolDirector> findByUserUuid(UUID userUuid);
    Optional<SchoolDirector> findByUser(User user);
    List<SchoolDirector> findByOrganizationId(Long id);
}
