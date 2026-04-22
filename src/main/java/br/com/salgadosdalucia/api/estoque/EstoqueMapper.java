package br.com.salgadosdalucia.api.estoque;

import br.com.salgadosdalucia.api.estoque.dto.EstoqueListagemDto;

public class EstoqueMapper {

    public static EstoqueListagemDto mapToDto(Estoque estoque) {
        return new EstoqueListagemDto(
                estoque.getId(),
                estoque.getSalgado().getNome(),
                estoque.getQuantidade()
        );
    }

}
