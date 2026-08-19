package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.SchoolRewardConfig;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolRewardConfig;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.SchoolRewardConfigRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardConfigServiceImpl implements RewardConfigService {

    private final SchoolRewardConfigRepo repo;
    private final SchoolRepo schoolRepo;
    private final AuthToViewEntity authToViewEntity;
    private final UserService userService;
    private final UserScopeService userScopeService;

    @Override
    public ResSchoolRewardConfig getConfig(UUID schoolUuid) {
        UUID targetUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        if (targetUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        School school = schoolRepo.findByUuid(targetUuid).orElseThrow();
        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolRewardConfig config = repo.findBySchool_Uuid(targetUuid)
                .orElseGet(() -> {
                    SchoolRewardConfig c = new SchoolRewardConfig();
                    c.setSchool(school);
                    return repo.save(c);
                });
        return new ResSchoolRewardConfig(config);
    }

    @Override
    @Transactional
    public ResponseMessage updateConfig(UUID schoolUuid, ResSchoolRewardConfig req) {
        UUID targetUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        School school = schoolRepo.findByUuid(targetUuid).orElseThrow();

        // Lockdown check: If linked to Org, only SysAdmin or Director can change these
        if (school.getOrganization() != null &&
                !userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN) &&
                !userService.getCurrentUser().getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            throw new ValidationException(MessageKey.ORG_SETTINGS_LOCKED.getKey());
        }

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolRewardConfig config = repo.findBySchool_Uuid(targetUuid).orElse(new SchoolRewardConfig());
        config.setSchool(school);

        mapDtoToEntity(req, config);

        repo.save(config);
        return new ResponseMessage("Reward configuration updated.");
    }

    public void mapDtoToEntity(ResSchoolRewardConfig req, SchoolRewardConfig config) {
        config.setExerciseAutoEnabled(req.isExerciseAutoEnabled());
        config.setGapFillBase(req.getGapFillBase());
        config.setOrderingBase(req.getOrderingBase());
        config.setMatchingBase(req.getMatchingBase());
        config.setSelectionBase(req.getSelectionBase());
        config.setMultiSelectBase(req.getMultiSelectBase());
        config.setCircleBase(req.getCircleBase());
        config.setTracingBase(req.getTracingBase());
        config.setAudioMultiplierEnabled(req.isAudioMultiplierEnabled());
        config.setAudioMultiplier(req.getAudioMultiplier());
        config.setVocabAutoEnabled(req.isVocabAutoEnabled());
        config.setVocabRewardPerWord(req.getVocabRewardPerWord());
    }
}