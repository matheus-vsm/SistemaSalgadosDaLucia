package br.com.salgadosdalucia.api.compra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemCompraRequest(
        @NotBlank(message = "O nome do item é obrigatório!")
        String nome,
        @NotNull(message = "A quantidade do item é obrigatória!")
        @Positive(message = "A quantidade deve ser maior que zero!")
        Integer quantidade,
        @NotNull(message = "O valor unitário do item é obrigatório!")
        @DecimalMin(value = "0.01", message = "O valor unitário deve ser maior que zero!")
        @Digits(integer = 8, fraction = 2, message = "Informe um valor com até duas casas decimais!")
        BigDecimal valorUnitario
) {
}
