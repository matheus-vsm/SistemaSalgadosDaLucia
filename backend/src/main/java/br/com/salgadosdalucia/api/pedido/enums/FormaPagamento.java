package br.com.salgadosdalucia.api.pedido.enums;

import br.com.salgadosdalucia.api.enums.EnumDescritivo;

public enum FormaPagamento implements EnumDescritivo {

    DEBITO("Débito"),
    CREDITO("Crédito"),
    PIX("PIX"),
    DINHEIRO("Dinheiro"),
    TRANSFERENCIA("Transferência");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

}
