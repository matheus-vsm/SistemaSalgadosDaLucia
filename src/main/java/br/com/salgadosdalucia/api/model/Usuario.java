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
    public Long id;
    public String nome;
    public String login;
    public String senha;
    @Enumerated(EnumType.STRING)
    public PerfilUsuario perfilUsuario;

}
