package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.cliente.dto.ClienteDto;
import br.com.salgadosdalucia.api.cliente.dto.ClienteResponse;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<ClienteResponse> cadastrarCliente(@Valid @RequestBody ClienteDto dto, UriComponentsBuilder uriBuilder) {
        ClienteResponse cliente = service.cadastrar(dto);
        URI uri = uriBuilder.path("/clientes/{id}").buildAndExpand(cliente.id()).toUri();

        return ResponseEntity.created(uri).body(cliente);
    }

    @GetMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<Page<ClienteResponse>> listarClientes(@PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao) {
        var page = service.listarTodos(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<ClienteResponse> buscarClientePorId(@PathVariable Long id) throws NotFoundException {
        ClienteResponse cliente = service.buscarPorId(id);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/nome")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<Page<ClienteResponse>> buscarClientePorNome(@PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao, @RequestParam String nome) {
        var page = service.buscarPorNome(paginacao, nome);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<ClienteResponse> atualizarCliente(@Valid @PathVariable Long id, @RequestBody ClienteDto dto) throws NotFoundException {
        ClienteResponse cliente = service.atualizar(id, dto);
        return ResponseEntity.ok(cliente);
    }

    @PatchMapping("/atualizar-status/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<Void> alterarStatusCliente(@Valid @PathVariable Long id, @RequestBody AlterarStatusDto status)
            throws NotFoundException {
        service.atualizarStatus(id, status);
        return ResponseEntity.noContent().build();
    }

}
