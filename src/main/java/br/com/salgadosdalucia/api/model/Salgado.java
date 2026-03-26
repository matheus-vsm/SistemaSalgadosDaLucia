package br.com.salgadosdalucia.api.model;

import br.com.salgadosdalucia.api.model.enums.Categoria;
import jakarta.persistence.*;
import lombok.*;

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
    private Double precoCentoCongelado;
    private Double precoCentoProcessado;
    private boolean ativo;

}
