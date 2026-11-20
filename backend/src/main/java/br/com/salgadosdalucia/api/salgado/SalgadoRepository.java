package br.com.salgadosdalucia.api.salgado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalgadoRepository extends JpaRepository<Salgado, Long> {

    Page<Salgado> findByNomeContainingIgnoreCaseAndAtivo(Pageable paginacao, String nome, boolean ativo);

    Page<Salgado> findAllByAtivo(Pageable paginacao, Boolean ativo);

}
