package uz.tune.mentourBiz.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.config.prop.AuthProp;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.repository.user.PermissionRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;
    private final PermissionRepo permissionRepo;
    private final AuthProp authProp;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Check for in-memory/system users first
        var optionalInMemoryUser = authProp.getUsers().stream().filter(user -> user.getUsername().equals(username)).findFirst();
        if (optionalInMemoryUser.isPresent()) {
            AuthProp.User user = optionalInMemoryUser.get();
            var systemUser = new User();
            systemUser.setPassword(passwordEncoder.encode(user.getPassword()));
            systemUser.setRole(UserRole.valueOf(user.getRole()));
            return new AppUserDetails(systemUser, new HashSet<>());
        }

        String schoolScope = GlobalVar.getSchoolScope();
        User user;

        // 2. Lookup Logic: Use scope if provided, otherwise perform a global lookup (works for Directors/SysAdmins)
        if (CoreUtils.isPresent(schoolScope)) {
            UUID schoolUuid = UUID.fromString(schoolScope);
            user = userRepo.findByUsernameAndSchoolUuid(username, schoolUuid)
                    .or(() -> userRepo.findDirectorByUsernameAndSchool(username, schoolUuid))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found in the selected school context."));
        } else {
            // Global lookup for users with unique usernames (Directors)
            user = userRepo.findByUsernameAndStatus(username, UserStatus.ACTIVE)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found or account is inactive."));
        }

        // 3. Populate Authorities
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));

        permissionRepo.findAllByRole(user.getRole()).forEach(permission
                -> authorities.add(new SimpleGrantedAuthority(permission.getName().name())));

        // 4. Update Language Preference if needed
        if (!GlobalVar.getLang().equals(user.getLang())) {
            user.setLang(GlobalVar.getLang());
            user = userRepo.saveAndFlush(user);
        }

        return new AppUserDetails(user, authorities);
    }
}
