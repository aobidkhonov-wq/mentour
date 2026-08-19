package uz.tune.mentourBiz.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.config.security.AppUserDetails;
import uz.tune.mentourBiz.config.security.AppUserDetailsService;
import uz.tune.mentourBiz.config.security.jwt.JwtUtils;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.SchoolExamSettings;
import uz.tune.mentourBiz.rest.domain.SchoolRewardConfig;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BeforeFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AppUserDetailsService appUserDetailsService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final StudentRepo studentRepo;
    private final TeacherRepository teacherRepository;
    private final SchoolAdminRepo schoolAdminRepo;
    private final SchoolRepo schoolRepo;
    private final SchoolAcademicConfigRepo academicConfigRepo;
    private final SchoolExamSettingsRepo examSettingsRepo;
    private final SchoolSubscriptionRepo subscriptionRepo;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final SchoolRewardConfigRepo rewardConfigRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        GlobalVar.clearContext();
        GlobalVar.initStartTime();

        // 1. EXTRACT HEADERS
        String X_IP = request.getHeader("X-Real-IP");
        String X_REQUEST_ID = request.getHeader("X-Request-ID");
        String X_LANG = request.getHeader("X-Lang");
        String ACCEPTED_LANG = request.getHeader("accept-language");

        String X_SCHOOL_ID = request.getHeader("X-SCHOOL-ID");

        // 2. INITIALIZE GLOBAL CONTEXT (Crucial: Sets scope before Auth takes place)
        if (!CoreUtils.isEmpty(X_LANG)) {
            Lang lang = Optional.ofNullable(Lang.getByName(X_LANG)).orElse(Lang.UZB);
            GlobalVar.setLang(lang);
        } else {
            Lang language = Optional.ofNullable(Lang.getByName(ACCEPTED_LANG)).orElse(Lang.UZB);
            GlobalVar.setLang(language);
        }
        GlobalVar.setIpAddress(Optional.ofNullable(X_IP).orElse(""));
        GlobalVar.setRequestId(Optional.ofNullable(X_REQUEST_ID).orElse(UUID.randomUUID().toString()));

        // FIX: Set school scope immediately so AuthService.authenticateUser can see it during login
        if (CoreUtils.isPresent(X_SCHOOL_ID)) {
            GlobalVar.setSchoolScope(X_SCHOOL_ID);
        }

        String uri = request.getRequestURI();
        if (uri.contains("ai/webhook")) {
            System.out.println(">>> Webhook reached filter. Token present: " + (getToken(request) != null));
        }
        boolean isPublicOrAuth = uri.contains("/public/") || uri.contains("/auth/") || uri.contains("/sign") || uri.contains("/profile");

        try {
            String jwtToken = getToken(request);
            if (CoreUtils.isPresent(jwtToken) && jwtUtils.validate(jwtToken)) {
                Claims claims = jwtUtils.getClaimsFromToken(jwtToken);
                String username = String.valueOf(claims.get("username"));
                AppUserDetails userDetails = (AppUserDetails) appUserDetailsService.loadUserByUsername(username);
                User user = userDetails.getUser();

                UUID contextSchoolUuid = null;

                // 3. RESOLVE & VERIFY SCHOOL CONTEXT (PREVENTS SPOOFING)
                if (user.getRole() == UserRole.SCHOOL_ADMIN || user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.STUDENT) {
                    // For these roles, we ignore the header and force their real assigned school
                    contextSchoolUuid = switch (user.getRole()) {
                        case SCHOOL_ADMIN ->
                                schoolAdminRepo.findByUserUuid(user.getUuid()).map(sa -> sa.getSchool().getUuid()).orElse(null);
                        case TEACHER ->
                                teacherRepository.findByUser_Uuid(user.getUuid()).map(t -> t.getSchool().getUuid()).orElse(null);
                        case STUDENT ->
                                studentRepo.findByUser_Uuid(user.getUuid()).map(s -> s.getSchool().getUuid()).orElse(null);
                        default -> null;
                    };
                } else {
                    // SysAdmin or Director: We trust the header if provided
                    if (CoreUtils.isPresent(X_SCHOOL_ID)) {
                        contextSchoolUuid = UUID.fromString(X_SCHOOL_ID);
                    }
                }

                if (contextSchoolUuid != null) {
                    // Update GlobalVar again with the VERIFIED UUID
                    GlobalVar.setSchoolScope(contextSchoolUuid.toString());
                    // Config provisioning is a side effect: a failure here must NOT
                    // reject an otherwise valid token as "token.invalid".
                    try {
                        ensureSchoolConfigsExist(contextSchoolUuid);
                    } catch (Exception e) {
                        Logger.exception("ensureSchoolConfigsExist failed for school " + contextSchoolUuid, e);
                    }
                }

                // 4. SECURITY & LOCKDOWN CHECKS
                if (user.getRole() != UserRole.SYS_ADMIN) {
                    if (contextSchoolUuid != null && !isPublicOrAuth) {
                        School school = schoolRepo.findByUuid(contextSchoolUuid).orElse(null);
                        if (school != null) {
                            // 402 Lockdown for Finance
                            if (!school.isPaymentActive() && (uri.contains("/finance") || uri.contains("/payments")) && !uri.contains("/activate-payment")) {
                                this.exception("Billing module not activated.", 402, response);
                                return;
                            }
                            // Frozen Check
                            if (school.getStatus() == SchoolStatus.FROZEN) {
                                this.exception("School is frozen.", 403, response);
                                return;
                            }
                            // Sub Expiry
                            SchoolSubscription sub = subscriptionRepo.findBySchool_Uuid(contextSchoolUuid).orElse(null);
                            if (sub != null && sub.getExpiresAt() != null && sub.getExpiresAt().isBefore(Instant.now())) {
                                this.exception("Subscription expired.", 402, response);
                                return;
                            }
                        }
                    }
                } else {
                    // SysAdmin check for billing activation in finance modules
                    if (contextSchoolUuid != null && (uri.contains("/finance") || uri.contains("/payments")) && !uri.contains("/activate-payment")) {
                        School school = schoolRepo.findByUuid(contextSchoolUuid).orElse(null);
                        if (school != null && !school.isPaymentActive()) {
                            this.exception("Billing module not activated.", 402, response);
                            return;
                        }
                    }
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                GlobalVar.setUserDetails(userDetails);
                GlobalVar.setUsername(username);
                try {
                    if (user.getId() != null) {
                        if (user.getLastActiveAt() == null || user.getLastActiveAt().isBefore(Instant.now().minusSeconds(300))) {
                            jdbcTemplate.update("UPDATE users SET last_active_at = NOW() WHERE id = ?", user.getId());
                        }
                    } else {
                        // in-memory user (sys_admin from config) — no JPA id, look up by username
                        jdbcTemplate.update(
                                "UPDATE users SET last_active_at = NOW() WHERE username = ? AND (last_active_at IS NULL OR last_active_at < NOW() - INTERVAL '300 seconds')",
                                username);
                    }
                } catch (Exception e) {
                    Logger.exception("last_active_at update failed for user " + username, e);
                }
            }
        } catch (Exception e) {
            Logger.exception("Authentication filter failed for uri " + uri, e);
            this.exception(MessageKey.TOKEN_INVALID.getKey(), 401, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void ensureSchoolConfigsExist(UUID schoolUuid) {
        schoolRepo.findByUuid(schoolUuid).ifPresent(school -> {
            if (academicConfigRepo.findBySchool_Uuid(schoolUuid).isEmpty()) {
                SchoolAcademicConfig ac = new SchoolAcademicConfig();
                ac.setSchool(school);
                academicConfigRepo.save(ac);
            }
            if (examSettingsRepo.findBySchool_Uuid(schoolUuid).isEmpty()) {
                SchoolExamSettings es = new SchoolExamSettings();
                es.setSchool(school);
                es.setNoScreenshot(true);
                es.setAttemptLimit(1);
                es.setTimeLimit(60);
                examSettingsRepo.save(es);
            }
            if (subscriptionRepo.findBySchool_Uuid(schoolUuid).isEmpty()) {
                SchoolSubscription sub = new SchoolSubscription();
                sub.setSchool(school);
                sub.setExpiresAt(Instant.now().plus(java.time.Duration.ofDays(30)));
                subscriptionRepo.save(sub);
            }

            // 4. Reward Configuration
            if (rewardConfigRepo.findBySchool_Uuid(schoolUuid).isEmpty()) {
                SchoolRewardConfig rc = new SchoolRewardConfig();
                rc.setSchool(school);
                rc.setExerciseAutoEnabled(true); // Enabled by default
                rc.setVocabAutoEnabled(true);    // Enabled by default
                rc.setGapFillBase(3);
                rc.setOrderingBase(1);
                rc.setMatchingBase(2);
                rc.setSelectionBase(5);
                rc.setMultiSelectBase(3);
                rc.setCircleBase(3);
                rc.setTracingBase(2);
                rc.setAudioMultiplierEnabled(true);
                rc.setAudioMultiplier(1.67);
                rewardConfigRepo.save(rc);
            }
        });
    }

    private String getToken(HttpServletRequest request) {
        String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (CoreUtils.isPresent(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private void exception(String message, Integer code, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        GlobalVar.clearContext();
        response.setStatus(code);
        OutputStream out = response.getOutputStream();
        final Map<String, Object> body = new HashMap<>();
        body.put("status", code);
        body.put("message", message);
        objectMapper.writeValue(out, body);
        out.flush();
    }
}