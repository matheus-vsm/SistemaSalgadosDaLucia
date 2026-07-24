package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.shared.endereco.Endereco;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "ativo"})
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String telefone;
    private boolean ativo;
    @Embedded
    private Endereco endereco;

}
