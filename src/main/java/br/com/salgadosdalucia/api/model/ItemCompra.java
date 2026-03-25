package br.com.salgadosdalucia.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itens_compra")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne
    @JoinColumn(name = "compra_id")
    public Compra compra;
    public String nome;
    public Integer quantidade;
    public Double precoUnitario;
    public Double subTotal;

    @PrePersist
    @PreUpdate
    public void calcularSubTotal() {
        this.subTotal = this.precoUnitario * this.quantidade;
    }

}
