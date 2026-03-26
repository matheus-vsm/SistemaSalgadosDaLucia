package br.com.salgadosdalucia.api.model;

import br.com.salgadosdalucia.api.model.enums.FormaPagamento;
import br.com.salgadosdalucia.api.model.enums.StatusPedido;
import br.com.salgadosdalucia.api.model.enums.TipoEntrega;
import jakarta.persistence.*;
import lombok.*;

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
    @OneToMany(mappedBy = "pedido")
    private List<ItemPedido> itens;
    @Embedded
    private Endereco enderecoEntrega;
    private Double valorTotal;
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

    @PrePersist // É chamado antes de salvar um objeto novo no banco
    public void gerarData() {
        this.dataPedido = LocalDate.now();
    }

}
