package br.com.salgadosdalucia.api.salgado.enums;

import br.com.salgadosdalucia.api.enums.EnumDescritivo;

public enum Categoria implements EnumDescritivo {
    FRITO("Frito"),
    ASSADO("Assado");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
