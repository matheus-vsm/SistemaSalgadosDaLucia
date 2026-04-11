package br.com.salgadosdalucia.api.salgado;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "salgados")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Salgado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String descricao;
    @Enumerated(EnumType.STRING)
    private Categoria categoria;
    private BigDecimal precoCentoCongelado;
    private BigDecimal precoCentoProcessado;
    private boolean ativo;

}
