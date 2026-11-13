package br.com.salgadosdalucia.api.config;

import br.com.salgadosdalucia.api.perfil.PerfilUsuarioNome;
import br.com.salgadosdalucia.api.usuario.UsuarioService;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class BootstrapAdminTest {
    @Test
    void validaAntesDeCriarERestringePerfilAdmin() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = mock(UsuarioService.class);
            var validator = factory.getValidator();
            assertThatThrownBy(() -> new BootstrapAdmin(service, validator, "", "", "").run(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootstrapAdmin(service, validator, "Nome", "admin", "x".repeat(73)).run(null))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(service);
            new BootstrapAdmin(service, validator, "Nome", "admin", "somente-para-teste").run(null);
            verify(service).cadastrar(new UsuarioRequest("Nome", "admin", "somente-para-teste", PerfilUsuarioNome.ADMIN));
        }
    }
}
