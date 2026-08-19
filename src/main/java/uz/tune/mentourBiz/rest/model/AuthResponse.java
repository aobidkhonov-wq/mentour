package uz.tune.mentourBiz.rest.model;


import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.UUID;

public record AuthResponse(UUID uuid, String username, String accessToken, Long accessTokenExpire, String refreshToken, Long refreshTokenExpire, UserRole role) {
}
