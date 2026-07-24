package br.com.salgadosdalucia.api.shared.helper;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public class ValidacaoEntidadeHelper {

    public static <T> T buscarEntidadePorId(JpaRepository<T, Long> repository, Long id, String nomeEntidade) throws NotFoundException {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("%s não encontrado com o ID: %s", nomeEntidade, id)));
    }

    public static void validarStatusAtivo(boolean ativo, Long id, String nomeEntidade, String acao) {
        if (!ativo) {
            throw new BusinessException(
                    String.format("%s com ID %d está inativo e %s.", nomeEntidade, id, acao));
        }
    }

}
