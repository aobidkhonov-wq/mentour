package uz.tune.mentourBiz.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.config.filter.AfterFilter;
import uz.tune.mentourBiz.config.filter.BeforeFilter;



@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserDetailsService appUserDetailsService;
    private final BeforeFilter beforeFilter;
    private final AfterFilter afterFilter;
    private final PasswordEncoder passwordEncoder;

    private final String[] PERMIT_URLS = new String[]{
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v2/api-docs/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/doc/**",
            "/webjars/**",
            "/favicon.ico",
            "/v4/api-docs/**",
            "/swagger.json",
            "/error",
            "/public/**",
            BaseURI.PING,
            BaseURI.API1 + BaseURI.AUTH + "/**",
            BaseURI.API1 + "/parent/sign-in",
            BaseURI.API1 + BaseURI.BBB + BaseURI.WEBHOOK,
            BaseURI.API1 + BaseURI.USER + "/introspect",
            BaseURI.API1 + "/public/telegram-webhook/**",
            BaseURI.API1 + BaseURI.LESSONS + BaseURI.MARKERS + "/*",
            BaseURI.API1 + BaseURI.BBB + "/plugin/config",
            BaseURI.API1 + BaseURI.USER + "/referral/register",
            BaseURI.API1 + BaseURI.SPEAKING + "/webhook/ai",
            BaseURI.API1 + "/public/ai/webhook/**",
            BaseURI.API1 + "/public/ai/webhook/explanation",
            "*/webhook",
            BaseURI.API1 + "/public/**",
            "/api/v1/public/sello/subscription/**",
            BaseURI.API1 + "/public/sello/**",
            BaseURI.API1 + "/public/octo/**",
            BaseURI.API1 + "/public/uzum/**",
            "/api/v1/public/sello/**",
            "/api/v1/public/sello/subscription/**",
            "/api/v1/public/sello/**",
            "/api/v1/public/sello/check-invoice",
            "/api/v1/public/sello/pay-info",
            "/api/v1/public/ofb-pay/**"
    };

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();

        authenticationProvider.setUserDetailsService(appUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/").permitAll()
                        .requestMatchers(PERMIT_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(beforeFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(afterFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }
}