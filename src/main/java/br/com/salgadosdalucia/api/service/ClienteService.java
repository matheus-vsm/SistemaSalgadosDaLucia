package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.dto.ClienteDto;
import br.com.salgadosdalucia.api.model.Cliente;
import br.com.salgadosdalucia.api.repositoy.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;

    //public Optional<Cliente> findById(Long id) {}

    public Cliente cadastrar(ClienteDto dto) {
        Cliente cliente = mapToEntity(dto);
        return repository.save(cliente);
    }

    public Page<Cliente> listarTodos(Pageable paginacao) {
        return repository.findAllByAtivoTrue(paginacao);
                // só se devolver dto .map(this::mapToDto); // equivalente a .map(cliente -> mapToDto(cliente))
        // map do page aplica FUNÇÃO de conversão
    }

    public Cliente buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }

    public List<Cliente> buscarPorNome(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome);
    }

    public Cliente atualizar(Long id, ClienteDto dto) {
        Cliente cliente = repository.findById(id).orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        cliente.setNome(dto.nome());
        cliente.setTelefone(dto.telefone());
        cliente.setEndereco(dto.endereco());

        return repository.save(cliente);
    }

    public void desativar(Long id) {
        Cliente cliente = repository.findById(id).orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        cliente.setAtivo(false);
        repository.save(cliente);
    }

    private ClienteDto mapToDto(Cliente cliente) {
        return new ClienteDto(cliente.getNome(), cliente.getTelefone(), cliente.getEndereco());
    }

    private Cliente mapToEntity(ClienteDto dto) {
        return new Cliente(null, dto.nome(), dto.telefone(), true, dto.endereco());
    }

}
