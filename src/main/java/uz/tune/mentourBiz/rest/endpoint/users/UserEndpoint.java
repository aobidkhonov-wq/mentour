package uz.tune.mentourBiz.rest.endpoint.users;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.FcmToken;
import uz.tune.mentourBiz.rest.domain.Notification;
import uz.tune.mentourBiz.rest.domain.SchoolDirector;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.student.ReqReferralRegister;
import uz.tune.mentourBiz.rest.payload.req.user.ReqAdminResetPassword;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserCreate;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserDeleteList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserFreezeList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserUpdateDetails;
import uz.tune.mentourBiz.rest.payload.res.ResNotification;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.ai.ResIntrospect;
import uz.tune.mentourBiz.rest.payload.res.user.ResUserInfo;
import uz.tune.mentourBiz.rest.repository.FcmTokenRepo;
import uz.tune.mentourBiz.rest.repository.NotificationRepo;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.NotificationService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.SignService;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.USER)
@RequiredArgsConstructor
public class UserEndpoint {

    private final UserService userService;
    private final SignService signService;
    private final FcmTokenRepo tokenRepo;
    private final NotificationRepo notificationRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserScopeService userScopeService;
    private final NotificationService notificationService;
    private final SchoolRepo schoolRepo;
    private final AuthToViewEntity authToViewEntity;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final GroupRepository groupRepository;
    private final UserRepo userRepo;

    @GetMapping(BaseURI.LIST)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_GET_ALL)")
    public ResponseEntity<Page<ResUserInfo>> getAllUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String schoolUuid,
            @RequestParam(required = false, name = "classId") UUID classId,
            @RequestParam(required = false, name = "courseId") UUID courseId,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        Sort sortByCreatedArtDesc = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sortByCreatedArtDesc);

        return ResponseEntity.ok(userService.getAllUsers(status, role, fullName, username,
                schoolName, pageable, schoolUuid, classId, courseId));
    }

    @PostMapping("/device-token")
    public ResponseEntity<ResponseMessage> registerToken(@RequestParam String token, @RequestParam String platform,
                                                         HttpServletRequest request) {
        User user = userService.getCurrentUser();

        String xLang = request.getHeader("X-Lang");
        uz.tune.mentourBiz.rest.enums.Lang lang = (xLang != null && !xLang.isBlank())
                ? uz.tune.mentourBiz.rest.enums.Lang.getByName(xLang)
                : uz.tune.mentourBiz.rest.enums.Lang.RUS;
        Optional<FcmToken> existing = tokenRepo.findByToken(token);
        if (existing.isEmpty()) {
            FcmToken fcmToken = new FcmToken();
            fcmToken.setUser(user);
            fcmToken.setToken(token);
            fcmToken.setDeviceType(platform);
            fcmToken.setLanguage(lang);
            tokenRepo.save(fcmToken);
        } else {
            FcmToken token1 = existing.get();
            token1.setUser(user);
            token1.setDeviceType(platform);
            token1.setLanguage(lang);
            tokenRepo.save(token1);
        }

        return ResponseEntity.ok(new ResponseMessage("Token registered"));
    }

    @GetMapping("/notifications")
    public ResponseEntity<Page<ResNotification>> getMyNotifications(Pageable pageable) {
        User user = userService.getCurrentUser();
        UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
        UUID groupUuid = enrollmentRepo.findTopByStudent_User_UuidAndStatus(user.getUuid(), EnrollmentStatus.ONGOING)
                .map(e -> e.getGroup().getUuid()).orElse(null);

        Page<ResNotification> response = notificationRepo.findMyNotifications(user.getUuid(), groupUuid, schoolUuid, pageable)
                .map(ResNotification::new);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ResUserInfo> getUserInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_CREATE)")
    public ResponseEntity<ResponseMessage> createUser(@RequestBody ReqUserCreate request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_UPDATE)")
    public ResponseEntity<ResponseMessage> updateUser(@PathVariable UUID userId, @RequestBody ReqUserUpdateDetails request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PatchMapping("/{userId}" + BaseURI.UNDELETE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_UNDELETE)")
    public ResponseEntity<ResponseMessage> undeleteUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.undeleteUser(userId));
    }

    @PostMapping("/{userId}" + BaseURI.RESET + BaseURI.PASSWORD)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_RESET_PASSWORD)")
    public ResponseEntity<ResponseMessage> resetPassword(@PathVariable UUID userId, @RequestBody ReqAdminResetPassword request) {
        return ResponseEntity.ok(userService.resetPassword(userId, request));
    }

    @PostMapping("/referral/register")
    public ResponseEntity<ResponseMessage> registerViaReferral(@RequestBody ReqReferralRegister request) {
        return ResponseEntity.ok(userService.registerStudentViaReferral(request));
    }

    @DeleteMapping("/{userId}" + BaseURI.PERMANENT)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_HARD_DELETE)")
    public ResponseEntity<ResponseMessage> hardDeleteUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.hardDeleteUser(userId));
    }


    @GetMapping("/introspect")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).INTROSPECT_GET)")
    public ResponseEntity<ResIntrospect> introspect(HttpServletRequest request) {
        return ResponseEntity.ok(signService.introspect(request));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BROADCAST)")
    public ResponseEntity<ResponseMessage> broadcast(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam NotificationTargetType type,
            @RequestParam(required = false) UUID targetUuid,
            @RequestParam(required = false) UUID attachmentUuid) {

        User user = userService.getCurrentUser();

        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR) && targetUuid != null && type == NotificationTargetType.SCHOOL_WIDE) {
            userScopeService.resolveSchoolUuid(targetUuid);
        }

        if (user.getRole().equals(UserRole.SCHOOL_ADMIN) && type == NotificationTargetType.BROADCAST_ALL) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        notificationService.sendGlobalNotification(title, content, type, targetUuid, attachmentUuid);
        return ResponseEntity.ok(new ResponseMessage("Broadcast sent successfully"));
    }


    @GetMapping("/sent-notifications")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SENT_NOTIFICATIONS_GET)")
    public ResponseEntity<Page<ResNotification>> getSentNotifications(
            @RequestParam(required = false) UUID targetUuid,
            @RequestParam(required = false) NotificationTargetType type,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID currentUserSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
        User currentUser = userService.getCurrentUser();
        Page<Notification> sent = null;
        switch (type) {
            case GROUP_WIDE -> {
                Group group = groupRepository.findByUuid(targetUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
                School targetSchool = group.getBranch().getSchool();
                if (!currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                    if (!targetSchool.getUuid().equals(currentUserSchoolUuid)) {
                        throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
                    }
                    sent = notificationRepo.findSentNotificationsByGroup(targetUuid, pageable);
                } else {
                    SchoolDirector byUser = schoolDirectorRepo.findByUser(currentUser).orElseThrow();
                    if (targetSchool.getOrganization().getUuid().equals(byUser.getOrganization().getUuid())) {
                        sent = notificationRepo.findSentNotificationsByGroup(targetUuid, pageable);
                    } else {
                        throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
                    }
                }
            }
            case SCHOOL_WIDE -> {
                School targetSchool = schoolRepo.findByUuid(targetUuid).orElseThrow();
                if (!currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                    if (!targetSchool.getUuid().equals(currentUserSchoolUuid)) {
                        throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
                    }
                    sent = notificationRepo.findSentNotificationsBySchool(targetSchool.getUuid(), pageable);
                }else {
                    SchoolDirector schoolDirector = schoolDirectorRepo.findByUser(currentUser).orElseThrow();
                    if (targetSchool.getOrganization().getUuid().equals(schoolDirector.getOrganization().getUuid())) {
                        sent = notificationRepo.findSentNotificationsBySchool(targetSchool.getUuid(), pageable);
                    }
                }
            }
        }
        return
                sent != null ? ResponseEntity.ok(sent.map(ResNotification::new))
                        : ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_GET_ONE)")
    public ResponseEntity<ResUserInfo> getUserByUuid(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserByUuid(userId));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_DELETE)")
    public ResponseEntity<ResponseMessage> deleteUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(userService.deleteUser(userId, note));
    }

    @DeleteMapping(BaseURI.LIST)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_DELETE)")
    public ResponseEntity<ResponseMessage> deleteUsers(@RequestBody ReqUserDeleteList request) {
        return ResponseEntity.ok(userService.deleteUsers(request));
    }

    @PutMapping(BaseURI.FREEZE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).USER_UPDATE)")
    public ResponseEntity<ResponseMessage> freezeUsers(@RequestBody ReqUserFreezeList request) {
        return ResponseEntity.ok(userService.freezeUsers(request));
    }
}