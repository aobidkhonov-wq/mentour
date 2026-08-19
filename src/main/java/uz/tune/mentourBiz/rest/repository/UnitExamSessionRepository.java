package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.UnitExamSession;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UnitExamSessionRepository extends BaseRepository<UnitExamSession> {
    Optional<UnitExamSession> findByStudentAndUnit(Student student, Unit unit);
    boolean existsByStudentAndUnit(Student student, Unit unit);
    List<UnitExamSession> findAllByStudentAndUnitUuidIn(Student student, Set<UUID> unitUuids);
}