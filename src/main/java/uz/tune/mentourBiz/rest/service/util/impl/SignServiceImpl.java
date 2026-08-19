package uz.tune.mentourBiz.rest.service.util.impl;


import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.security.jwt.JwtUtils;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.model.AuthResponse;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqSignIn;
import uz.tune.mentourBiz.rest.payload.req.auth.ReqTokenRefresh;
import uz.tune.mentourBiz.rest.payload.res.ai.ResIntrospect;
import uz.tune.mentourBiz.rest.payload.res.auth.ResSignIn;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;
import uz.tune.mentourBiz.rest.service.util.SignService;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignServiceImpl implements SignService {

    private final AuthService authService;
    @Value("${app.internal-secret}")
    private String internalSecret;
    private final UserRepo userRepo;
    private final JwtUtils jwtUtils;
    private final MessageSingleton messageSingleton;
    private final PasswordEncoder passwordEncoder;
    private final UserScopeService userScopeService;

    @Override
    public ResSignIn signIn(ReqSignIn request) {
        AuthResponse authResponse = authService.authenticateUser(request);
        return new ResSignIn(authResponse);
    }

    @Override
    public ResSignIn tokenRefresh(ReqTokenRefresh request) {
        AuthResponse authResponse = authService.refreshToken(request);
        return new ResSignIn(authResponse);
    }

    @Override
    public ResIntrospect introspect(HttpServletRequest request) {
        String providedSecret = request.getHeader("Secret-Key");
        if (!internalSecret.equals(providedSecret)) {
            throw new PermissionForbidden(MessageKey.TOKEN_INVALID.getKey());
        }

        String token = getTokenFromRequest(request);
        if (token == null || !jwtUtils.validate(token)) {
            throw new ValidationException(MessageKey.TOKEN_INVALID.getKey());
        }

        Claims claims = jwtUtils.getClaimsFromToken(token);
        String username = String.valueOf(claims.get("username"));

        User user = userRepo.findByUsernameAndStatus(username, UserStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        uz.tune.mentourBiz.config.GlobalVar.setUserDetails(new uz.tune.mentourBiz.config.security.AppUserDetails(user, new ArrayList<>()));

        UUID schoolUuid = userScopeService.resolveSchoolUuid(null);

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        ResIntrospect response = new ResIntrospect(user, schoolUuid);
        response.setAuthorizedSchoolUuids(authorizedUuids);

        uz.tune.mentourBiz.config.GlobalVar.clearContext();

        return response;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (CoreUtils.isPresent(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

}
