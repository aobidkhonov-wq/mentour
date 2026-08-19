package uz.tune.mentourBiz.rest.repository.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.TeacherSalaryPlan;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherSalaryPlanRepository extends BaseRepository<TeacherSalaryPlan> {

    Optional<TeacherSalaryPlan> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"teacher", "teacher.user"})
    Optional<TeacherSalaryPlan> findByTeacher_User_Uuid(UUID teacherUserUuid);

    /** Every plan whose teacher belongs to one of the given schools. */
    @EntityGraph(attributePaths = {"teacher", "teacher.user"})
    List<TeacherSalaryPlan> findAllByTeacher_School_UuidIn(Collection<UUID> schoolUuids);

    /** Teachers assigned to a school-wide salary plan. */
    @EntityGraph(attributePaths = {"teacher", "teacher.user"})
    List<TeacherSalaryPlan> findAllBySalaryPlan_Uuid(UUID salaryPlanUuid);

    /** Every plan on the platform — SYS_ADMIN only; the teacher and user are fetched eagerly. */
    @EntityGraph(attributePaths = {"teacher", "teacher.user"})
    @Query("SELECT p FROM TeacherSalaryPlan p")
    List<TeacherSalaryPlan> findAllWithTeacher();
}
