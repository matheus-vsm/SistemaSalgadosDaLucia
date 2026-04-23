package br.com.salgadosdalucia.api.compra;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Query("""
            SELECT DISTINCT c FROM Compra c
            JOIN ItemCompra ic
            ON c.id = ic.compra.id 
            WHERE (:dataCompra IS NULL OR c.dataCompra = :dataCompra)
            AND (
                (:dataInicioCompra IS NULL OR :dataFimCompra IS NULL)
                OR c.dataCompra BETWEEN :dataInicioCompra AND :dataFimCompra
            )
            AND (:nomeItem IS NULL OR UPPER(ic.nome) LIKE UPPER(CONCAT('%', :nomeItem, '%')))
            AND (:observacao IS NULL OR UPPER(c.observacao) LIKE UPPER(CONCAT('%', :observacao, '%')))
            """)
    Page<Compra> findAllWithFiltros(LocalDate dataCompra, LocalDate dataInicioCompra, LocalDate dataFimCompra,
                                    String nomeItem, String observacao, Pageable paginacao);

}
