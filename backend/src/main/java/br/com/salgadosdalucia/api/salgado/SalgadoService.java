package br.com.salgadosdalucia.api.salgado;

import br.com.salgadosdalucia.api.estoque.Estoque;
import br.com.salgadosdalucia.api.estoque.EstoqueRepository;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoRequest;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoResponse;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalgadoService {

    private final SalgadoRepository salgadoRepository;

    private final EstoqueRepository estoqueRepository;

    @Transactional(rollbackFor = Exception.class)
    public SalgadoResponse cadastrar(SalgadoRequest salgado) {
        Salgado novoSalgado = salgadoRepository.save(SalgadoMapper.mapToEntity(salgado));

        Estoque estoque = Estoque.builder()
                .salgado(novoSalgado)
                .quantidade(0)
                .build();
        Estoque novoEstoque = estoqueRepository.save(estoque);

        return SalgadoMapper.mapToResponse(novoSalgado, novoEstoque);
    }

    public Page<SalgadoResponse> listarSalgados(Pageable paginacao, Boolean ativo) {
        return mapearComEstoque(salgadoRepository.findAllByAtivo(paginacao, ativo));
    }

    public SalgadoResponse buscarPorId(Long id) throws NotFoundException {
        Salgado salgado = ValidacaoEntidadeHelper.buscarEntidadePorId(salgadoRepository, id, "Salgado");
        Estoque estoque = estoqueRepository.findBySalgadoId(id).orElse(null);
        return SalgadoMapper.mapToResponse(salgado, estoque);
    }

    public Page<SalgadoResponse> buscarPorNome(Pageable paginacao, String nome) {
        return mapearComEstoque(salgadoRepository.findByNomeContainingIgnoreCase(paginacao, nome));
    }

    @Transactional(rollbackFor = Exception.class)
    public SalgadoResponse atualizar(Long id, SalgadoRequest dto) throws NotFoundException {
        Salgado salgado = ValidacaoEntidadeHelper.buscarEntidadePorId(salgadoRepository, id, "Salgado");

        salgado.setNome(dto.nome());
        salgado.setDescricao(dto.descricao());
        salgado.setCategoria(dto.categoria());
        salgado.setPrecoCentoCongelado(dto.precoCentoCongelado());
        salgado.setPrecoCentoProcessado(dto.precoCentoProcessado());
        Estoque estoque = estoqueRepository.findBySalgadoId(id).orElse(null);

        return SalgadoMapper.mapToResponse(salgado, estoque);
    }

    @Transactional(rollbackFor = Exception.class)
    public void atualizarStatus(Long id, AlterarStatusDto status) throws NotFoundException, BadRequestException {
        Salgado salgado = ValidacaoEntidadeHelper.buscarEntidadePorId(salgadoRepository, id, "Salgado");

        if (salgado.isAtivo() == status.status()) {
            throw new BadRequestException(String.format("Salgado %s já está %s.", salgado.getNome(),
                    status.status() ? "ativo" : "inativo"));
        }

        salgado.setAtivo(status.status());
    }

    private Page<SalgadoResponse> mapearComEstoque(Page<Salgado> salgados) {
        var salgadosIds = salgados.getContent().stream()
                .map(Salgado::getId)
                .toList();

        Map<Long, Estoque> estoquesPorSalgado = salgadosIds.isEmpty()
                ? Map.of()
                : estoqueRepository.findAllBySalgadoIdIn(salgadosIds).stream()
                .collect(Collectors.toMap(
                        estoque -> estoque.getSalgado().getId(),
                        Function.identity())); // significa “use o próprio objeto recebido”. É equivalente a estoque -> estoque
        // estoque)); // tbm poderia ser

        return salgados.map(salgado -> SalgadoMapper.mapToResponse(
                salgado, estoquesPorSalgado.get(salgado.getId())));
    }

}
