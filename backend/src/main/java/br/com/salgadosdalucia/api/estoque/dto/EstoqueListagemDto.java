package br.com.salgadosdalucia.api.estoque.dto;

public record EstoqueListagemDto(
        Long id,
        Long salgadoId,
        String nomeSalgado,
        Integer quantidade
) {
}
