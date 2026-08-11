package br.com.salgadosdalucia.api.perfil;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Perfil findByNome(PerfilUsuarioNome perfilUsuarioNome);
}
