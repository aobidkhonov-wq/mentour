package uz.tune.mentourBiz.rest.endpoint.parent;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.rest.enums.AppType;
import uz.tune.mentourBiz.rest.enums.Platform;
import uz.tune.mentourBiz.rest.payload.ReqUpdateMobileVersion;
import uz.tune.mentourBiz.rest.payload.req.parent.ReqChangeParentPassword;
import uz.tune.mentourBiz.rest.payload.req.parent.ReqParentSignIn;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.auth.ResSignIn;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseView;
import uz.tune.mentourBiz.rest.payload.res.parent.ResChildOverview;
import uz.tune.mentourBiz.rest.payload.res.parent.ResChildStats;
import uz.tune.mentourBiz.rest.service.VersionService;
import uz.tune.mentourBiz.rest.service.parent.ParentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/parent")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;
    private final VersionService versionService;

    // Public: phone + password. Creates the PARENT account on first login if the phone
    // is registered as a student's parent contact.
    @PostMapping("/sign-in")
    public ResponseEntity<ResSignIn> signIn(@Valid @RequestBody ReqParentSignIn request) {
        return ResponseEntity.ok(parentService.signIn(request));
    }

    @PatchMapping("/password")
    @PreAuthorize("hasAuthority('PARENT')")
    public ResponseEntity<ResponseMessage> changePassword(@Valid @RequestBody ReqChangeParentPassword request) {
        return ResponseEntity.ok(parentService.changePassword(request));
    }

    // Dashboard: each child with overall + per-course attendance and score statistics.
    @GetMapping("/children")
    @PreAuthorize("hasAuthority('PARENT')")
    public ResponseEntity<List<ResChildStats>> getChildren() {
        return ResponseEntity.ok(parentService.getChildrenDashboard());
    }


    @GetMapping("/children/overview")
    @PreAuthorize("hasAuthority('PARENT')")
    public ResponseEntity<List<ResChildOverview>> getChildrenOverview() {
        return ResponseEntity.ok(parentService.getChildrenOverview());
    }

    // One child's full course dashboard (lessons, attendance per lesson, progress).
    @GetMapping("/children/courses")
    @PreAuthorize("hasAuthority('PARENT')")
    public ResponseEntity<List<ResCourseView>> getChildCourses() {
        return ResponseEntity.ok(parentService.getChildCourseDashboards());
    }

    // Parent Edu ilovasi uchun versiya tekshiruvi (VersionCheck bilan bir xil logika, faqat PARENT app scope'ida).
    @GetMapping(BaseURI.VERSION + BaseURI.CHECK)
    public ResponseEntity<Map<String, Object>> checkVersion(
            @RequestParam String version,
            @RequestParam Platform platform) {

        String schoolScope = GlobalVar.getSchoolScope();
        return ResponseEntity.ok(versionService.checkVersionStatus(version, platform, schoolScope, AppType.PARENT));
    }

    @PostMapping(BaseURI.VERSION + BaseURI.UPDATE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public ResponseEntity<ResponseMessage> updateParentAppVersions(@RequestBody ReqUpdateMobileVersion request) {
        return ResponseEntity.ok(versionService.updateAllPlatforms(request, AppType.PARENT));
    }
}
