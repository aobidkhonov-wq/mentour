package uz.tune.mentourBiz.rest.service.util.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.security.AppUserDetails;
import uz.tune.mentourBiz.config.security.jwt.JwtUtils;
import uz.tune.mentourBiz.exception.BadRequestException;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.SchoolDirector;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.model.AuthResponse;
import uz.tune.mentourBiz.rest.model.AuthTokenModel;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqSignIn;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqTokenRefresh;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;
import uz.tune.mentourBiz.rest.service.util.TelegramAlertService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepo userRepo;
    private final MessageSingleton messageSingleton;
    private final ObjectMapper objectMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepo studentRepo;
    private final SchoolAdminRepo schoolAdminRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final TelegramAlertService telegramAlertService;

    public AuthResponse authenticateUser(ReqSignIn request) {
        AppUserDetails userDetails = this.authenticateUser(request.getUsername(), request.getPassword());
        User user = userDetails.getUser();

        user.setLastActiveAt(java.time.Instant.now());
        userRepo.save(user);

        AuthTokenModel accessToken = jwtUtils.generateJwtToken(user, TokenType.ACCESS);
        AuthTokenModel refreshToken = jwtUtils.generateJwtToken(user, TokenType.REFRESH);

        return new AuthResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                accessToken.token(),
                accessToken.expireDate(),
                refreshToken.token(),
                refreshToken.expireDate(),
                userDetails.getRole()
        );
    }

    public AppUserDetails authenticateUser(String username, String password) {
        String schoolScope = uz.tune.mentourBiz.config.GlobalVar.getSchoolScope();
        User user;

        if (uz.tune.mentourBiz.utils.CoreUtils.isPresent(schoolScope)) {
            UUID schoolUuid = UUID.fromString(schoolScope);
            user = userRepo.findByUsernameAndSchoolUuid(username, schoolUuid)
                    .or(() -> userRepo.findDirectorByUsernameAndSchool(username, schoolUuid))
                    .orElseThrow(() -> new ValidationException(MessageKey.USER_NOT_FOUND.getKey()));
        } else {
            user = userRepo.findByUsernameAndStatus(username, UserStatus.ACTIVE)
                    .orElseThrow(() -> new ValidationException(MessageKey.USER_NOT_FOUND.getKey()));
        }

        // Alert only — no login block, BeforeFilter handles post-login blocking
        if (UserRole.SCHOOL_DIRECTOR.equals(user.getRole())) {
            schoolDirectorRepo.findByUser(user).map(SchoolDirector::getOrganization).ifPresent(org -> {
                if (SchoolStatus.DELETED.equals(org.getStatus())) {
                    telegramAlertService.notifyDeletedOrganization(user, org.getName());
                }
            });
        } else if (UserRole.SCHOOL_ADMIN.equals(user.getRole())) {
            School school = schoolAdminRepo.findByUser(user).map(sa -> sa.getSchool()).orElse(null);
            if (school != null) {
                Organization org = school.getOrganization();
                if (org != null && SchoolStatus.DELETED.equals(org.getStatus())) {
                    telegramAlertService.notifyDeletedOrganization(user, org.getName());
                } else if (SchoolStatus.FROZEN.equals(school.getStatus())) {
                    telegramAlertService.notifyFrozenSchool(user, school.getName());
                } else if (SchoolStatus.ACTIVE.equals(school.getStatus())) {
                    schoolSubscriptionRepo.findBySchool_Uuid(school.getUuid()).ifPresent(sub -> {
                        if (sub.getExpiresAt() != null && sub.getExpiresAt().isBefore(java.time.Instant.now())) {
                            telegramAlertService.notifyExpiredSubscription(user, school.getName(), sub.getExpiresAt());
                        }
                    });
                }
            }
        }

        checkStudent(user);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (Throwable th) {
            throw new ValidationException(MessageKey.PASSWORD_IS_INCORRECT.getKey());
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return (AppUserDetails) authentication.getPrincipal();
    }

    public AuthResponse authenticateUser(User user) {
        AuthTokenModel accessToken = jwtUtils.generateJwtToken(user, TokenType.ACCESS);
        AuthTokenModel refreshToken = jwtUtils.generateJwtToken(user, TokenType.REFRESH);

        return new AuthResponse(
                user.getUuid(),
                user.getUsername(),
                accessToken.token(),
                accessToken.expireDate(),
                refreshToken.token(),
                refreshToken.expireDate(),
                user.getRole()
        );
    }

    public AuthResponse refreshToken(ReqTokenRefresh request) {
        if (!jwtUtils.validate(request.getToken())) {
            throw new ValidationException(MessageKey.INVALID_TOKEN.getKey());
        }

        Claims claims = jwtUtils.getClaimsFromToken(request.getToken());
        String username = String.valueOf(claims.get("username"));
        String tokenType = String.valueOf(claims.get("tokenType"));

        if (!TokenType.REFRESH.name().equals(tokenType)) {
            throw new ValidationException(MessageKey.INVALID_TOKEN.getKey());
        }

        User user = userRepo.findByUsernameAndStatus(username, UserStatus.ACTIVE)
                .orElseThrow(() -> new ValidationException(MessageKey.USER_NOT_FOUND.getKey()));

        checkStudent(user);

        user.setLastActiveAt(java.time.Instant.now());
        userRepo.save(user);

        AuthTokenModel accessTokenModel = jwtUtils.generateJwtToken(user, TokenType.ACCESS);
        AuthTokenModel refreshToken = jwtUtils.generateJwtToken(user, TokenType.REFRESH);

        return new AuthResponse(
                user.getUuid(),
                user.getUsername(),
                accessTokenModel.token(),
                accessTokenModel.expireDate(),
                refreshToken.token(),
                refreshToken.expireDate(),
                user.getRole()
        );
    }

    public void checkStudent(User user) {
        if (user.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepo.findByUserUuid(user.getUuid()).orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            enrollmentRepository.findTopByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING)
                    .orElseThrow(() -> new BadRequestException("You do not have any active enrollments!"));
        }
    }
}
