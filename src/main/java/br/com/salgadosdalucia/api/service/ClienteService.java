package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.dto.ClienteDto;
import br.com.salgadosdalucia.api.dto.EnderecoDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.model.Cliente;
import br.com.salgadosdalucia.api.model.Endereco;
import br.com.salgadosdalucia.api.repositoy.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(rollbackFor = Exception.class)
    public Cliente cadastrar(ClienteDto dto) {
        Cliente cliente = mapToEntity(dto);
        return clienteRepository.save(cliente);
    }

    public Page<Cliente> listarTodos(Pageable paginacao) {
        return clienteRepository.findAllByAtivoTrue(paginacao);
        // só se devolver dto .map(this::mapToDto); // equivalente a .map(cliente -> mapToDto(cliente))
        // map do page aplica FUNÇÃO de conversão
    }

    public Cliente buscarPorId(Long id) throws NotFoundException {
        return clienteRepository.findById(id).orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
    }

    public List<Cliente> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome);
    }

    @Transactional(rollbackFor = Exception.class)
    public Cliente atualizar(Long id, ClienteDto dto) throws NotFoundException {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
        cliente.setNome(dto.nome());
        cliente.setTelefone(dto.telefone());
        cliente.setEndereco(mapToEndereco(dto.endereco()));

        return clienteRepository.save(cliente);
    }

    @Transactional(rollbackFor = Exception.class)
    public void desativar(Long id) throws BadRequestException, NotFoundException {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
        if (!cliente.isAtivo()) {
            throw new BadRequestException(String.format("Cliente %s já está desativado.", cliente.getNome()));
        }
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    private ClienteDto mapToDto(Cliente cliente) {
        return new ClienteDto(cliente.getNome(), cliente.getTelefone(), mapToEnderecoDto(cliente.getEndereco()));
    }

    private Cliente mapToEntity(ClienteDto dto) {
        return new Cliente(null, dto.nome(), dto.telefone(), true, mapToEndereco(dto.endereco()));
    }

    private Endereco mapToEndereco(EnderecoDto dto) {
        return new Endereco(dto.logradouro(), dto.numero(), dto.complemento(), dto.cep(),
                dto.bairro(), dto.cidade(), dto.uf());
    }

    private EnderecoDto mapToEnderecoDto(Endereco endereo) {
        return new EnderecoDto(endereo.getLogradouro(), endereo.getNumero(), endereo.getComplemento(),
                endereo.getCep(), endereo.getBairro(), endereo.getCidade(), endereo.getUf());
    }

}
