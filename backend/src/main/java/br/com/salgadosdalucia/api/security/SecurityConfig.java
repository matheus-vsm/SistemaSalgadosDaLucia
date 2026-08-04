package br.com.salgadosdalucia.api.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@SecurityScheme(name = SecurityConfig.SECURITY, type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class SecurityConfig {

    public static final String SECURITY = "bearer auth";

    private final FiltroTokenAcesso FiltroTokenAcesso;

    @Bean
    public SecurityFilterChain filtrosSeguranca(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(
                            "/login", "/atualizar-token",
                            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
                    ).permitAll();

                    // Qualquer outra requisição que não foi mapeada acima exige que o usuário esteja autenticado.
                    // Se houver uma rota não listada e um FUNCIONARIO tentar acessar, ele será barrado.
                    // Um ADMIN, por sua vez, como tem a role mais alta e a hierarquia configurada,
                    // terá acesso a essas rotas não explicitamente restritas a uma role inferior.
                    req.anyRequest().authenticated();
                })
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(FiltroTokenAcesso, UsernamePasswordAuthenticationFilter.class) // para conseguir filtrar as requisições para verificar se os tokens são validos
        .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder encriptador() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy hierarquiaPerfis() {
        // String hierarquia = "ROLE_ADMIN > ROLE_MODERADOR\n" + "ROLE_MODERADOR > ROLE_INSTRUTOR\n" + "ROLE_MODERADOR > ROLE_ESTUDANTE";
        // String hierarquia = "ROLE_ADMIN > ROLE_FUNCIONARIO";
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("FUNCIONARIO").build();
    }

}
