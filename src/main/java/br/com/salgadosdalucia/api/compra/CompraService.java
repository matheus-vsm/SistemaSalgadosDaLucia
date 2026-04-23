package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CompraFiltroDto;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraRequest;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraResponse;
import br.com.salgadosdalucia.api.compra.dto.ItemCompraRequest;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;

    @Transactional(rollbackFor = Exception.class)
    public CriacaoCompraResponse registrarCompra(CriacaoCompraRequest request) {
        Compra compra = Compra.builder()
                .dataCompra(request.dataCompra())
                .observacao(request.observacao())
                .build();
        List<ItemCompra> itens = new ArrayList<>();

        for (ItemCompraRequest itemRequest : request.itens()) {
            ItemCompra item = new ItemCompra();
            item.setCompra(compra);
            item.setNome(itemRequest.nome());
            item.setQuantidade(itemRequest.quantidade());
            item.setPrecoUnitario(itemRequest.precoUnitario());
            calcularSubTotal(item);

            itens.add(item);
        }

        compra.setItens(itens);
        calcularValorTotal(compra);

        compraRepository.save(compra);

        return CompraMapper.mapToResponse(compra);
    }

    private void calcularValorTotal(Compra compra) {
        if (compra.getItens().isEmpty()) {
            compra.setValorTotal(BigDecimal.ZERO);
            return;
        }

        compra.setValorTotal(compra.getItens().stream()
                .filter(item -> item.getSubTotal() != null)
                .map(ItemCompra::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void calcularSubTotal(ItemCompra item) {
        item.setSubTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
    }

    public Page<CriacaoCompraResponse> listarComFiltro(CompraFiltroDto filtro, Pageable paginacao) {
        return compraRepository.findAllWithFiltros(filtro.dataCompra(), filtro.dataInicioCompra(),
                        filtro.dataFimCompra(), filtro.nomeItem(), filtro.observacao(), paginacao)
                .map(CompraMapper::mapToResponse);
    }

    public CriacaoCompraResponse buscarPorId(Long id) throws NotFoundException {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compra não encontrada com id: " + id));
        return CompraMapper.mapToResponse(compra);
    }

}
