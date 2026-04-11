package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.dto.AlterarStatusDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Validated
public class ClienteController {

    private final ClienteService service;

    @PostMapping
    public ResponseEntity<Cliente> cadastrarCliente(@Valid @RequestBody ClienteDto dto, UriComponentsBuilder uriBuilder) {
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
    public ResponseEntity<Cliente> buscarClientePorId(@PathVariable Long id) throws NotFoundException {
        Cliente cliente = service.buscarPorId(id);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<List<Cliente>> buscarClientePorNome(@PathVariable String nome) {
        List<Cliente> cliente = service.buscarPorNome(nome);
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizarCliente(@Valid @PathVariable Long id, @RequestBody ClienteDto dto) throws NotFoundException {
        Cliente cliente = service.atualizar(id, dto);
        return ResponseEntity.ok(cliente);
    }

    @PatchMapping("/atualizar-status/{id}")
    public ResponseEntity<Void> alterarStatusCliente(@Valid @PathVariable Long id, @RequestBody AlterarStatusDto status)
            throws BadRequestException, NotFoundException {
        service.atualizarStatus(id, status);
        return ResponseEntity.noContent().build();
    }

}
