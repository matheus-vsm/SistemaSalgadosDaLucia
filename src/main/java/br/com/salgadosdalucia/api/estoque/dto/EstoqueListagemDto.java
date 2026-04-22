package br.com.salgadosdalucia.api.estoque.dto;

public record EstoqueListagemDto(
        Long id,
        String nomeSalgado,
        Integer quantidade
) {
}
