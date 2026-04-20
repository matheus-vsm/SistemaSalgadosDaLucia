package br.com.salgadosdalucia.api.usuario.dto;

import br.com.salgadosdalucia.api.usuario.Usuario;

public class UsuarioMapper {

    public static UsuarioPedidoDto mapToUsuarioPedidoDto(Usuario usuario) {
        return new UsuarioPedidoDto(usuario.getId(), usuario.getNome());
    }

}
