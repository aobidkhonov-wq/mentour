package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.rest.enums.Platform;
import uz.tune.mentourBiz.rest.payload.ReqUpdateMobileVersion;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.service.VersionService;

import java.util.Map;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.CHECK)
@RequiredArgsConstructor
public class VersionCheck {

    private final VersionService versionService;

    @GetMapping(BaseURI.VERSION)
    public ResponseEntity<Map<String, Object>> checkVersion(
            @RequestParam String version,
            @RequestParam Platform platform) {

        String schoolScope = GlobalVar.getSchoolScope();
        return ResponseEntity.ok(versionService.checkVersionStatus(version, platform, schoolScope));
    }

    @PostMapping(BaseURI.VERSION + BaseURI.UPDATE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public ResponseEntity<ResponseMessage> updateMobileVersions(@RequestBody ReqUpdateMobileVersion request) {
        return ResponseEntity.ok(versionService.updateAllPlatforms(request));
    }

}
