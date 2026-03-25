package br.com.salgadosdalucia.api.model;

import br.com.salgadosdalucia.api.enums.Categoria;
import br.com.salgadosdalucia.api.enums.StatusSalgado;
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
    public Long id;
    public String nome;
    public String descricao;
    @Enumerated(EnumType.STRING)
    public Categoria categoria;
    public Double precoCentoCongelado;
    public Double precoCentoProcessado;
    @Enumerated(EnumType.STRING)
    public StatusSalgado status;

}
