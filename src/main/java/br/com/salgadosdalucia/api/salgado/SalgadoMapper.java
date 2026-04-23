package br.com.salgadosdalucia.api.salgado;

import br.com.salgadosdalucia.api.salgado.dto.SalgadoDto;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoResponse;

public class SalgadoMapper {

    public static SalgadoDto mapToDto(Salgado salgado) {
        return new SalgadoDto(salgado.getNome(), salgado.getDescricao(), salgado.getCategoria(),
                salgado.getPrecoCentoCongelado(), salgado.getPrecoCentoProcessado());
    }

    public static SalgadoResponse mapToResponse(Salgado salgado) {
        return new SalgadoResponse(salgado.getId(), salgado.getNome(), salgado.getDescricao(), salgado.getCategoria(),
                salgado.getPrecoCentoCongelado(), salgado.getPrecoCentoProcessado(), salgado.isAtivo());
    }

    public static Salgado mapToEntity(SalgadoDto dto) {
        return new Salgado(null, dto.nome(), dto.descricao(), dto.categoria(),
                dto.precoCentoCongelado(), dto.precoCentoProcessado(), true);
    }

}
