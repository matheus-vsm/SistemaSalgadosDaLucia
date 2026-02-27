package br.com.salgadosdalucia.api.model;

import br.com.salgadosdalucia.api.enums.StatusPedido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    public Cliente cliente;
    @OneToMany
    public List<ItemPedido> itens;
    public Endereco enderecoEntrega;
    public Double valorTotal;
    public LocalDate dataPedido;
    public LocalDateTime dataEntrega;
    @Enumerated(EnumType.STRING)
    public StatusPedido status;

    @PrePersist // JPA lifecycle callback to set the order date before saving
    public void gerarData() {
        this.dataPedido = (this.dataPedido == null) ? LocalDate.now() : this.dataPedido;
    }

}
