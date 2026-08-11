package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.cliente.dto.ClienteDto;
import br.com.salgadosdalucia.api.cliente.dto.ClienteResponse;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoMapper;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse cadastrar(ClienteDto dto) {
        Cliente cliente = ClienteMapper.mapToEntity(dto);
        clienteRepository.save(cliente);
        return ClienteMapper.mapToResponse(cliente);
    }

    public Page<ClienteResponse> listarTodos(Pageable paginacao, Boolean ativo) {
        return clienteRepository.findAllByAtivo(paginacao, ativo).map(ClienteMapper::mapToResponse);
        // só se devolver dto .map(this::mapToDto); // equivalente a .map(cliente -> mapToDto(cliente))
        // map do page aplica FUNÇÃO de conversão
    }

    public ClienteResponse buscarPorId(Long id) throws NotFoundException {
        Cliente cliente = ValidacaoEntidadeHelper.buscarEntidadePorId(clienteRepository, id, "Cliente");
        return ClienteMapper.mapToResponse(cliente);
    }

    public Page<ClienteResponse> buscarPorNome(Pageable paginacao, String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(paginacao, nome).map(ClienteMapper::mapToResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse atualizar(Long id, ClienteDto dto) throws NotFoundException {
        Cliente cliente = ValidacaoEntidadeHelper.buscarEntidadePorId(clienteRepository, id, "Cliente");
        cliente.setNome(dto.nome());
        cliente.setTelefone(dto.telefone());
        cliente.setEndereco(EnderecoMapper.mapToEntity(dto.endereco()));

        return ClienteMapper.mapToResponse(cliente); // @Transactional faz o save automaticamente no final da transação
    }

    @Transactional(rollbackFor = Exception.class)
    public void atualizarStatus(Long id, AlterarStatusDto status) throws NotFoundException {
        Cliente cliente = ValidacaoEntidadeHelper.buscarEntidadePorId(clienteRepository, id, "Cliente");

        if (cliente.isAtivo() == status.status()) {
            throw new BusinessException(
                    String.format("Cliente %s já está %s.",
                            cliente.getNome(),
                            status.status() ? "ativado" : "desativado")
            );
        }

        cliente.setAtivo(status.status());
    }

}
