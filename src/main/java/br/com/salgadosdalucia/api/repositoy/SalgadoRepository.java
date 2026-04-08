package br.com.salgadosdalucia.api.repositoy;

import br.com.salgadosdalucia.api.model.Salgado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalgadoRepository extends JpaRepository<Salgado, Long> {

    List<Salgado> findByNomeContainingIgnoreCase(String nome);

    Page<Salgado> findAllByAtivoTrue(Pageable paginacao);

}
