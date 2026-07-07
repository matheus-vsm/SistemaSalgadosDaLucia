package br.com.salgadosdalucia.api.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    @Modifying
    @Query("UPDATE Usuario u SET u.senha = :novaSenhaCriptografada WHERE u.id = :id")
    void alterarSenha(Long id, String novaSenhaCriptografada);
}
