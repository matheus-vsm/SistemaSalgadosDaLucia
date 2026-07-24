package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.usuario.dto.UsuarioPedidoDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;
import br.com.salgadosdalucia.api.perfil.Perfil;

import java.util.List;

public class UsuarioMapper {

    public static UsuarioPedidoDto mapToUsuarioPedidoDto(Usuario usuario) {
        return new UsuarioPedidoDto(usuario.getId(), usuario.getNome());
    }

    public static UsuarioResponse mapToUsuarioResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getUsername(),
                usuario.getPerfis(), usuario.isAtivo()
        );
    }

    public static Usuario mapToEntity(UsuarioRequest dados, String senhaCriptografada, Perfil perfil) {
        return new Usuario(null, dados.nome(), dados.username(), senhaCriptografada, List.of(perfil), true);
    }

}
