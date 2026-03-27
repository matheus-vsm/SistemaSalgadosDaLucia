package br.com.salgadosdalucia.api.repositoy;

import br.com.salgadosdalucia.api.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {



}
