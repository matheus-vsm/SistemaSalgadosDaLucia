package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.usuario.dto.UsuarioPedidoDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;

public class UsuarioMapper {

    public static UsuarioPedidoDto mapToUsuarioPedidoDto(Usuario usuario) {
        return new UsuarioPedidoDto(usuario.getId(), usuario.getNome());
    }

    public static UsuarioResponse mapToUsuarioResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getUsername(),
                usuario.getPerfil(), usuario.isAtivo()
        );
    }

    public static Usuario mapToEntity(UsuarioRequest dados, String senhaCriptografada) {
        return new Usuario(null, dados.nome(), dados.username(), senhaCriptografada, dados.perfil(), true);
    }

}
