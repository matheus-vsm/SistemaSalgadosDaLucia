package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.cliente.dto.ClienteDto;
import br.com.salgadosdalucia.api.cliente.dto.ClienteResponse;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.shared.endereco.Endereco;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService")
class ClienteServiceTest {
    private static final Long ID = 1L;

    @Mock
    private ClienteRepository clienteRepository;
    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;
    private ClienteDto dto;
    private Pageable paginacao;


    @BeforeEach
    void setUp() {
        cliente = criarCliente(ID, "Alice", true);
        dto = criarDto();
        paginacao = PageRequest.of(0, 2);
    }

    @Nested
    @DisplayName("Cadastrar cliente")
    class Cadastrar {
        @Test
        @DisplayName("Deve salvar um novo cliente como ativo e retornar seus dados")
        void deveCadastrar() {
            ClienteResponse resposta = clienteService.cadastrar(dto);

            // captura o cliente enviado ao repositório para conferir os dados salvos
            ArgumentCaptor<Cliente> clienteEnviado = ArgumentCaptor.forClass(Cliente.class);
            verify(clienteRepository).save(clienteEnviado.capture());
            Cliente clienteSalvo = clienteEnviado.getValue();

            assertThat(clienteSalvo.getId()).isNull();
            assertThat(clienteSalvo.getNome()).isEqualTo(dto.nome());
            assertThat(clienteSalvo.getTelefone()).isEqualTo(dto.telefone());
            assertThat(clienteSalvo.isAtivo()).isTrue();
            assertThat(criarResponse(clienteSalvo).endereco()).isEqualTo(dto.endereco());
            assertThat(resposta.nome()).isEqualTo(dto.nome());
            assertThat(resposta.telefone()).isEqualTo(dto.telefone());
            assertThat(resposta.ativo()).isTrue();
            assertThat(resposta.endereco()).isEqualTo(dto.endereco());
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco no cadastro")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(clienteRepository.save(any(Cliente.class))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> clienteService.cadastrar(dto)).isSameAs(erroDoBanco);

            verify(clienteRepository).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }
    }

    @Nested
    @DisplayName("Listar todos os clientes")
    class ListarTodos {
        @Test
        @DisplayName("Deve listar os clientes ativos")
        void deveListarClientesAtivos() {
            Cliente segundoCliente = criarCliente(2L, "Luan", true);
            when(clienteRepository.findAllByAtivo(paginacao, true))
                    .thenReturn(new PageImpl<>(List.of(cliente, segundoCliente), paginacao, 5));

            Page<ClienteResponse> resposta = clienteService.listarTodos(paginacao, true);

            assertThat(resposta.getContent()).containsExactly(criarResponse(cliente), criarResponse(segundoCliente));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(clienteRepository).findAllByAtivo(paginacao, true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve listar os clientes inativos")
        void deveListarClientesInativos() {
            Cliente segundoCliente = criarCliente(2L, "Luan", false);
            Cliente terceiroCliente = criarCliente(3L, "Luana", false);
            when(clienteRepository.findAllByAtivo(paginacao, false))
                    .thenReturn(new PageImpl<>(List.of(segundoCliente, terceiroCliente), paginacao, 5));

            Page<ClienteResponse> resposta = clienteService.listarTodos(paginacao, false);

            assertThat(resposta.getContent()).containsExactly(criarResponse(segundoCliente), criarResponse(terceiroCliente));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(clienteRepository).findAllByAtivo(paginacao, false);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver clientes")
        void deveRetornarPaginaVazia() {
            when(clienteRepository.findAllByAtivo(paginacao, true)).thenReturn(Page.empty(paginacao));

            assertThat(clienteService.listarTodos(paginacao, true).getContent()).isEmpty();

            verify(clienteRepository).findAllByAtivo(paginacao, true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco na consulta por status")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(clienteRepository.findAllByAtivo(paginacao, true)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> clienteService.listarTodos(paginacao, true)).isSameAs(erroDoBanco);

            verify(clienteRepository).findAllByAtivo(paginacao, true);
            verifyNoMoreInteractions(clienteRepository);
        }
    }

    @Nested
    @DisplayName("Buscar cliente por ID")
    class BuscarPorId {
        @Test
        @DisplayName("Deve encontrar um cliente ativo pelo ID")
        void deveBuscarClienteAtivo() {
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            assertThat(clienteService.buscarPorId(ID)).isEqualTo(criarResponse(cliente));

            verify(clienteRepository).findById(ID);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve encontrar um cliente inativo pelo ID")
        void deveBuscarClienteInativo() {
            cliente.setAtivo(false);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            assertThat(clienteService.buscarPorId(ID)).isEqualTo(criarResponse(cliente));

            verify(clienteRepository).findById(ID);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve informar quando o cliente não existir")
        void deveRejeitarClienteInexistente() {
            when(clienteRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.buscarPorId(ID))
                    .isInstanceOf(NotFoundException.class).hasMessage("Cliente não encontrado com o ID: 1");

            verify(clienteRepository).findById(ID);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco na busca por ID")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(clienteRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> clienteService.buscarPorId(ID)).isSameAs(erroDoBanco);

            verify(clienteRepository).findById(ID);
            verifyNoMoreInteractions(clienteRepository);
        }
    }

    @Nested
    @DisplayName("Buscar clientes por nome")
    class BuscarPorNome {
        @Test
        @DisplayName("Deve buscar pelo nome informado e retornar os clientes encontrados")
        void deveBuscar() {
            String busca = "Ali";
            Cliente segundoCliente = criarCliente(2L, "Aline", false);
            when(clienteRepository.findByNomeContainingIgnoreCase(paginacao, busca))
                    .thenReturn(new PageImpl<>(List.of(cliente, segundoCliente), paginacao, 5));

            Page<ClienteResponse> resposta = clienteService.buscarPorNome(paginacao, busca);

            assertThat(resposta.getContent()).containsExactly(criarResponse(cliente), criarResponse(segundoCliente));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);

            verify(clienteRepository).findByNomeContainingIgnoreCase(paginacao, busca);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia quando nenhum nome corresponder")
        void deveRetornarPaginaVazia() {
            when(clienteRepository.findByNomeContainingIgnoreCase(paginacao, "inexistente"))
                    .thenReturn(Page.empty(paginacao));

            assertThat(clienteService.buscarPorNome(paginacao, "inexistente").getContent()).isEmpty();

            verify(clienteRepository).findByNomeContainingIgnoreCase(paginacao, "inexistente");
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco na consulta por nome")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(clienteRepository.findByNomeContainingIgnoreCase(paginacao, "Luis")).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> clienteService.buscarPorNome(paginacao, "Luis")).isSameAs(erroDoBanco);

            verify(clienteRepository).findByNomeContainingIgnoreCase(paginacao, "Luis");
            verifyNoMoreInteractions(clienteRepository);
        }
    }

    @Nested
    @DisplayName("Atualizar cliente")
    class Atualizar {
        @Test
        @DisplayName("Deve alterar os dados de um cliente ativo")
        void deveAtualizarClienteAtivo() {
            ClienteDto novosDados = new ClienteDto("Aline", "11988887777",
                    new EnderecoDto("Rua Nova", "20", null, "02000000", "Bairro Novo", "Sao Bernardo", "SP"));
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            ClienteResponse resposta = clienteService.atualizar(ID, novosDados);

            assertThat(resposta.id()).isEqualTo(ID);
            assertThat(resposta.nome()).isEqualTo(novosDados.nome());
            assertThat(resposta.telefone()).isEqualTo(novosDados.telefone());
            assertThat(resposta.endereco()).isEqualTo(novosDados.endereco());
            assertThat(resposta.ativo()).isTrue();
            assertThat(criarResponse(cliente)).isEqualTo(resposta);
            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve alterar os dados de um cliente inativo sem ativá-lo")
        void deveAtualizarClienteInativo() {
            cliente.setAtivo(false);
            ClienteDto novosDados = new ClienteDto("Aline", "11988887777",
                    new EnderecoDto("Rua Nova", "20", null, "02000000", "Bairro Novo", "Sao Bernardo", "SP"));
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            ClienteResponse resposta = clienteService.atualizar(ID, novosDados);

            assertThat(resposta.id()).isEqualTo(ID);
            assertThat(resposta.nome()).isEqualTo(novosDados.nome());
            assertThat(resposta.telefone()).isEqualTo(novosDados.telefone());
            assertThat(resposta.endereco()).isEqualTo(novosDados.endereco());
            assertThat(resposta.ativo()).isFalse();
            assertThat(criarResponse(cliente)).isEqualTo(resposta);
            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve rejeitar atualização de cliente inexistente")
        void deveRejeitarClienteInexistente() {
            when(clienteRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.atualizar(ID, dto))
                    .isInstanceOf(NotFoundException.class).hasMessage("Cliente não encontrado com o ID: %d", ID);

            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco ao buscar o cliente para atualização")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(clienteRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> clienteService.atualizar(ID, dto)).isSameAs(erroDoBanco);

            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }
    }

    @Nested
    @DisplayName("Atualizar status do cliente")
    class AtualizarStatus {
        @Test
        @DisplayName("Deve ativar um cliente inativo")
        void deveAtivarCliente() {
            cliente.setAtivo(false);
            ClienteResponse anterior = criarResponse(cliente);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            clienteService.atualizarStatus(ID, new AlterarStatusDto(true));

            assertThat(cliente.isAtivo()).isTrue();
            assertThat(cliente.getId()).isEqualTo(anterior.id());
            assertThat(cliente.getNome()).isEqualTo(anterior.nome());
            assertThat(cliente.getTelefone()).isEqualTo(anterior.telefone());
            assertThat(criarResponse(cliente).endereco()).isEqualTo(anterior.endereco());
            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve desativar um cliente ativo")
        void deveDesativarCliente() {
            ClienteResponse anterior = criarResponse(cliente);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            clienteService.atualizarStatus(ID, new AlterarStatusDto(false));

            assertThat(cliente.isAtivo()).isFalse();
            assertThat(cliente.getId()).isEqualTo(anterior.id());
            assertThat(cliente.getNome()).isEqualTo(anterior.nome());
            assertThat(cliente.getTelefone()).isEqualTo(anterior.telefone());
            assertThat(criarResponse(cliente).endereco()).isEqualTo(anterior.endereco());
            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve avisar quando o cliente já está ativo")
        void deveRejeitarClienteJaAtivo() {
            ClienteResponse anterior = criarResponse(cliente);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            assertThatThrownBy(() -> clienteService.atualizarStatus(ID, new AlterarStatusDto(true)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cliente %s já está ativado.", cliente.getNome());

            assertThat(criarResponse(cliente)).isEqualTo(anterior);
            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve avisar quando o cliente já está inativo")
        void deveRejeitarClienteJaInativo() {
            cliente.setAtivo(false);
            ClienteResponse anterior = criarResponse(cliente);
            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            assertThatThrownBy(() -> clienteService.atualizarStatus(ID, new AlterarStatusDto(false)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cliente %s já está desativado.", cliente.getNome());

            assertThat(criarResponse(cliente)).isEqualTo(anterior);
            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve rejeitar alteração de status de cliente inexistente")
        void deveRejeitarClienteInexistente() {
            when(clienteRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.atualizarStatus(ID, new AlterarStatusDto(false)))
                    .isInstanceOf(NotFoundException.class).hasMessage("Cliente não encontrado com o ID: %d", ID);

            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco ao buscar o cliente para alterar status")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(clienteRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> clienteService.atualizarStatus(ID, new AlterarStatusDto(false))).isSameAs(erroDoBanco);

            verify(clienteRepository).findById(ID);
            verify(clienteRepository, never()).save(any(Cliente.class));
            verifyNoMoreInteractions(clienteRepository);
        }
    }

    private ClienteDto criarDto() {
        return new ClienteDto("Matheus", "11999998888", criarEnderecoDto());
    }

    private EnderecoDto criarEnderecoDto() {
        return new EnderecoDto("Rua das Ruas", "10", "Casa", "01000000", "Centro", "Sao Paulo", "SP");
    }

    private Cliente criarCliente(Long id, String nome, boolean ativo) {
        return new Cliente(id, nome, "11999998888", ativo,
                new Endereco("Rua das Ruas", "10", "Casa", "01000000", "Centro", "Sao Paulo", "SP"));
    }

    private ClienteResponse criarResponse(Cliente entidadeCliente) {
        Endereco endereco = entidadeCliente.getEndereco();
        return new ClienteResponse(entidadeCliente.getId(), entidadeCliente.getNome(), entidadeCliente.getTelefone(), entidadeCliente.isAtivo(),
                new EnderecoDto(endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(),
                        endereco.getCep(), endereco.getBairro(), endereco.getCidade(), endereco.getUf()));
    }

}
