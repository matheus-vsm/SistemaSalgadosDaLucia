package br.com.salgadosdalucia.api.pedido;

import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
@NoArgsConstructor
@Validated
public class PedidoController {
}
