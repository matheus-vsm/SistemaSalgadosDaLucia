package br.com.salgadosdalucia.api.enums;

import br.com.salgadosdalucia.api.pedido.enums.FormaPagamento;
import br.com.salgadosdalucia.api.pedido.enums.StatusPedido;
import br.com.salgadosdalucia.api.pedido.enums.TipoEntrega;
import br.com.salgadosdalucia.api.pedido.enums.TipoPreco;
import br.com.salgadosdalucia.api.perfil.PerfilUsuarioNome;
import br.com.salgadosdalucia.api.salgado.enums.Categoria;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class EnumService {

    public Map<String, List<EnumDTO>> listarEnums(String entidade) {
        return switch (entidade.toUpperCase()) {
            case "PEDIDO" -> Map.of(
                    "formasPagamento", converterEnum(FormaPagamento.values()),
                    "tiposEntrega", converterEnum(TipoEntrega.values()),
                    "statusPedido", converterEnum(StatusPedido.values()),
                    "tiposPrecos", converterEnum(TipoPreco.values())
            );

            case "SALGADO" -> Map.of(
                    "categorias", converterEnum(Categoria.values())
            );

            case "USUARIO" -> Map.of(
                    "perfis", converterEnum(PerfilUsuarioNome.values())
            );

            default -> throw new RuntimeException("Entidade não encontrada");
        };
    }

    private List<EnumDTO> converterEnum(EnumDescritivo[] valores) {
        return Arrays.stream(valores)
                .map(valor -> new EnumDTO(
                        ((Enum<?>) valor).name(),
                        valor.getDescricao()
                ))
                .toList();
    }

}
