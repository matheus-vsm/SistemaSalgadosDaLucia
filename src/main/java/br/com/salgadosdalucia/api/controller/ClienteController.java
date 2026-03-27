package br.com.salgadosdalucia.api.controller;

import br.com.salgadosdalucia.api.dto.ClienteDto;
import br.com.salgadosdalucia.api.model.Cliente;
import br.com.salgadosdalucia.api.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @PostMapping
    @Transactional
    public ResponseEntity<Cliente> novoCliente(@RequestBody ClienteDto dto, UriComponentsBuilder uriBuilder) {
        Cliente cliente = service.cadastrar(dto);
        URI uri = uriBuilder.path("/clientes/{id}").buildAndExpand(cliente.getId()).toUri();

        return ResponseEntity.created(uri).body(cliente);
    }

}
