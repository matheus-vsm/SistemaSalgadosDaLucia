package br.com.salgadosdalucia.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
    private Long id;
    @OneToMany(mappedBy = "compra")
    private List<ItemCompra> itens;
    private BigDecimal valorTotal;
    private LocalDate dataCompra;
    private String observacao;

}
