package br.com.salgadosdalucia.api.usuario.dto;

import br.com.salgadosdalucia.api.perfil.PerfilUsuarioNome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
        @NotBlank(message = "Nome de Usuário é obrigatório!")
        @Size(max = 100, message = "O campo nome deve ter até 100 caracteres!")
        String nome,
        @NotBlank(message = "Apelido de Usuário é obrigatório!")
        @Size(max = 50, message = "O campo username deve ter até 50 caracteres!")
        String username,
        @NotBlank(message = "Senha é obrigatória!")
        String senha,
        @NotNull(message = "Perfis de Usuário é obrigatório!")
        PerfilUsuarioNome perfilUsuarioNome
) {
}
