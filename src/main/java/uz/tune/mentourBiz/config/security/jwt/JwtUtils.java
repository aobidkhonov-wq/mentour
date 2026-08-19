package uz.tune.mentourBiz.config.security.jwt;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.TokenType;
import uz.tune.mentourBiz.rest.model.AuthTokenModel;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${app.jwt.accessTokenExpiration}")
    private Long accessTokenExpiration;

    @Value("${app.jwt.refreshTokenExpiration}")
    private Long refreshTokenExpiration;

    private final SecretKey key;

    public JwtUtils(@Value("${app.jwt.secret}") String jwtSecret) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public AuthTokenModel generateJwtToken(User user, TokenType tokenType) {
        return generateTokenFromUsername(user, tokenType);
    }

    private AuthTokenModel generateTokenFromUsername(User user, TokenType tokenType) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUuid());
        claims.put("username", user.getUsername());
        claims.put("tokenType", tokenType.name());

        Date expireDate = new Date(System.currentTimeMillis() +
                (TokenType.ACCESS.equals(tokenType) ? accessTokenExpiration : refreshTokenExpiration));
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(expireDate)
                .signWith(key)
                .compact();

        return new AuthTokenModel(token, expireDate.getTime());
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            Logger.exception("Invalid signature");
        } catch (MalformedJwtException e) {
            Logger.exception(MessageKey.TOKEN_INVALID.getKey());
        } catch (ExpiredJwtException e) {
            Logger.exception("Token is expired");
        } catch (UnsupportedJwtException e) {
            Logger.exception("Token is unsupported");
        } catch (IllegalArgumentException e) {
            Logger.exception("Claims is empty");
        }

        return false;
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new RuntimeException("JWT_PARSE_EXCEPTION ", ex);
        }
    }

}
