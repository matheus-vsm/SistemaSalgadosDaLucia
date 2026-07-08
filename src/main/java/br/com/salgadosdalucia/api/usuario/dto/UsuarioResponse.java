package br.com.salgadosdalucia.api.usuario.dto;

import br.com.salgadosdalucia.api.perfil.Perfil;

import java.util.List;

public record UsuarioResponse(
        Long id,
        String nome,
        String username,
        List<Perfil> perfis,
        boolean ativo
) {
}
