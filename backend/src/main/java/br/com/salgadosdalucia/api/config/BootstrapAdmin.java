package br.com.salgadosdalucia.api.config;

import br.com.salgadosdalucia.api.perfil.PerfilUsuarioNome;
import br.com.salgadosdalucia.api.usuario.UsuarioService;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("bootstrap-admin")
public class BootstrapAdmin implements ApplicationRunner {

    private final UsuarioService usuarios;
    private final Validator validator;
    private final UsuarioRequest request;

    public BootstrapAdmin(UsuarioService usuarios, Validator validator,
                          @Value("${bootstrap.admin.name}") String name,
                          @Value("${bootstrap.admin.username}") String username,
                          @Value("${bootstrap.admin.password}") String password) {
        this.usuarios = usuarios;
        this.validator = validator;
        this.request = new UsuarioRequest(name, username, password, PerfilUsuarioNome.ADMIN);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!validator.validate(request).isEmpty() || request.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Preencha BOOTSTRAP_ADMIN_NAME, USERNAME e PASSWORD conforme as validacoes de cadastro (senha ate 72 bytes).");
        }
        // O servico aplica BCrypt, valida duplicidade e salva em transacao.
        usuarios.cadastrar(request);
        System.out.println("Administrador criado. Remova as variaveis BOOTSTRAP_ADMIN_* do ambiente.");
    }

}
