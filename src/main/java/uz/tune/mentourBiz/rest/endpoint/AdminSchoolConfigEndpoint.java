package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.payload.ResUniversalSchoolSettings;
import uz.tune.mentourBiz.rest.payload.res.*;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.SchoolExamSettingsRepo;
import uz.tune.mentourBiz.rest.repository.SchoolRewardConfigRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/admin/school-management")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class AdminSchoolConfigEndpoint {

    private final SchoolRepo schoolRepo;
    private final SchoolAcademicConfigRepo academicRepo;
    private final SchoolExamSettingsRepo examRepo;
    private final SchoolRewardConfigRepo rewardRepo;
    private final SchoolSubscriptionRepo subRepo;

    @GetMapping("/{schoolUuid}/settings")
    public ResponseEntity<ResUniversalSchoolSettings> getFullSettings(@PathVariable UUID schoolUuid) {
        School school = schoolRepo.findByUuid(schoolUuid).orElseThrow();

        ResUniversalSchoolSettings settings = new ResUniversalSchoolSettings();

        academicRepo.findBySchool_Uuid(schoolUuid).ifPresent(c -> settings.setAcademic(new ResAcademicConfig(c)));
        examRepo.findBySchool_Uuid(schoolUuid).ifPresent(c -> settings.setExam(new ResSchoolExamSettings(c)));
        rewardRepo.findBySchool_Uuid(schoolUuid).ifPresent(c -> settings.setReward(new ResSchoolRewardConfig(c)));
        subRepo.findBySchool_Uuid(schoolUuid).ifPresent(c -> settings.setSubscription(new ResSchoolSubscription(school, c)));

        return ResponseEntity.ok(settings);
    }

    @PatchMapping("/{schoolUuid}/settings/subscription")
    public ResponseEntity<ResponseMessage> updateSub(@PathVariable UUID schoolUuid, @RequestBody ResSchoolSubscription req) {
        SchoolSubscription sub = subRepo.findBySchool_Uuid(schoolUuid).orElseThrow();
        if (req.getExpiresAt() != null) sub.setExpiresAt(req.getExpiresAt());
        sub.setAiExerciseEnabled(req.isAiExerciseEnabled());
        sub.setAiWritingEnabled(req.isAiWritingEnabled());
        sub.setAiSpeakingEnabled(req.isAiSpeakingEnabled());
        subRepo.save(sub);
        return ResponseEntity.ok(new ResponseMessage("Subscription updated by SysAdmin"));
    }

}