package br.com.salgadosdalucia.api.model;

import br.com.salgadosdalucia.api.model.enums.TipoPreco;
import jakarta.persistence.*;
import lombok.*;

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
    private Double precoUnitario;
    private Double subTotal;

}
