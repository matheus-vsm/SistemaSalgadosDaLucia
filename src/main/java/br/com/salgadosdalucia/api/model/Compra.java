package br.com.salgadosdalucia.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "compras")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
//    @OneToMany
//    public List<ItemCompra> itens;
    public Double valorTotal;
    public LocalDate dataCompra;

}
