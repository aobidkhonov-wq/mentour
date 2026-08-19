package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolRewardConfig;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.service.RewardConfigService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/reward-config")
@RequiredArgsConstructor
public class RewardConfigEndpoint {

    private final RewardConfigService service;

    @GetMapping("/{schoolUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).CONFIG_GET)")
    public ResponseEntity<ResSchoolRewardConfig> getConfig(@PathVariable UUID schoolUuid) {
        return ResponseEntity.ok(service.getConfig(schoolUuid));
    }

    @PatchMapping("/{schoolUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).CONFIG_UPDATE)")
    public ResponseEntity<ResponseMessage> updateConfig(
            @PathVariable UUID schoolUuid,
            @RequestBody ResSchoolRewardConfig req) {
        return ResponseEntity.ok(service.updateConfig(schoolUuid, req));
    }
}