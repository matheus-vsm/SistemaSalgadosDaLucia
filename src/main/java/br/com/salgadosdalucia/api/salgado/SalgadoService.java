package br.com.salgadosdalucia.api.salgado;

import br.com.salgadosdalucia.api.estoque.Estoque;
import br.com.salgadosdalucia.api.estoque.EstoqueRepository;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoDto;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoResponse;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalgadoService {

    private final SalgadoRepository salgadoRepository;

    private final EstoqueRepository estoqueRepository;

    @Transactional(rollbackFor = Exception.class)
    public SalgadoResponse cadastrar(SalgadoDto salgado) {
        Salgado novoSalgado = salgadoRepository.save(SalgadoMapper.mapToEntity(salgado));

        Estoque estoque = Estoque.builder()
                .salgado(novoSalgado)
                .quantidade(0)
                .build();
        estoqueRepository.save(estoque);

        return SalgadoMapper.mapToResponse(novoSalgado);
    }

    public Page<Salgado> listarSalgados(Pageable paginacao) {
        return salgadoRepository.findAllByAtivoTrue(paginacao);
    }

    public Salgado buscarPorId(Long id) throws NotFoundException {
        Salgado salgado = salgadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Salgado não encontrado"));
        return salgado;
    }

    public List<Salgado> buscarPorNome(String nome) {
        return salgadoRepository.findByNomeContainingIgnoreCase(nome);
    }

    @Transactional(rollbackFor = Exception.class)
    public Salgado atualizar(Long id, SalgadoDto dto) throws NotFoundException {
        Salgado salgado = salgadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Salgado não encontrado"));

        salgado.setNome(dto.nome());
        salgado.setDescricao(dto.descricao());
        salgado.setCategoria(dto.categoria());
        salgado.setPrecoCentoCongelado(dto.precoCentoCongelado());
        salgado.setPrecoCentoProcessado(dto.precoCentoProcessado());

        return salgado;
    }

    @Transactional(rollbackFor = Exception.class)
    public void atualizarStatus(Long id, AlterarStatusDto status) throws NotFoundException, BadRequestException {
        Salgado salgado = salgadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Salgado não encontrado"));

        if (salgado.isAtivo() == status.status()) {
            throw new BadRequestException(String.format("Salgado %s já está %s.", salgado.getNome(),
                    status.status() ? "ativo" : "inativo"));
        }

        salgado.setAtivo(status.status());
    }
}
