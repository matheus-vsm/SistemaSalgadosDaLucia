package br.com.salgadosdalucia.api.controller;

import br.com.salgadosdalucia.api.dto.ClienteDto;
import br.com.salgadosdalucia.api.model.Cliente;
import br.com.salgadosdalucia.api.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @PostMapping
    @Transactional
    public ResponseEntity<Cliente> cadastrarCliente(@RequestBody ClienteDto dto, UriComponentsBuilder uriBuilder) {
        Cliente cliente = service.cadastrar(dto);
        URI uri = uriBuilder.path("/clientes/{id}").buildAndExpand(cliente.getId()).toUri();

        return ResponseEntity.created(uri).body(cliente);
    }

    @GetMapping
    public ResponseEntity<Page<Cliente>> listarClientes(@PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao) {
        var page = service.listarTodos(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarClientePorId(@PathVariable Long id) {
        var cliente = service.buscarPorId(id);
        return ResponseEntity.ok(cliente);
    }

//    @GetMapping("/{nome}")
//    public ResponseEntity<List<Cliente>> buscarClientePorNome(@PathVariable String nome) {
//        var cliente = service.buscarPorNome(nome);
//        return ResponseEntity.ok(cliente);
//    }

}
