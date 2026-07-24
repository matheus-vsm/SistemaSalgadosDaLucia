package br.com.salgadosdalucia.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FiltroTokenAcesso FiltroTokenAcesso;

    @Bean
    public SecurityFilterChain filtrosSeguranca(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(req -> {
                    req.requestMatchers("/login", "/atualizar-token").permitAll();

                    // cliente
                    req.requestMatchers(HttpMethod.POST, "/clientes").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/clientes").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/clientes/**").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.PUT, "/clientes/**").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.PATCH, "/clientes/atualizar-status/**").hasRole("FUNCIONARIO");

                    // compras
                    req.requestMatchers(HttpMethod.POST, "/compras").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/compras/filtro").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/compras").hasRole("FUNCIONARIO");

                    // estoque
                    req.requestMatchers(HttpMethod.PATCH, "/estoque").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/estoque").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/estoque/**").hasRole("FUNCIONARIO");

                    // pedido
                    req.requestMatchers(HttpMethod.POST, "/pedidos").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/pedidos").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/pedidos").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.PUT, "/pedidos").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.PATCH, "/pedidos").hasRole("FUNCIONARIO");

                    // salgado
                    req.requestMatchers(HttpMethod.POST, "/salgados").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.GET, "/salgados").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/salgados/**").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.PUT, "/salgados/**").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.PATCH, "/salgados/atualizar-status/**").hasRole("ADMIN");

                    // usuario
                    req.requestMatchers(HttpMethod.POST, "/usuarios/cadastrar").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.GET, "/usuarios").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.GET, "/usuarios/**").hasRole("FUNCIONARIO");
                    req.requestMatchers(HttpMethod.PATCH, "/usuarios").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.DELETE, "/usuarios").hasRole("ADMIN");

                    // Qualquer outra requisição que não foi mapeada acima exige que o usuário esteja autenticado.
                    // Se houver uma rota não listada e um FUNCIONARIO tentar acessar, ele será barrado.
                    // Um ADMIN, por sua vez, como tem a role mais alta e a hierarquia configurada,
                    // terá acesso a essas rotas não explicitamente restritas a uma role inferior.
                    req.anyRequest().authenticated();
                })
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
