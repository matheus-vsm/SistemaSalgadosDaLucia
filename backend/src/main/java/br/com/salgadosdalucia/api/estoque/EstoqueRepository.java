package br.com.salgadosdalucia.api.estoque;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findBySalgadoId(Long id);

    // ex: SELECT * FROM estoque WHERE salgado_id IN (1, 2, 3, 4);
    List<Estoque> findAllBySalgadoIdIn(Collection<Long> ids);

    @Query("""
            SELECT e FROM Estoque e
            JOIN e.salgado s
            WHERE s.ativo = true
            """)
    Page<Estoque> findAllSalgadosAtivos(Pageable paginacao);

}
