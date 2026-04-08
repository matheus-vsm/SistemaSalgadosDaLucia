package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.dto.AlterarStatusDto;
import br.com.salgadosdalucia.api.dto.SalgadoDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.mapper.SalgadoMapper;
import br.com.salgadosdalucia.api.model.Salgado;
import br.com.salgadosdalucia.api.repositoy.SalgadoRepository;
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

    @Transactional(rollbackFor = Exception.class)
    public Salgado cadastrar(SalgadoDto salgado) {
        Salgado novoSalgado = SalgadoMapper.mapToEntity(salgado);
        return salgadoRepository.save(novoSalgado);
    }

    public Page<Salgado> listarSalgados(Pageable paginacao) {
        return salgadoRepository.findAllByAtivoTrue(paginacao);
    }

    public Salgado buscarPorId(Long id) throws NotFoundException {
        return salgadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Salgado não encontrado"));
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
