package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Validated
public class PedidoController {

    private final PedidoService service;

    @PostMapping
    public ResponseEntity<PedidoResponse> cadastrarPedido(@RequestBody @Valid PedidoRequest request, UriComponentsBuilder uriBuilder) throws BadRequestException {
        PedidoResponse pedido = service.cadastrar(request);
        URI uri = uriBuilder.path("/pedidos/{id}").buildAndExpand(pedido.id()).toUri();
        return ResponseEntity.created(uri).body(pedido);
    }

//    @GetMapping
//    public ResponseEntity<Pedido> listarTodos(){
//
//    }

}
