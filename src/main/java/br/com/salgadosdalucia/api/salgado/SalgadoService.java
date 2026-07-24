package br.com.salgadosdalucia.api.salgado;

import br.com.salgadosdalucia.api.estoque.Estoque;
import br.com.salgadosdalucia.api.estoque.EstoqueRepository;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoRequest;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoResponse;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        estoqueRepository.save(estoque);

        return SalgadoMapper.mapToResponse(novoSalgado);
    }

    public Page<SalgadoResponse> listarSalgados(Pageable paginacao) {
        return salgadoRepository.findAllByAtivoTrue(paginacao).map(SalgadoMapper::mapToResponse);
    }

    public SalgadoResponse buscarPorId(Long id) throws NotFoundException {
        Salgado salgado = ValidacaoEntidadeHelper.buscarEntidadePorId(salgadoRepository, id, "Salgado");
        return SalgadoMapper.mapToResponse(salgado);
    }

    public Page<SalgadoResponse> buscarPorNome(Pageable paginacao, String nome) {
        return salgadoRepository.findByNomeContainingIgnoreCase(paginacao, nome)
                .map(SalgadoMapper::mapToResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public SalgadoResponse atualizar(Long id, SalgadoRequest dto) throws NotFoundException {
        Salgado salgado = ValidacaoEntidadeHelper.buscarEntidadePorId(salgadoRepository, id, "Salgado");

        salgado.setNome(dto.nome());
        salgado.setDescricao(dto.descricao());
        salgado.setCategoria(dto.categoria());
        salgado.setPrecoCentoCongelado(dto.precoCentoCongelado());
        salgado.setPrecoCentoProcessado(dto.precoCentoProcessado());

        return SalgadoMapper.mapToResponse(salgado);
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
}
