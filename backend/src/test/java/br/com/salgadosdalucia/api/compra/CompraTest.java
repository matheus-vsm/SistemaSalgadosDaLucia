package br.com.salgadosdalucia.api.compra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Compra")
class CompraTest {

    @Nested
    @DisplayName("Gerar data por chamada direta, sem executar o ciclo de persistência JPA")
    class GerarData {
        @Test
        @DisplayName("Deve preencher data ausente com o dia atual e preservar a data nas próximas chamadas")
        void deveGerarDataAusente() {
            Compra compra = new Compra();
            LocalDate antes = LocalDate.now();

            compra.gerarData();

            LocalDate depois = LocalDate.now();
            assertThat(compra.getDataCompra()).isBetween(antes, depois);
            LocalDate atribuida = compra.getDataCompra();
            compra.gerarData();
            assertThat(compra.getDataCompra()).isEqualTo(atribuida);
        }

        @Test
        @DisplayName("Deve preservar a data explicitamente informada")
        void devePreservarDataInformada() {
            LocalDate informada = LocalDate.of(2020, 3, 14);
            Compra compra = Compra.builder().dataCompra(informada).build();

            compra.gerarData();

            assertThat(compra.getDataCompra()).isEqualTo(informada);
        }
    }

}
