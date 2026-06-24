package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.cliente.Cliente;
import br.com.salgadosdalucia.api.pedido.enums.FormaPagamento;
import br.com.salgadosdalucia.api.pedido.enums.StatusPedido;
import br.com.salgadosdalucia.api.pedido.enums.TipoEntrega;
import br.com.salgadosdalucia.api.shared.endereco.Endereco;
import br.com.salgadosdalucia.api.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido",
            cascade = CascadeType.ALL, // propaga TODAS as operações (salvar, atualizar, deletar, etc.) do Pedido para ItemPedido automaticamente
            orphanRemoval = true) // remove do banco qualquer ItemPedido que for removido da lista "itens" do Pedido
    private List<ItemPedido> itens;

    @Embedded
    private Endereco enderecoEntrega;

    private BigDecimal valorTotal;
    private LocalDate dataPedido;
    private LocalDateTime dataEntrega;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    @Enumerated(EnumType.STRING)
    private TipoEntrega tipoEntrega;

    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuarioResponsavel;

    private BigDecimal frete;

    @PrePersist // É chamado antes de salvar um objeto novo no banco
    public void gerarData() {
        if (this.dataPedido == null) this.dataPedido = LocalDate.now();
    }

}
