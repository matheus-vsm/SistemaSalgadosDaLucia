package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.security.FiltroTokenAcesso;
import br.com.salgadosdalucia.api.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Contexto de autenticação")
class AutenticacaoContextTest {
    @Nested
    @DisplayName("Inicializar contexto não web do bootstrap")
    class BootstrapNaoWeb {
        @DisplayName("Deve dispensar componentes web e suas dependências de segurança e MVC")
        @Test
        void contextoNaoWebUsadoNoBootstrapDispensaDependenciasDeSegurancaEMvc() {
            new ApplicationContextRunner()
                    .withUserConfiguration(AutenticacaoController.class, AutenticacaoService.class,
                            FiltroTokenAcesso.class, SecurityConfig.class)
                    .run(context -> assertThat(context).hasNotFailed()
                            .doesNotHaveBean(AutenticacaoController.class)
                            .doesNotHaveBean(AutenticacaoService.class)
                            .doesNotHaveBean(FiltroTokenAcesso.class)
                            .doesNotHaveBean(SecurityConfig.class));
        }
    }
}
