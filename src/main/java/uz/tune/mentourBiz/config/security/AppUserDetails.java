package uz.tune.mentourBiz.config.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.Collection;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class AppUserDetails implements UserDetails {

    private final User user;

    private final Collection<? extends GrantedAuthority> authorities;

    public UUID getId() {
        return user.getUuid();
    }

    public UserRole getRole() {
        return user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }
}
