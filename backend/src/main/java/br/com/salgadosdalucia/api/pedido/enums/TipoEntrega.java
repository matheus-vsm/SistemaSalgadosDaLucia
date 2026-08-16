package br.com.salgadosdalucia.api.pedido.enums;

import br.com.salgadosdalucia.api.enums.EnumDescritivo;

public enum TipoEntrega implements EnumDescritivo {

    ENTREGA("Entrega"),
    RETIRADA("Retirada");

    private final String descricao;

    TipoEntrega(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

}
