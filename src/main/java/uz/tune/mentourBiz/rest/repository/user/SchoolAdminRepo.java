package uz.tune.mentourBiz.rest.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolAdmin;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolAdminRepo extends BaseRepository<SchoolAdmin> {
    @EntityGraph(attributePaths = { "user", "school" })
    Optional<SchoolAdmin> findByUserUuid(UUID uuid);
    Optional<SchoolAdmin> findByUser(User user);
    List<SchoolAdmin> findAllBySchool_UuidAndUser_Status(UUID schoolUuid, UserStatus status);
    List<SchoolAdmin> findAllByUserUuidIn(List<UUID> userUuids);
    Page<SchoolAdmin> findAllBySchool_UuidAndUser_Status(UUID schoolUuid, UserStatus status, Pageable pageable);
    void deleteAllByUserIdIn(List<Long> ids);
    Page<SchoolAdmin> findAllBySchool_UuidInAndUser_Status(Collection<UUID> schoolUuids, UserStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "school" })
    Page<SchoolAdmin> findAllByUser_Status(UserStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "school" })
    Page<SchoolAdmin> findAll(Pageable pageable);
}