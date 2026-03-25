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
    public Long id;
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    public Cliente cliente;
    @OneToMany
    public List<ItemPedido> itens;
    @Embedded
    public Endereco enderecoEntrega;
    public Double valorTotal;
    public LocalDate dataPedido;
    public LocalDateTime dataEntrega;
    @Enumerated(EnumType.STRING)
    public StatusPedido status;
    @Enumerated(EnumType.STRING)
    public TipoEntrega tipoEntrega;
    @Enumerated(EnumType.STRING)
    public FormaPagamento formaPagamento;
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    public Usuario usuarioResponsavel;

    @PrePersist // JPA lifecycle callback to set the order date before saving
    public void gerarData() {
        this.dataPedido = (this.dataPedido == null) ? LocalDate.now() : this.dataPedido;
    }

    public void calcularValorTotal() {
        if (itens == null) {
            this.valorTotal = 0.0;
            return;
        }

        this.valorTotal = itens.stream()
                .filter(item -> item.getSubTotal() != null)
                .mapToDouble(ItemPedido::getSubTotal)
                .sum();
    }

    public void defineEnderecoEntrega() {
        if (this.tipoEntrega == TipoEntrega.RETIRADA) {
            this.enderecoEntrega = null; // Retirada não tem endereço de entrega
        } else if (this.tipoEntrega == TipoEntrega.ENTREGA && this.enderecoEntrega == null) {
            // Se for entrega e o endereço não foi definido, pode-se lançar uma exceção ou definir um endereço padrão
            this.enderecoEntrega = cliente.getEndereco();
        }
    }

}
