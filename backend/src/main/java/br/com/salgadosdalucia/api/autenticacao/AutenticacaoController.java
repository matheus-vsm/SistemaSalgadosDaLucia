package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.autenticacao.dto.DadosLoginDto;
import br.com.salgadosdalucia.api.autenticacao.dto.TokenResponse;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// “Só carregue esta classe se a aplicação estiver rodando como uma aplicação web do tipo Servlet.”
// Ou seja, quando o backend rodar normal, atendendo HTTP (servidor web servlet), essa classe será carregada pelo spring. Caso suba com perfil "bootstrap-admin" com configuração "web-application-type=none", essa classe não será carregada, evitando que o spring tente criar beans de autenticação e segurança, que não são necessários.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequestMapping("/autenticacao")
@Validated
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Autenticação", description = "Endpoints públicos para autenticação e renovação de tokens")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @PostMapping(value = "/login")
    @Operation(summary = "Efetuar login", description = "Valida as credenciais e retorna tokens de acesso e renovação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados de login inválidos."),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas.")
    })
    public ResponseEntity<TokenResponse> efetuarLogin(@RequestBody @Valid DadosLoginDto dadosLoginDto) {
        TokenResponse tokenResponse = autenticacaoService.autenticar(dadosLoginDto);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping(value = "/atualizar-token")
    @Operation(summary = "Atualizar tokens", description = "Gera um novo token de acesso e um novo refresh token a partir de um refresh token válido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens atualizados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Usuário associado ao token não encontrado.")
    })
    public ResponseEntity<TokenResponse> atualizarToken(@RequestBody @Valid DadosRefreshToken dados) throws NotFoundException {
        TokenResponse tokenResponse = autenticacaoService.atualizarToken(dados);

        return ResponseEntity.ok(tokenResponse);
    }

}
