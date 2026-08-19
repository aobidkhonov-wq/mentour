package uz.tune.mentourBiz.rest.service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.req.ReqAtRiskThreshold;
import uz.tune.mentourBiz.rest.payload.res.ResAcademicConfig;
import uz.tune.mentourBiz.rest.payload.res.ResAtRiskThreshold;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicConfigServiceImpl implements AcademicConfigService {

    private final SchoolAcademicConfigRepo repo;
    private final SchoolRepo schoolRepo;
    private final AuthToViewEntity authToViewEntity;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final StudentRepo studentRepo;

    @Override
    public ResAcademicConfig getConfig(UUID schoolUuid) {
        UUID targetUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        if (targetUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        School school = schoolRepo.findByUuid(targetUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolAcademicConfig config = repo.findBySchool_Uuid(targetUuid)
                .orElseGet(() -> createDefault(targetUuid));
        return new ResAcademicConfig(config);
    }

    @Override
    @Transactional
    public ResponseMessage updateConfig(UUID schoolUuid, ResAcademicConfig req) {
        UUID targetUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        if (targetUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        School school = schoolRepo.findByUuid(targetUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        if (school.getOrganization() != null && !userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN)
                && !userService.getCurrentUser().getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            throw new ValidationException(MessageKey.ORG_SETTINGS_LOCKED.getKey());
        }

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolAcademicConfig config = repo.findBySchool_Uuid(targetUuid)
                .orElseGet(() -> createDefault(targetUuid));

        Page<Student> allBySchoolUuidInAndUserStatus =
                studentRepo.findAllBySchool_UuidInAndUser_Status(List.of(targetUuid), UserStatus.ACTIVE, PageRequest.of(0, Integer.MAX_VALUE));
        if (req.getMinScoreToPass() != null) config.setMinScoreToPass(req.getMinScoreToPass());
        if (req.getMaxRetries() != null) config.setMaxRetries(req.getMaxRetries());
        if (req.getPenaltyPerAttempt() != null) config.setPenaltyPerAttempt(req.getPenaltyPerAttempt());
        if (req.getPenaltyEnabled() != null) config.setPenaltyEnabled(req.getPenaltyEnabled());
        if (req.getIsAttemptInstalled() != null) config.setAttemptInstalled(req.getIsAttemptInstalled());
        if (req.getDefaultCourseLengthInstalled() != null) config.setDefaultCourseLengthInstalled(req.getDefaultCourseLengthInstalled());
        if (req.getAttendanceThreshold() != null){
            config.setAttendanceThreshold(req.getAttendanceThreshold());
            allBySchoolUuidInAndUserStatus.forEach(student -> {
                student.setIsAtRisk(userService.calculateIsAtRisk(student, config));
                studentRepo.save(student);
            });
        }
        if (req.getResultsThreshold() != null){
            config.setResultsThreshold(req.getResultsThreshold());
            allBySchoolUuidInAndUserStatus.forEach(student -> {
                student.setIsAtRisk(userService.calculateIsAtRisk(student, config));
                studentRepo.save(student);
            });
        }
        if (req.getTeacherMonthlyCoinLimit() != null) config.setTeacherMonthlyCoinLimit(req.getTeacherMonthlyCoinLimit());
        if (req.getDefaultCourseLengthDays() != null) config.setDefaultCourseLengthDays(req.getDefaultCourseLengthDays());

        repo.save(config);


        return new ResponseMessage("Configuration updated successfully");
    }

    @Override
    public ResAtRiskThreshold getAtRiskThresholds(UUID schoolUuid) {
        UUID targetUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        if (targetUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        School school = schoolRepo.findByUuid(targetUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolAcademicConfig config = repo.findBySchool_Uuid(targetUuid).orElseGet(() -> createDefault(targetUuid));
        return new ResAtRiskThreshold(config.getAttendanceThreshold(), config.getResultsThreshold());
    }

    @Override
    @Transactional
    public ResponseMessage updateAtRiskThresholds(ReqAtRiskThreshold req) {
        UUID targetUuid = userScopeService.resolveSchoolUuid(req.getSchoolUuid());
        if (targetUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        School school = schoolRepo.findByUuid(targetUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        if (school.getOrganization() != null && !userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN)
                && !userService.getCurrentUser().getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            throw new ValidationException(MessageKey.ORG_SETTINGS_LOCKED.getKey());
        }

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolAcademicConfig config = repo.findBySchool_Uuid(targetUuid).orElseGet(() -> createDefault(targetUuid));
        if (req.getAttendanceThreshold() != null) config.setAttendanceThreshold(req.getAttendanceThreshold());
        if (req.getResultsThreshold() != null) config.setResultsThreshold(req.getResultsThreshold());
        repo.save(config);
        return new ResponseMessage("At-risk thresholds updated successfully");
    }

    private SchoolAcademicConfig createDefault(UUID schoolUuid) {
        School school = schoolRepo.findByUuid(schoolUuid).get();
        SchoolAcademicConfig config = new SchoolAcademicConfig();
        config.setSchool(school);
        return repo.save(config);
    }
}