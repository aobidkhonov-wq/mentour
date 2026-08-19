package uz.tune.mentourBiz.rest.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.ReqAtRiskThreshold;
import uz.tune.mentourBiz.rest.payload.res.ResAcademicConfig;
import uz.tune.mentourBiz.rest.payload.res.ResAtRiskThreshold;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.service.AcademicConfigService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/academic-config")
@RequiredArgsConstructor
public class AcademicConfigEndpoint {

    private final AcademicConfigService service;

    @GetMapping("/{schoolUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).CONFIG_GET)")
    public ResponseEntity<ResAcademicConfig> getConfig(@PathVariable UUID schoolUuid) {
        return ResponseEntity.ok(service.getConfig(schoolUuid));
    }

    @PatchMapping("/{schoolUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).CONFIG_UPDATE)")
    public ResponseEntity<ResponseMessage> updateConfig(
            @PathVariable UUID schoolUuid,
            @RequestBody ResAcademicConfig req) {
        return ResponseEntity.ok(service.updateConfig(schoolUuid, req));
    }

    // Attendance + result thresholds that drive the "at-risk" classification.
    @GetMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).CONFIG_GET)")
    public ResponseEntity<ResAtRiskThreshold> get(@RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(service.getAtRiskThresholds(schoolUuid));
    }

    @PutMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).CONFIG_UPDATE)")
    public ResponseEntity<ResponseMessage> update(@Valid @RequestBody ReqAtRiskThreshold request) {
        return ResponseEntity.ok(service.updateAtRiskThresholds(request));
    }
}
