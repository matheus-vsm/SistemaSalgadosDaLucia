package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.handler.GlobalHandlerException;
import br.com.salgadosdalucia.api.perfil.Perfil;
import br.com.salgadosdalucia.api.perfil.PerfilRepository;
import br.com.salgadosdalucia.api.perfil.PerfilUsuarioNome;
import br.com.salgadosdalucia.api.security.FiltroTokenAcesso;
import br.com.salgadosdalucia.api.security.PasswordConfig;
import br.com.salgadosdalucia.api.security.SecurityConfig;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import br.com.salgadosdalucia.api.usuario.UsuarioService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Spring MVC, provider de senha, JWT e cadeia de segurança reais; somente persistência simulada.
@WebMvcTest(controllers = AutenticacaoController.class, properties = {
        "jwt.secret=" + TokenServiceTest.SECRET, "jwt.issuer=" + TokenServiceTest.ISSUER
})
@Import({SecurityConfig.class, FiltroTokenAcesso.class, PasswordConfig.class,
        AutenticacaoService.class, TokenService.class, UsuarioService.class,
        GlobalHandlerException.class, AutenticacaoHttpTest.RotasTeste.class})
@DisplayName("Autenticação HTTP")
class AutenticacaoHttpTest {

    private static final String LOGIN = "/autenticacao/login";
    private static final String REFRESH = "/autenticacao/atualizar-token";
    private static final String PROTEGIDA = "/teste-autenticacao/protegida";

    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean PerfilRepository perfis;
    private Usuario usuario;

    @BeforeEach
    void prepararUsuario() {
        usuario = Usuario.builder().id(7L).username("alice").nome("Alice")
                .senha(passwordEncoder.encode("senha correta")).ativo(true).build();
    }

    @Nested
    @DisplayName("Efetuar login")
    class Login {
        @DisplayName("Deve validar a senha com BCrypt e retornar tokens verificáveis sem exigir autenticação prévia")
        @Test
        void loginPublicoValidaSenhaComBCryptEEmiteTokensVerificaveis() throws Exception {
            when(usuarios.findByUsernameIgnoreCase("ALICE")).thenReturn(Optional.of(usuario));

            String body = enviar(LOGIN, "{\"username\":\"ALICE\",\"senha\":\"senha correta\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenAcesso").isString())
                    .andExpect(jsonPath("$.refreshToken").isString())
                    .andExpect(jsonPath("$.senha").doesNotExist())
                    .andReturn().getResponse().getContentAsString();
            var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);

            assertThat(tokens.verificaToken(json.get("tokenAcesso").asText())).isEqualTo("alice");
            assertThat(tokens.verificaRefreshToken(json.get("refreshToken").asText())).isEqualTo("7");
        }

        @DisplayName("Deve retornar 401 sem tokens quando a senha estiver incorreta")
        @Test
        void senhaIncorretaRetorna401SemTokens() throws Exception {
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            erro(enviar(LOGIN, "{\"username\":\"alice\",\"senha\":\"errada\"}"), 401, LOGIN)
                    .andExpect(jsonPath("$.message").value("Usuário ou senha inválidos."));
        }

        @DisplayName("Deve usar a mesma resposta de credenciais inválidas para usuário inexistente")
        @Test
        void usuarioInexistenteRetornaAMesmaMensagemDeSenhaIncorreta() throws Exception {
            when(usuarios.findByUsernameIgnoreCase("inexistente")).thenReturn(Optional.empty());
            erro(enviar(LOGIN, "{\"username\":\"inexistente\",\"senha\":\"qualquer\"}"), 401, LOGIN)
                    .andExpect(jsonPath("$.message").value("Usuário ou senha inválidos."));
        }

        @DisplayName("Deve recusar login de usuário inativo mesmo com senha correta")
        @Test
        void usuarioInativoNaoFazLoginMesmoComSenhaCorreta() throws Exception {
            usuario.setAtivo(false);
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            erro(enviar(LOGIN, "{\"username\":\"alice\",\"senha\":\"senha correta\"}"), 401, LOGIN)
                    .andExpect(jsonPath("$.message").value("Usuário ou senha inválidos."));
        }
    }

    @Nested
    @DisplayName("Validar os corpos das requisições")
    class ValidacaoEntrada {
        @DisplayName("Deve rejeitar campos obrigatórios inválidos do login antes de autenticar")
        @ParameterizedTest
        @MethodSource("br.com.salgadosdalucia.api.autenticacao.AutenticacaoHttpTest#loginsInvalidos")
        void rejeitaCamposObrigatoriosDoLoginAntesDeAutenticar(String json) throws Exception {
            erro(enviar(LOGIN, json), 400, LOGIN)
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("obrigatóri")));
            verifyNoInteractions(usuarios, perfis);
        }

        @DisplayName("Deve rejeitar refresh ausente, nulo ou em branco antes de buscar usuário")
        @ParameterizedTest
        @ValueSource(strings = {"{}", "{\"refreshToken\":null}", "{\"refreshToken\":\"\"}", "{\"refreshToken\":\"   \"}"})
        void rejeitaRefreshAusenteNuloOuEmBrancoAntesDeBuscarUsuario(String json) throws Exception {
            erro(enviar(REFRESH, json), 400, REFRESH)
                    .andExpect(jsonPath("$.message").value("refreshToken: Refresh Token é obrigatório!"));
            verifyNoInteractions(usuarios, perfis);
        }

        @DisplayName("Deve retornar 400 quando o corpo da requisição estiver ausente")
        @ParameterizedTest
        @ValueSource(strings = {LOGIN, REFRESH})
        void corpoAusenteRetorna400(String rota) throws Exception {
            erro(enviar(rota, ""), 400, rota);
            verifyNoInteractions(usuarios, perfis);
        }

        @DisplayName("Deve retornar 400 quando o JSON estiver malformado")
        @ParameterizedTest
        @ValueSource(strings = {LOGIN, REFRESH})
        void jsonMalformadoRetorna400(String rota) throws Exception {
            erro(enviar(rota, "{"), 400, rota);
            verifyNoInteractions(usuarios, perfis);
        }
    }

    @Nested
    @DisplayName("Renovar tokens")
    class Renovacao {
        @DisplayName("Deve renovar tokens para o usuário consultado por ID sem exigir token de acesso")
        @Test
        void refreshPublicoRenovaParaOUsuarioConsultadoPorId() throws Exception {
            when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
            String refresh = tokens.gerarRefreshToken(usuario);
            enviar(REFRESH, "{\"refreshToken\":\"" + refresh + "\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenAcesso").isString())
                    .andExpect(jsonPath("$.refreshToken").isString());
            verify(usuarios).findById(7L);
            verify(usuarios, never()).findByUsernameIgnoreCase(anyString());
        }

        @DisplayName("Deve retornar 404 sem tokens para usuário removido")
        @Test
        void refreshDeUsuarioRemovidoRetorna404SemTokens() throws Exception {
            when(usuarios.findById(7L)).thenReturn(Optional.empty());
            erro(enviar(REFRESH, "{\"refreshToken\":\"" + tokens.gerarRefreshToken(usuario) + "\"}"), 404, REFRESH)
                    .andExpect(jsonPath("$.message").value("Usuario não encontrado com o ID: 7"));
        }

        @DisplayName("Deve retornar 400 para refresh malformado sem consultar o banco")
        @Test
        void refreshMalformadoRetorna400SemConsultarBanco() throws Exception {
            erro(enviar(REFRESH, "{\"refreshToken\":\"nao-e-jwt\"}"), 400, REFRESH)
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Token JWT inválido ou expirado!")));
            verifyNoInteractions(usuarios);
        }

        @DisplayName("Deve retornar 400 para refresh inválido ou com finalidade errada antes de buscar usuário")
        @ParameterizedTest
        @ValueSource(strings = {"acesso", "sem-tipo", "expirado", "assinatura", "emissor", "sem-expiracao", "sem-subject", "subject-invalido"})
        void refreshInvalidoOuComFinalidadeErradaRetorna400SemBuscarUsuario(String caso) throws Exception {
            var builder = JWT.create().withIssuer(caso.equals("emissor") ? "outro" : TokenServiceTest.ISSUER);
            // Username numérico torna a troca de finalidade perigosa mesmo com subject convertível para Long.
            if (!caso.equals("sem-subject")) builder.withSubject(caso.equals("subject-invalido") ? "alice" : "7");
            if (!caso.equals("sem-tipo")) builder.withClaim("tipo", caso.equals("acesso") ? "acesso" : "refresh");
            if (!caso.equals("sem-expiracao"))
                builder.withExpiresAt(Instant.now().plusSeconds(caso.equals("expirado") ? -60 : 60));
            String jwt = builder.sign(Algorithm.HMAC256(caso.equals("assinatura") ? "outro" : TokenServiceTest.SECRET));

            erro(enviar(REFRESH, "{\"refreshToken\":\"" + jwt + "\"}"), 400, REFRESH);
            verifyNoInteractions(usuarios);
        }

        @DisplayName("Deve permitir reutilizar refresh válido sem exigir strings diferentes na resposta")
        @Test
        void refreshPodeSerReutilizadoAteExpirarSemExigirQueNovasStringsSejamDiferentes() throws Exception {
            when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
            String json = "{\"refreshToken\":\"" + tokens.gerarRefreshToken(usuario) + "\"}";
            for (int tentativa = 0; tentativa < 2; tentativa++) {
                String body = enviar(REFRESH, json).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                var resposta = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
                assertThat(tokens.verificaToken(resposta.get("tokenAcesso").asText())).isEqualTo("alice");
                assertThat(tokens.verificaRefreshToken(resposta.get("refreshToken").asText())).isEqualTo("7");
            }
        }
    }

    @Nested
    @DisplayName("Autenticar requisições pelo Bearer token")
    class Acesso {
        @DisplayName("Deve preencher o principal e permitir rota protegida com acesso válido")
        @Test
        void acessoValidoPreenchePrincipalEPermiteRotaProtegida() throws Exception {
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            mvc.perform(get(PROTEGIDA).header("Authorization", "Bearer " + tokens.gerarToken(usuario)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("alice"));
        }

        @DisplayName("Deve negar acesso à rota protegida sem credenciais")
        @Test
        void semCredenciaisNaoAcessaRotaProtegida() throws Exception {
            mvc.perform(get(PROTEGIDA)).andExpect(status().isForbidden());
            verifyNoInteractions(usuarios);
        }

        @DisplayName("Deve recusar acesso de usuário removido mesmo com token emitido anteriormente")
        @Test
        void usuarioRemovidoNaoAcessaComTokenEmitidoAntesDaRemocao() throws Exception {
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());
            erro(mvc.perform(get(PROTEGIDA).header("Authorization", "Bearer " + tokens.gerarToken(usuario))),
                    401, PROTEGIDA).andExpect(jsonPath("$.message").value("Token de acesso inválido ou expirado."));
        }

        @DisplayName("Deve retornar 401 para cabeçalho inválido sem consultar usuário")
        @ParameterizedTest
        @ValueSource(strings = {"", "Basic abc", "Bearer", "Bearer ", "Bearer  abc", "Bearer abc def", "abc"})
        void cabecalhoInvalidoRetorna401SemConsultarUsuario(String header) throws Exception {
            erro(mvc.perform(get(PROTEGIDA).header("Authorization", header)), 401, PROTEGIDA);
            verifyNoInteractions(usuarios);
        }

        @DisplayName("Deve aceitar variação de maiúsculas no esquema Bearer")
        @Test
        void esquemaBearerAceitaVariacaoDeMaiusculas() throws Exception {
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            mvc.perform(get(PROTEGIDA).header("Authorization", "bearer " + tokens.gerarToken(usuario)))
                    .andExpect(status().isOk());
        }

        @DisplayName("Deve retornar 401 no formato de erro da API para falhas de token no filtro")
        @ParameterizedTest
        @ValueSource(strings = {"malformado", "expirado", "assinatura", "emissor"})
        void falhaDeTokenNoFiltroRetorna401NoContratoDeErro(String caso) throws Exception {
            var builder = JWT.create().withSubject("alice").withClaim("tipo", "acesso")
                    .withIssuer(caso.equals("emissor") ? "outro emissor" : TokenServiceTest.ISSUER)
                    .withExpiresAt(Instant.now().plusSeconds(caso.equals("expirado") ? -60 : 60));
            String jwt = caso.equals("malformado") ? "nao-e-jwt" : builder.sign(Algorithm.HMAC256(
                    caso.equals("assinatura") ? "outro segredo" : TokenServiceTest.SECRET));

            erro(mvc.perform(get(PROTEGIDA).header("Authorization", "Bearer " + jwt)), 401, PROTEGIDA)
                    .andExpect(jsonPath("$.message").value("Token de acesso inválido ou expirado."));
            verifyNoInteractions(usuarios);
        }

        @DisplayName("Deve recusar refresh como acesso mesmo quando o username for igual ao ID")
        @Test
        void refreshNaoServeParaAcessarRotaMesmoQuandoExisteUsernameIgualAoId() throws Exception {
            usuario.setUsername("7");
            erro(mvc.perform(get(PROTEGIDA).header("Authorization", "Bearer " + tokens.gerarRefreshToken(usuario))),
                    401, PROTEGIDA);
            verifyNoInteractions(usuarios);
        }

        @DisplayName("Não deve reaproveitar autenticação na requisição seguinte sem token")
        @Test
        void autenticacaoNaoVazaParaProximaRequisicaoSemToken() throws Exception {
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            mvc.perform(get(PROTEGIDA).header("Authorization", "Bearer " + tokens.gerarToken(usuario)))
                    .andExpect(status().isOk());
            mvc.perform(get(PROTEGIDA)).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Bloquear usuário desativado após emissão dos tokens")
    class Desativacao {
        @DisplayName("Deve bloquear acesso e renovação com tokens emitidos antes da desativação")
        @Test
        void desativacaoAposEmissaoBloqueiaAcessoERenovacao() throws Exception {
            String acesso = tokens.gerarToken(usuario);
            String refresh = tokens.gerarRefreshToken(usuario);
            usuario.setAtivo(false);
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));

            erro(mvc.perform(get(PROTEGIDA).header("Authorization", "Bearer " + acesso)), 401, PROTEGIDA);
            erro(enviar(REFRESH, "{\"refreshToken\":\"" + refresh + "\"}"), 401, REFRESH)
                    .andExpect(jsonPath("$.message").value("Usuário inativo."));
        }
    }

    @Nested
    @DisplayName("Acessar endpoints públicos")
    class RotasPublicas {

        @DisplayName("Deve permitir acesso às rotas públicas sem credencial")
        @ParameterizedTest
        @ValueSource(strings = {"/actuator/health", "/swagger-ui/index.html", "/v3/api-docs"})
        void regrasDeSegurancaPermitemRotasPublicasSemCredencial(String rota) throws Exception {
            mvc.perform(get(rota)).andExpect(status().isOk());
            verifyNoInteractions(usuarios);
        }

        @DisplayName("Deve retornar 401 quando uma credencial inválida for enviada ao endpoint público")
        @Test
        void credencialInvalidaExplicitaRetorna401MesmoEmEndpointPublico() throws Exception {
            erro(mvc.perform(post(LOGIN).header("Authorization", "Bearer invalido")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"alice\",\"senha\":\"senha correta\"}")), 401, LOGIN);
            verifyNoInteractions(usuarios);
        }
    }

    @Nested
    @DisplayName("Respeitar permissões e hierarquia de perfis")
    class Permissoes {

        @DisplayName("Deve permitir operações de funcionário sem conceder permissão de administrador")
        @Test
        void funcionarioAutenticadoNaoRecebePermissaoDeAdministrador() throws Exception {
            definirPerfil(PerfilUsuarioNome.FUNCIONARIO);
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            String token = tokens.gerarToken(usuario);
            mvc.perform(get("/teste-autenticacao/funcionario").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            erro(mvc.perform(get("/teste-autenticacao/admin").header("Authorization", "Bearer " + token)),
                    403, "/teste-autenticacao/admin");
        }

        @DisplayName("Deve permitir ao administrador operações de administrador e de funcionário")
        @Test
        void administradorHerdaPermissaoDeFuncionario() throws Exception {
            definirPerfil(PerfilUsuarioNome.ADMIN);
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            String token = tokens.gerarToken(usuario);
            mvc.perform(get("/teste-autenticacao/funcionario").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            mvc.perform(get("/teste-autenticacao/admin").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    static Stream<String> loginsInvalidos() {
        return Stream.of(
                "{\"senha\":\"senha\"}", "{\"username\":null,\"senha\":\"senha\"}",
                "{\"username\":\"\",\"senha\":\"senha\"}", "{\"username\":\"   \",\"senha\":\"senha\"}",
                "{\"username\":\"alice\"}", "{\"username\":\"alice\",\"senha\":null}",
                "{\"username\":\"alice\",\"senha\":\"\"}", "{\"username\":\"alice\",\"senha\":\"   \"}");
    }

    private void definirPerfil(PerfilUsuarioNome nome) {
        Perfil perfil = new Perfil();
        ReflectionTestUtils.setField(perfil, "nome", nome);
        usuario.setPerfis(List.of(perfil));
    }

    private ResultActions enviar(String rota, String json) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions erro(ResultActions resultado, int status, String rota) throws Exception {
        return resultado.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.path").value(rota))
                .andExpect(jsonPath("$.dateTime").exists())
                .andExpect(jsonPath("$.tokenAcesso").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @RestController
    static class RotasTeste {
        // Sentinelas para as regras de URL; não simulam a implementação de Swagger/Actuator.
        @GetMapping({"/actuator/health", "/swagger-ui/index.html", "/v3/api-docs"})
        String publica() {
            return "ok";
        }

        @GetMapping(PROTEGIDA)
        Map<String, String> protegida(Authentication authentication) {
            return Map.of("username", authentication.getName());
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/teste-autenticacao/admin")
        String admin() {
            return "ok";
        }

        @PreAuthorize("hasRole('FUNCIONARIO')")
        @GetMapping("/teste-autenticacao/funcionario")
        String funcionario() {
            return "ok";
        }
    }
}
