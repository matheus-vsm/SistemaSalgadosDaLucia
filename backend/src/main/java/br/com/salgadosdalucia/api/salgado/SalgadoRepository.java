package br.com.salgadosdalucia.api.salgado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface SalgadoRepository extends JpaRepository<Salgado, Long> {

    Page<Salgado> findByNomeContainingIgnoreCaseAndAtivo(Pageable paginacao, String nome, boolean ativo);

    @Query("select count(s) > 0 from Salgado s where upper(trim(s.nome)) = upper(trim(:nome))")
    boolean existsByNome(@Param("nome") String nome);

    @Query("select count(s) > 0 from Salgado s where upper(trim(s.nome)) = upper(trim(:nome)) and s.id <> :id")
    boolean existsByNomeAndIdNot(@Param("nome") String nome, @Param("id") Long id);

    Page<Salgado> findAllByAtivo(Pageable paginacao, Boolean ativo);

}
