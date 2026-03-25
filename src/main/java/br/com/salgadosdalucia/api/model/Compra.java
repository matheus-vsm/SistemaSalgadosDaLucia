package br.com.salgadosdalucia.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "compras")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @OneToMany
    public List<ItemCompra> itens;
    public Double valorTotal;
    public LocalDate dataCompra;
    public String observacao;

    @PrePersist
    @PreUpdate
    public void calcularValorTotal() {
        if (itens == null) {
            this.valorTotal = 0.0;
            return;
        }

        this.valorTotal = itens.stream()
                .filter(item -> item.getSubTotal() != null)
                .mapToDouble(ItemCompra::getSubTotal)
                .sum();
    }

}
