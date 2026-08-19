package uz.tune.mentourBiz.rest.repository.unit.exercise;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UnitProgressRepository extends BaseRepository<UnitProgress> {
    @EntityGraph(attributePaths = {"unit"})
    List<UnitProgress> findAllByUnit_UuidInAndStudent_User_Uuid(Set<UUID> uuids, UUID studentUuid);
    List<UnitProgress> findAllByStudent_IdInAndUnit_IdIn(List<Long> studentIds, List<Long> unitIds);
    Optional<UnitProgress> findByStudentAndUnit(Student student, Unit unit);

    @Query("SELECT up FROM UnitProgress up WHERE up.unit.uuid IN :unitUuids AND up.student.user.uuid IN :studentUuids")
    List<UnitProgress> findAllByUnit_UuidInAndStudent_User_UuidIn(
            @Param("unitUuids") Set<UUID> unitUuids,
            @Param("studentUuids") Set<UUID> studentUuids
    );

    @Query("""
    SELECT AVG(CAST(up.progressPercentage AS double)) 
    FROM UnitProgress up 
    WHERE up.status= 'COMPLETED' AND up.unit.status = 'ACTIVE' AND  up.student.id IN :studentIds 
    AND up.unit.id IN :unitIds
    """)
    Double getAverageProgressForStudentsAndUnits(
            @Param("studentIds") List<Long> studentIds,
            @Param("unitIds") List<Long> unitIds
    );
    @Query("""
    SELECT AVG(CAST(up.progressPercentage AS double))
     FROM UnitProgress up
     WHERE up.unit.status = 'ACTIVE' AND  up.student.id = :studentId
     AND up.unit.id IN :unitIds
    """)
    Double getAverageProgressForStudentsAndUnitsV2(
            @Param("studentId") Long studentId,
            @Param("unitIds") List<Long> unitIds
    );
}