package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.cliente.dto.ClienteDto;
import br.com.salgadosdalucia.api.cliente.dto.ClienteResponse;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.shared.endereco.Endereco;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
                Cliente novoCliente = invocation.getArgument(0);
                assertThat(novoCliente.getId()).isNull();
                novoCliente.setId(ID);
                return novoCliente;
            });

            ClienteResponse resposta = clienteService.cadastrar(dto);

            // captura o cliente enviado ao repositório para conferir os dados salvos
            ArgumentCaptor<Cliente> clienteEnviado = ArgumentCaptor.forClass(Cliente.class);
            verify(clienteRepository).save(clienteEnviado.capture());
            Cliente clienteSalvo = clienteEnviado.getValue();

            assertThat(clienteSalvo.getId()).isEqualTo(ID);
            assertThat(clienteSalvo.getNome()).isEqualTo(dto.nome());
            assertThat(clienteSalvo.getTelefone()).isEqualTo(dto.telefone());
            assertThat(clienteSalvo.isAtivo()).isTrue();
            assertThat(criarResponse(clienteSalvo).endereco()).isEqualTo(dto.endereco());
            assertThat(resposta.id()).isEqualTo(ID);
            assertThat(resposta.nome()).isEqualTo(dto.nome());
            assertThat(resposta.telefone()).isEqualTo(dto.telefone());
            assertThat(resposta.ativo()).isTrue();
            assertThat(resposta.endereco()).isEqualTo(dto.endereco());
            verifyNoMoreInteractions(clienteRepository);
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @DisplayName("Deve cadastrar endereço sem complemento e retornar o ID atribuído")
        void deveCadastrarSemComplemento(String complemento) {
            EnderecoDto endereco = new EnderecoDto("Rua Aurora", "25", complemento,
                    "30110010", "Funcionários", "Belo Horizonte", "MG");
            ClienteDto dados = new ClienteDto("Beatriz", "31987654321", endereco);
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
                Cliente novoCliente = invocation.getArgument(0);
                novoCliente.setId(37L);
                return novoCliente;
            });

            ClienteResponse resposta = clienteService.cadastrar(dados);

            assertThat(resposta).isEqualTo(new ClienteResponse(37L, "Beatriz", "31987654321", true, endereco));
            ArgumentCaptor<Cliente> enviado = ArgumentCaptor.forClass(Cliente.class);
            verify(clienteRepository).save(enviado.capture());
            assertThat(enviado.getValue().getEndereco().getComplemento()).isEqualTo(complemento);
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

            Page<ClienteResponse> resposta = clienteService.listarTodos(paginacao, true);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            assertThat(resposta.getTotalPages()).isZero();

            verify(clienteRepository).findAllByAtivo(paginacao, true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve preservar a ordem e os metadados de uma última página parcial")
        void deveListarPaginaPosteriorAInicial() {
            Pageable ultimaPagina = PageRequest.of(2, 2, Sort.by("nome").descending());
            when(clienteRepository.findAllByAtivo(ultimaPagina, true))
                    .thenReturn(new PageImpl<>(List.of(cliente), ultimaPagina, 5));

            Page<ClienteResponse> resposta = clienteService.listarTodos(ultimaPagina, true);

            assertThat(resposta.getContent()).containsExactly(criarResponse(cliente));
            assertThat(resposta.getPageable()).isEqualTo(ultimaPagina);
            assertThat(resposta.getNumber()).isEqualTo(2);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            assertThat(resposta.hasPrevious()).isTrue();
            assertThat(resposta.hasNext()).isFalse();
            verify(clienteRepository).findAllByAtivo(ultimaPagina, true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("Deve preservar o total quando a página solicitada estiver além dos resultados")
        void deveListarPaginaForaDosResultados(boolean ativo) {
            Pageable foraDosResultados = PageRequest.of(4, 2);
            when(clienteRepository.findAllByAtivo(foraDosResultados, ativo))
                    .thenReturn(new PageImpl<>(List.of(), foraDosResultados, 7));

            Page<ClienteResponse> resposta = clienteService.listarTodos(foraDosResultados, ativo);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(foraDosResultados);
            assertThat(resposta.getTotalElements()).isEqualTo(7);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            verify(clienteRepository).findAllByAtivo(foraDosResultados, ativo);
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
        @ParameterizedTest
        @CsvSource({"true, 1, 1, 5", "false, 1, 1, 5", "true, 3, 0, 5", "false, 3, 0, 5"})
        @DisplayName("Deve encaminhar nome e status juntos e preservar conteúdo e totais da página")
        void deveBuscarPorNomeEStatus(boolean ativo, int numero, int quantidade, long total) {
            Pageable pagina = PageRequest.of(numero, 2, Sort.by("nome"));
            cliente.setAtivo(ativo);
            List<Cliente> conteudo = quantidade == 0 ? List.of() : List.of(cliente);

            when(clienteRepository.findByNomeContainingIgnoreCaseAndAtivo(pagina, "Ali", ativo))
                    .thenReturn(new PageImpl<>(conteudo, pagina, total));

            Page<ClienteResponse> resposta = clienteService.buscarPorNome(pagina, "Ali", ativo);

            assertThat(resposta.getContent()).containsExactlyElementsOf(
                    quantidade == 0 ? List.of() : List.of(criarResponse(cliente)));
            assertThat(resposta.getPageable()).isEqualTo(pagina);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(clienteRepository).findByNomeContainingIgnoreCaseAndAtivo(pagina, "Ali", ativo);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve buscar pelo nome informado e retornar os clientes encontrados")
        void deveBuscar() {
            String busca = "Ali";
            Cliente segundoCliente = criarCliente(2L, "Aline", true);
            when(clienteRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, busca, true))
                    .thenReturn(new PageImpl<>(List.of(cliente, segundoCliente), paginacao, 5));

            Page<ClienteResponse> resposta = clienteService.buscarPorNome(paginacao, busca, true);

            assertThat(resposta.getContent()).containsExactly(criarResponse(cliente), criarResponse(segundoCliente));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);

            verify(clienteRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, busca, true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia quando nenhum nome corresponder")
        void deveRetornarPaginaVazia() {
            when(clienteRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, "inexistente", true))
                    .thenReturn(Page.empty(paginacao));

            Page<ClienteResponse> resposta = clienteService.buscarPorNome(paginacao, "inexistente", true);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            assertThat(resposta.getTotalPages()).isZero();

            verify(clienteRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, "inexistente", true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve preservar ordem e paginação dos clientes retornados na busca")
        void deveBuscarPaginaPosteriorAInicial() {
            Pageable segundaPagina = PageRequest.of(1, 2, Sort.by("nome").descending());
            Cliente primeiro = criarCliente(19L, "Marina", true);
            Cliente segundo = criarCliente(8L, "Maria", true);

            when(clienteRepository.findByNomeContainingIgnoreCaseAndAtivo(segundaPagina, "Mari", true))
                    .thenReturn(new PageImpl<>(List.of(primeiro, segundo), segundaPagina, 7));

            Page<ClienteResponse> resposta = clienteService.buscarPorNome(segundaPagina, "Mari", true);

            assertThat(resposta.getContent()).containsExactly(criarResponse(primeiro), criarResponse(segundo));
            assertThat(resposta.getPageable()).isEqualTo(segundaPagina);
            assertThat(resposta.getNumber()).isEqualTo(1);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(7);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            assertThat(resposta.hasPrevious()).isTrue();
            assertThat(resposta.hasNext()).isTrue();
            verify(clienteRepository).findByNomeContainingIgnoreCaseAndAtivo(segundaPagina, "Mari", true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve preservar os metadados da busca vazia além da última página")
        void deveBuscarPaginaForaDosResultados() {
            Pageable foraDosResultados = PageRequest.of(4, 2);
            when(clienteRepository.findByNomeContainingIgnoreCaseAndAtivo(foraDosResultados, "Mari", true))
                    .thenReturn(new PageImpl<>(List.of(), foraDosResultados, 7));

            Page<ClienteResponse> resposta = clienteService.buscarPorNome(foraDosResultados, "Mari", true);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(foraDosResultados);
            assertThat(resposta.getTotalElements()).isEqualTo(7);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            verify(clienteRepository).findByNomeContainingIgnoreCaseAndAtivo(foraDosResultados, "Mari", true);
            verifyNoMoreInteractions(clienteRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco na consulta por nome")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(clienteRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Luis", true)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> clienteService.buscarPorNome(paginacao, "Luis", true)).isSameAs(erroDoBanco);

            verify(clienteRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Luis", true);
            verifyNoMoreInteractions(clienteRepository);
        }
    }

    @Nested
    @DisplayName("Atualizar cliente")
    class Atualizar {
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("Deve substituir todo o endereço e remover complemento sem alterar ID ou status")
        void deveSubstituirEnderecoCompleto(boolean ativo) {
            cliente.setAtivo(ativo);
            EnderecoDto novoEndereco = new EnderecoDto("Avenida Atlântica", "92B", null,
                    "22021001", "Copacabana", "Rio de Janeiro", "RJ");
            ClienteDto novosDados = new ClienteDto("Carolina", "21912345678", novoEndereco);

            when(clienteRepository.findById(ID)).thenReturn(Optional.of(cliente));

            ClienteResponse resposta = clienteService.atualizar(ID, novosDados);

            assertThat(resposta).isEqualTo(new ClienteResponse(ID, "Carolina", "21912345678", ativo, novoEndereco));
            assertThat(criarResponse(cliente)).isEqualTo(resposta);
            verify(clienteRepository).findById(ID);
            verifyNoMoreInteractions(clienteRepository);
        }

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

    @Nested
    @DisplayName("Validar dados de cadastro e atualização explicitamente, sem chamar o serviço")
    class ValidacaoCadastro {
        private static ValidatorFactory factory;
        private static Validator validator;

        @BeforeAll
        static void criarValidador() {
            factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @AfterAll
        static void fecharValidador() {
            if (factory != null) factory.close();
        }

        @Nested
        @DisplayName("Validar os dados do cliente")
        class DadosDoCliente {
            @Test
            @DisplayName("Deve aceitar nome, telefone e endereço completos")
            void deveAceitarDadosValidos() {
                assertThat(validator.validate(new ClienteDto("Luísa Rocha", "31987654321", enderecoValido()))).isEmpty();
            }

            @ParameterizedTest
            @NullSource
            @EmptySource
            @ValueSource(strings = {"   ", "\t\n"})
            @DisplayName("Deve rejeitar nome ausente ou em branco")
            void deveExigirNome(String nome) {
                assertViolacoes(new ClienteDto(nome, "31987654321", enderecoValido()), "nome", Set.of(NotBlank.class));
            }

            @ParameterizedTest
            @NullSource
            @EmptySource
            @ValueSource(strings = {"   ", "\t\n"})
            @DisplayName("Deve rejeitar telefone ausente ou em branco")
            void deveExigirTelefone(String telefone) {
                assertViolacoes(new ClienteDto("Luísa Rocha", telefone, enderecoValido()), "telefone", Set.of(NotBlank.class));
            }

            @Test
            @DisplayName("Deve rejeitar endereço nulo")
            void deveExigirEndereco() {
                assertViolacoes(new ClienteDto("Luísa Rocha", "31987654321", null), "endereco", Set.of(NotNull.class));
            }
        }

        @Nested
        @DisplayName("Validar os campos do endereço em cascata")
        class DadosDoEndereco {
            @ParameterizedTest(name = "[{index}] Campo obrigatório: {0}, valor: [{1}]")
            @MethodSource("br.com.salgadosdalucia.api.cliente.ClienteServiceTest$ValidacaoCadastro#camposObrigatoriosInvalidos")
            @DisplayName("Deve rejeitar campos obrigatórios do endereço ausentes ou em branco")
            void deveValidarEnderecoEmCascata(String campo, String valor) {
                EnderecoDto endereco = enderecoComCampo(campo, valor);
                Set<Class<? extends Annotation>> restricoes = campo.equals("uf") && valor != null && valor.length() != 2
                        ? Set.of(NotBlank.class, Size.class) : Set.of(NotBlank.class);

                assertViolacoes(new ClienteDto("Luísa Rocha", "31987654321", endereco), "endereco." + campo, restricoes);
            }

            @ParameterizedTest
            @ValueSource(strings = {"S", "SPP", "S P"})
            @DisplayName("Deve rejeitar UF com tamanho diferente de dois caracteres")
            void deveExigirTamanhoDaUf(String uf) {
                assertViolacoes(new ClienteDto("Luísa Rocha", "31987654321", enderecoComCampo("uf", uf)),
                        "endereco.uf", Set.of(Size.class));
            }

            @ParameterizedTest
            @NullSource
            @EmptySource
            @DisplayName("Deve aceitar complemento ausente ou vazio")
            void deveAceitarComplementoOpcional(String complemento) {
                EnderecoDto endereco = new EnderecoDto("Rua Aurora", "28", complemento, "30110010",
                        "Funcionários", "Belo Horizonte", "MG");

                assertThat(validator.validate(new ClienteDto("Luísa Rocha", "31987654321", endereco))).isEmpty();
            }
        }

        static Stream<Arguments> camposObrigatoriosInvalidos() {
            return Stream.of("logradouro", "numero", "cep", "bairro", "cidade", "uf")
                    .flatMap(campo -> Arrays.asList(null, "", "   ", "\t\n").stream()
                            .map(valor -> Arguments.of(campo, valor)));
        }

        private static EnderecoDto enderecoValido() {
            return new EnderecoDto("Rua Aurora", "28", "Casa", "30110010", "Funcionários", "Belo Horizonte", "MG");
        }

        private static EnderecoDto enderecoComCampo(String campo, String valor) {
            EnderecoDto base = enderecoValido();
            return new EnderecoDto(
                    campo.equals("logradouro") ? valor : base.logradouro(),
                    campo.equals("numero") ? valor : base.numero(), base.complemento(),
                    campo.equals("cep") ? valor : base.cep(),
                    campo.equals("bairro") ? valor : base.bairro(),
                    campo.equals("cidade") ? valor : base.cidade(),
                    campo.equals("uf") ? valor : base.uf());
        }

        private static void assertViolacoes(ClienteDto dto, String propriedade, Set<Class<? extends Annotation>> restricoes) {
            var violacoes = validator.validate(dto);
            assertThat(violacoes).hasSize(restricoes.size())
                    .allSatisfy(violacao -> assertThat(violacao.getPropertyPath().toString()).isEqualTo(propriedade));
            assertThat(violacoes).<Class<? extends Annotation>>extracting(
                            violacao -> violacao.getConstraintDescriptor().getAnnotation().annotationType())
                    .containsExactlyInAnyOrderElementsOf(restricoes);
        }
    }

    @Nested
    @DisplayName("Validar entrada da alteração de status explicitamente, sem chamar o serviço")
    class ValidacaoStatus {
        private static ValidatorFactory factory;
        private static Validator validator;

        @BeforeAll
        static void criarValidador() {
            factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @AfterAll
        static void fecharValidador() {
            if (factory != null) factory.close();
        }

        @Nested
        @DisplayName("Exigir uma situação explícita")
        class StatusObrigatorio {
            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            @DisplayName("Deve aceitar ativação e desativação")
            void deveAceitarBooleano(boolean status) {
                assertThat(validator.validate(new AlterarStatusDto(status))).isEmpty();
            }

            @Test
            @DisplayName("Deve rejeitar status nulo indicando a propriedade e a restrição obrigatória")
            void deveRejeitarNulo() {
                var violacoes = validator.validate(new AlterarStatusDto(null));

                assertThat(violacoes).singleElement().satisfies(violacao -> {
                    assertThat(violacao.getPropertyPath().toString()).isEqualTo("status");
                    assertThat(violacao.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
                    assertThat(violacao.getMessage()).isEqualTo("True ou False obrigatório");
                });
            }
        }
    }
}
