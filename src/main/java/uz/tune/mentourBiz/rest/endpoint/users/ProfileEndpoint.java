package uz.tune.mentourBiz.rest.endpoint.users;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.profile.ReqProfileChangePassword;
import uz.tune.mentourBiz.rest.payload.req.profile.ReqProfileUpdateInfo;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.profile.ResProfileInfo;
import uz.tune.mentourBiz.rest.payload.res.profile.ResStudentProfileInfo;
import uz.tune.mentourBiz.rest.service.user.ProfileService;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.PROFILE)
@RequiredArgsConstructor
public class ProfileEndpoint {

    private final ProfileService profileService;

    @GetMapping(BaseURI.INFO)
    public ResponseEntity<ResProfileInfo> getProfileInfo() {
        return ResponseEntity.ok(profileService.getProfileInfo());
    }

    @GetMapping(BaseURI.INFO + BaseURI.STUDENT)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PROFILE_GET_INFO)")
    public ResponseEntity<ResStudentProfileInfo> getStudentInfo() {
        return ResponseEntity.ok(profileService.getStudentProfileInfo());
    }

    @PatchMapping(BaseURI.UPDATE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PROFILE_UPDATE_INFO)")
    public ResponseEntity<ResponseMessage> updateProfileInfo(@RequestBody ReqProfileUpdateInfo request) {
        return ResponseEntity.ok(profileService.updateProfileInfo(request));
    }

    // TODO KEYCLOACK TIMEEEE


    @PutMapping(BaseURI.PASSWORD)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PROFILE_CHANGE_PASSWORD)")
    public ResponseEntity<ResponseMessage> changePassword(@RequestBody ReqProfileChangePassword request) {
        return ResponseEntity.ok(profileService.changePassword(request));
    }


}