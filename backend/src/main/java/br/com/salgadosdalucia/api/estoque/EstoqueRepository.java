package br.com.salgadosdalucia.api.estoque;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findBySalgadoId(Long id);

    @Query("""
            SELECT e FROM Estoque e
            JOIN e.salgado s
            WHERE s.ativo = true
            """)
    Page<Estoque> findAllSalgadosAtivos(Pageable paginacao);

}
