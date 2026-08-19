package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentParentContactRepository extends BaseRepository<StudentParentContact> {

    Optional<StudentParentContact> findByUuid(UUID uuid);

    List<StudentParentContact> findAllByStudentAndIsActiveTrue(Student student);

    List<StudentParentContact> findAllByStudent(Student student);

    List<StudentParentContact> findAllByPhoneNumberOrTelegramNickname(String phoneNumber, String nickName);

    // Parent login / dashboard: a phone may be linked to several children (one row per student).
    List<StudentParentContact> findAllByPhoneNumberAndIsActiveTrue(String phoneNumber);

    @Query("SELECT c.telegramChatId FROM StudentParentContact c " +
            "WHERE c.student.school.organization.uuid = :orgUuid " +
            "AND c.telegramChatId IS NOT NULL")
    List<String> findParentChatIdsByOrganization(@Param("orgUuid") UUID orgUuid);

    @Query("SELECT c.telegramChatId FROM StudentParentContact c WHERE c.student.school.uuid = :schoolUuid AND c.telegramChatId IS NOT NULL")
    List<String> findParentChatIdsBySchool(@Param("schoolUuid") UUID schoolUuid);

}