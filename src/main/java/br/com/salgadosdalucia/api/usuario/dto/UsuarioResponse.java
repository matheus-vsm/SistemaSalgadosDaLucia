package br.com.salgadosdalucia.api.usuario.dto;

import br.com.salgadosdalucia.api.usuario.PerfilUsuario;

public record UsuarioResponse(
        Long id,
        String nome,
        String username,
        PerfilUsuario perfil,
        boolean ativo
) {
}
