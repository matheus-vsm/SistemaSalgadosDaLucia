package br.com.salgadosdalucia.api.usuario.dto;

import br.com.salgadosdalucia.api.usuario.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
        @NotBlank(message = "Nome de Usuário é obrigatório!")
        String nome,
        @NotBlank(message = "Apelido de Usuário é obrigatório!")
        String username,
        @NotBlank(message = "Senha é obrigatória!")
        String senha,
        @NotNull(message = "Perfil de Usuário é obrigatório!")
        PerfilUsuario perfil
) {
}
