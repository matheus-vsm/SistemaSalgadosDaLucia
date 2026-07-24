package br.com.salgadosdalucia.api.perfil;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "perfis")
public class Perfil implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private PerfilUsuarioNome nome;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.nome;
    }

}
