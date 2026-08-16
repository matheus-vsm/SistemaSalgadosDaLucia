package br.com.salgadosdalucia.api.pedido.enums;

import br.com.salgadosdalucia.api.enums.EnumDescritivo;

public enum StatusPedido implements EnumDescritivo {

    EM_ANDAMENTO("Em Andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

}
