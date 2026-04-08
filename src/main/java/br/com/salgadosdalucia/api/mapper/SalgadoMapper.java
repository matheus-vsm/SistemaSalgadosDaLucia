package br.com.salgadosdalucia.api.mapper;

import br.com.salgadosdalucia.api.dto.SalgadoDto;
import br.com.salgadosdalucia.api.model.Salgado;

public class SalgadoMapper {

    public static SalgadoDto mapToDto(Salgado salgado) {
        return new SalgadoDto(salgado.getNome(), salgado.getDescricao(), salgado.getCategoria(),
                salgado.getPrecoCentoCongelado(), salgado.getPrecoCentoProcessado());
    }

    public static Salgado mapToEntity(SalgadoDto dto) {
        return new Salgado(null, dto.nome(), dto.descricao(), dto.categoria(),
                dto.precoCentoCongelado(), dto.precoCentoProcessado(), true);
    }

}
