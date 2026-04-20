package br.com.salgadosdalucia.api.pedido;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    //    Page<Pedido> findAllByStatus(StatusPedido statusPedido, Pageable paginacao);
    @Query("""
            SELECT p FROM Pedido p
            WHERE (:statusPedido IS NULL OR p.status = :statusPedido)
            AND (:clienteId IS NULL OR p.cliente.id = :clienteId)
            AND (:nomeCliente IS NULL OR UPPER(p.cliente.nome) LIKE UPPER(CONCAT('%', :nomeCliente, '%')))
            AND (:dataPedido IS NULL OR p.dataPedido = :dataPedido)
            AND (:inicioEntrega IS NULL OR p.dataEntrega >= :inicioEntrega)
            AND (:fimEntrega IS NULL OR p.dataEntrega <= :fimEntrega)
            AND (:tipoEntrega IS NULL OR p.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento)
            AND (:usuarioResponsavelId IS NULL OR p.usuarioResponsavel.id = :usuarioResponsavelId)
            AND (:nomeUsuarioResponsavel IS NULL OR UPPER(p.usuarioResponsavel.nome) LIKE UPPER(CONCAT('%', :nomeUsuarioResponsavel, '%')))
            """)
    Page<Pedido> findWithFiltros(StatusPedido statusPedido, Long clienteId, String nomeCliente, LocalDate dataPedido,
                                 LocalDateTime inicioEntrega, LocalDateTime fimEntrega,
                                 TipoEntrega tipoEntrega, FormaPagamento formaPagamento,
                                 Long usuarioResponsavelId, String nomeUsuarioResponsavel, Pageable paginacao);
    // ACIMA adicionar @Param(ex: "statusPedido") para evitar que o hibernate se perca e garantir a injeção correta dos parâmetros

}
