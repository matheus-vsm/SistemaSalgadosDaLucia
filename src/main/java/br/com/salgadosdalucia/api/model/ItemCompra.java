package br.com.salgadosdalucia.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
    private Long id;
    @ManyToOne
    @JoinColumn(name = "compra_id")
    private Compra compra;
    private String nome;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subTotal;

}
