package csc478project.restaurantapp;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
@EnableConfigurationProperties(StaffAccountsProperties.class)
public class SecurityConfig {
    private static final Set<String> STAFF_ROLES = Set.of(
        "MANAGER", "OWNER", "ADMIN", "CHEF", "WAITER", "HOST"
    );

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
        StaffAccountsProperties accounts,
        PasswordEncoder passwordEncoder,
        ObjectProvider<JdbcTemplate> jdbcProvider
    ) {
        var users = new ArrayList<UserDetails>();
        accounts.getAccounts().forEach((configuredRole, account) -> {
            String role = configuredRole.toUpperCase(Locale.ROOT);
            if (!STAFF_ROLES.contains(role)) {
                throw new IllegalStateException("Unsupported staff role: " + configuredRole);
            }
            if (account == null || account.getPassword() == null || account.getPassword().isBlank()) {
                return;
            }
            if (account.getUsername() == null || account.getUsername().isBlank()) {
                throw new IllegalStateException("Username is required for role: " + configuredRole);
            }
            users.add(User.withUsername(account.getUsername())
                .password(passwordEncoder.encode(account.getPassword()))
                .roles(role)
                .build());
        });
        UserDetailsService bootstrapAccounts = new InMemoryUserDetailsManager(users);
        JdbcTemplate jdbc = jdbcProvider.getIfAvailable();
        return jdbc == null ? bootstrapAccounts : new StaffAccountDetailsService(jdbc, bootstrapAccounts);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/", "/restaurant.html", "/login.html", "/signup.html", "/owner-setup.html",
                    "/access-denied.html", "/styles.css", "/app.css", "/js/**", "/api/auth/csrf",
                    "/api/auth/signup", "/api/auth/owner-setup-available", "/api/auth/owner-setup"
                ).permitAll()
                .requestMatchers("/owner", "/owner.html").hasRole("OWNER")
                .requestMatchers("/api/summary").hasAnyRole("MANAGER", "OWNER")
                .requestMatchers("/api/staff/accounts").hasAnyRole("MANAGER", "OWNER")
                .requestMatchers("/manager", "/manager.html", "/api/manager/**")
                    .hasAnyRole("MANAGER", "OWNER")
                .requestMatchers("/admin", "/admin.html", "/api/admin/**")
                    .hasAnyRole("ADMIN", "OWNER")
                .requestMatchers("/chef.html", "/kitchen.html")
                    .hasAnyRole("CHEF", "MANAGER", "OWNER")
                .requestMatchers("/waiter.html").hasAnyRole("WAITER", "MANAGER", "OWNER")
                .requestMatchers("/host.html").hasAnyRole("HOST", "MANAGER", "OWNER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login.html?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN"))
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")
                )
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login.html"),
                    new NegatedRequestMatcher(new AntPathRequestMatcher("/api/**"))
                )
                .accessDeniedHandler((request, response, exception) -> {
                    if (new AntPathRequestMatcher("/api/**").matches(request)) {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(
                            "{\"error\":\"Your account does not have permission to access this resource.\"}"
                        );
                    } else {
                        response.sendRedirect("/access-denied.html");
                    }
                }));
        return http.build();
    }
}