package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.cliente.dto.ClienteDto;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoMapper;
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
        Cliente cliente = ClienteMapper.mapToEntity(dto);
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
        cliente.setEndereco(EnderecoMapper.mapToEntity(dto.endereco()));

        return cliente; // @Transactional faz o save automaticamente no final da transação
    }

    @Transactional(rollbackFor = Exception.class)
    public void atualizarStatus(Long id, AlterarStatusDto status) throws BadRequestException, NotFoundException {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        if (cliente.isAtivo() == status.status()) {
            throw new BadRequestException(
                    String.format("Cliente %s já está %s.",
                            cliente.getNome(),
                            status.status() ? "ativado" : "desativado")
            );
        }

        cliente.setAtivo(status.status());
    }

}
