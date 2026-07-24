package br.com.salgadosdalucia.api.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record AlterarSenhaUsuarioDto(
        @NotBlank(message = "É necessário inserir a senha atual para altera-la")
        String senhaAtual,
        @NotBlank(message = "Nova senha é obrigatória!")
        String novaSenha,
        @NotBlank(message = "É necessário confirmar a nova senha")
        String novaSenhaConfirmacao
) {
}
