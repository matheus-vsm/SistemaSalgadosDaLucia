package br.com.salgadosdalucia.api.estoque;

import br.com.salgadosdalucia.api.estoque.dto.AtualizarQuantidadeRequest;
import br.com.salgadosdalucia.api.estoque.dto.EstoqueListagemDto;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;

    @Transactional(rollbackFor = Exception.class)
    public void atualizarQuantidade(Long id, AtualizarQuantidadeRequest request) throws NotFoundException {
        Estoque estoque = ValidacaoEntidadeHelper.buscarEntidadePorId(estoqueRepository, id, "Estoque");

        if (request.quantidade() + estoque.getQuantidade() < 0) {
            throw new BusinessException("A quantidade no estoque não pode ser negativa.");
        }

        estoque.setQuantidade(estoque.getQuantidade() + request.quantidade());
    }

    public Page<EstoqueListagemDto> listar(Pageable paginacao) {
        return estoqueRepository.findAllSalgadosAtivos(paginacao).map(EstoqueMapper::mapToDto);
    }

    public EstoqueListagemDto buscarPorId(Long salgadoId) throws NotFoundException {
        Estoque estoque = ValidacaoEntidadeHelper.buscarEntidadePorId(estoqueRepository, salgadoId, "Estoque");
        if (!estoque.getSalgado().isAtivo()) {
            throw new NotFoundException("Salgado com id: " + salgadoId + " está inativo, estoque não disponível.");
        }
        return EstoqueMapper.mapToDto(estoque);
    }

}
