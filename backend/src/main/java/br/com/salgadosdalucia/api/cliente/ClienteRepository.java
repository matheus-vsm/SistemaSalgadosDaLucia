package br.com.salgadosdalucia.api.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Page<Cliente> findByNomeContainingIgnoreCaseAndAtivo(Pageable paginacao, String nome, boolean ativo);

    Page<Cliente> findAllByAtivo(Pageable paginacao, Boolean ativo);

}
