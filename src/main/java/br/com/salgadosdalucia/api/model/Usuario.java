package br.com.salgadosdalucia.api.model;

import br.com.salgadosdalucia.api.model.enums.PerfilUsuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String login;
    private String senha;
    @Enumerated(EnumType.STRING)
    private PerfilUsuario perfilUsuario;

}
