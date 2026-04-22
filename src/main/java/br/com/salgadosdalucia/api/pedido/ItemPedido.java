package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.pedido.enums.TipoPreco;
import br.com.salgadosdalucia.api.salgado.Salgado;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    @ManyToOne
    @JoinColumn(name = "salgado_id", nullable = false)
    private Salgado salgado;
    private Integer quantidade;
    @Enumerated(EnumType.STRING)
    private TipoPreco tipoPreco;
    private BigDecimal precoUnitario;
    private BigDecimal subTotal;

}
