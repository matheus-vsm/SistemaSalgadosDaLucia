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
    public Long id;
    @ManyToOne
    @JoinColumn(name = "pedido_id")
    public Pedido pedido;
    @ManyToOne
    @JoinColumn(name = "salgado_id", nullable = false)
    public Salgado salgado;
    public Integer quantidade;
    @Enumerated(EnumType.STRING)
    public TipoPreco tipoPreco;
    public Double precoUnitario;
    public Double subTotal;

    @PrePersist
    @PreUpdate
    public void calcularPrecos() {
        if (this.salgado != null && this.quantidade != null) {
            double precoCento;

            if (this.tipoPreco == TipoPreco.CONGELADO) {
                precoCento = this.salgado.precoCentoCongelado;
            } else{
                precoCento = this.salgado.precoCentoProcessado;
            }
            this.precoUnitario = precoCento / 100.0;
            this.subTotal = this.precoUnitario * this.quantidade;
        }
    }

}
