package br.com.salgadosdalucia.api.pedido.enums;

import br.com.salgadosdalucia.api.enums.EnumDescritivo;

public enum TipoPreco implements EnumDescritivo {

    CONGELADO("Congelado"),
    PROCESSADO("Processado (Assado/Frito)");

    private final String descricao;

    TipoPreco(String descricao) {
        this.descricao = descricao;
    }
    @Override
    public String getDescricao() {
        return descricao;
    }

}
