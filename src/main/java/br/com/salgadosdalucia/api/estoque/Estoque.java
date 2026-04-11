package br.com.salgadosdalucia.api.estoque;

import br.com.salgadosdalucia.api.salgado.Salgado;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estoque")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "salgado_id")
    private Salgado salgado;
    private Integer quantidade;

}
