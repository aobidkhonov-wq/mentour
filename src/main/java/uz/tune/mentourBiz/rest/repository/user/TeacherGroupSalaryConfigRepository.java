package uz.tune.mentourBiz.rest.repository.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.TeacherGroupSalaryConfig;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherGroupSalaryConfigRepository extends BaseRepository<TeacherGroupSalaryConfig> {

    List<TeacherGroupSalaryConfig> findAllBySalaryPlan_Uuid(UUID salaryPlanUuid);

    /** Overrides of several plans at once, so listing plans does not fire one query per plan. */
    @EntityGraph(attributePaths = {"salaryPlan", "group"})
    List<TeacherGroupSalaryConfig> findAllBySalaryPlan_UuidIn(Collection<UUID> salaryPlanUuids);
}
