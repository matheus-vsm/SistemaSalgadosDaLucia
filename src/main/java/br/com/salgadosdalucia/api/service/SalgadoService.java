package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.dto.SalgadoDto;
import br.com.salgadosdalucia.api.mapper.SalgadoMapper;
import br.com.salgadosdalucia.api.model.Salgado;
import br.com.salgadosdalucia.api.repositoy.SalgadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalgadoService {

    private final SalgadoRepository salgadoRepository;

    @Transactional(rollbackFor = Exception.class)
    public Salgado cadastrar(SalgadoDto salgado) {
        Salgado novoSalgado = SalgadoMapper.mapToEntity(salgado);
        return salgadoRepository.save(novoSalgado);
    }
    
}
