package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.cliente.Cliente;
import br.com.salgadosdalucia.api.cliente.ClienteRepository;
import br.com.salgadosdalucia.api.cliente.dto.ClientePedidoDto;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.pedido.dto.*;
import br.com.salgadosdalucia.api.pedido.enums.FormaPagamento;
import br.com.salgadosdalucia.api.pedido.enums.StatusPedido;
import br.com.salgadosdalucia.api.pedido.enums.TipoEntrega;
import br.com.salgadosdalucia.api.pedido.enums.TipoPreco;
import br.com.salgadosdalucia.api.salgado.Salgado;
import br.com.salgadosdalucia.api.salgado.SalgadoRepository;
import br.com.salgadosdalucia.api.salgado.enums.Categoria;
import br.com.salgadosdalucia.api.shared.endereco.Endereco;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioPedidoDto;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    private static final Long PEDIDO_ID = 10L;
    private static final Long CLIENTE_ID = 1L;
    private static final Long USUARIO_ID = 2L;
    private static final Long SALGADO_ID = 3L;
    private static final LocalDate DATA_PEDIDO = LocalDate.now();
    private static final LocalDateTime DATA_ENTREGA = LocalDateTime.now().plusDays(10).withHour(15).withMinute(30);

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SalgadoRepository salgadoRepository;
    @InjectMocks
    private PedidoService pedidoService;

    private Cliente cliente;
    private Usuario usuario;
    private Salgado salgado;
    private Pedido pedido;
    private CriacaoPedidoRequest request;
    private Pageable paginacao;

    @BeforeEach
    void setUp() {
        cliente = new Cliente(CLIENTE_ID, "Matheus", "11999998888", true,
                new Endereco("Rua A", "10", null, "01001000", "Centro", "Sao Bernardo", "SP"));
        usuario = Usuario.builder().id(USUARIO_ID).nome("Lucia").ativo(true).build();
        salgado = new Salgado(SALGADO_ID, "Coxinha", "Frango", Categoria.FRITO,
                new BigDecimal("60.00"), new BigDecimal("80.00"), true);
        request = criarRequest(TipoEntrega.ENTREGA, new BigDecimal("10.00"),
                List.of(new ItemPedidoDto(SALGADO_ID, 50, TipoPreco.CONGELADO)));
        pedido = criarPedido(PEDIDO_ID);
        paginacao = PageRequest.of(0, 2);
    }

    @Nested
    @DisplayName("Registrar pedido")
    class Registrar {
        @Test
        @DisplayName("Deve registrar pedido com preço congelado, frete e endereço do cliente")
        void deveRegistrar() throws NotFoundException {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.registrar(request);

            // captura o pedido enviado ao banco para conferir também as associações dos itens
            ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
            verify(pedidoRepository).save(captor.capture());

            Pedido salvo = captor.getValue();
            assertThat(salvo.getCliente()).isSameAs(cliente);
            assertThat(salvo.getUsuarioResponsavel()).isSameAs(usuario);
            assertThat(salvo.getItens()).hasSize(1);
            assertThat(salvo.getItens().getFirst().getPedido()).isSameAs(salvo);
            assertThat(salvo.getItens().getFirst().getSalgado()).isSameAs(salgado);
            assertThat(resposta.cliente()).isEqualTo(new ClientePedidoDto(CLIENTE_ID, cliente.getNome()));
            assertThat(resposta.usuarioResponsavel()).isEqualTo(new UsuarioPedidoDto(USUARIO_ID, usuario.getNome()));
            assertThat(resposta.status()).isEqualTo(StatusPedido.EM_ANDAMENTO);
            assertThat(resposta.tipoEntrega()).isEqualTo(TipoEntrega.ENTREGA);
            assertThat(resposta.formaPagamento()).isEqualTo(FormaPagamento.PIX);
            assertThat(resposta.dataPedido()).isEqualTo(DATA_PEDIDO);
            assertThat(resposta.dataEntrega()).isEqualTo(DATA_ENTREGA);
            assertThat(resposta.enderecoEntrega()).isEqualTo(cliente.getEndereco());
            assertThat(resposta.frete()).isEqualByComparingTo("10.00");
            assertThat(resposta.valorTotal()).isEqualByComparingTo("40.00");
            assertThat(resposta.itens()).containsExactly(new ItemPedidoResponse(null, null,
                    "Coxinha", 50, TipoPreco.CONGELADO, new BigDecimal("0.60"), new BigDecimal("30.00")));
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve somar itens congelados e processados sem frete na retirada")
        void deveSomarItensSemFrete() throws NotFoundException {
            request = criarRequest(TipoEntrega.RETIRADA, null, List.of(
                    new ItemPedidoDto(SALGADO_ID, 50, TipoPreco.CONGELADO),
                    new ItemPedidoDto(SALGADO_ID, 25, TipoPreco.PROCESSADO)));

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.registrar(request);

            assertThat(resposta.valorTotal()).isEqualByComparingTo("50.00");
            assertThat(resposta.enderecoEntrega()).isNull();
            assertThat(resposta.frete()).isNull();
            assertThat(resposta.itens().get(1).precoUnitario()).isEqualByComparingTo("0.80");
            assertThat(resposta.itens().get(1).subTotal()).isEqualByComparingTo("20.00");
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository, times(2)).findById(SALGADO_ID);
            verify(pedidoRepository).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve arredondar o preço unitário para duas casas antes de calcular o subtotal")
        void deveArredondarPrecoUnitario() throws NotFoundException {
            salgado.setPrecoCentoCongelado(new BigDecimal("60.50"));

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.registrar(request);

            assertThat(resposta.itens().getFirst().precoUnitario()).isEqualByComparingTo("0.61");
            assertThat(resposta.itens().getFirst().subTotal()).isEqualByComparingTo("30.50");
            assertThat(resposta.valorTotal()).isEqualByComparingTo("40.50");
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar cliente inexistente sem salvar")
        void deveRejeitarClienteInexistente() throws NotFoundException {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Cliente não encontrado com o ID: %s", CLIENTE_ID);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar cliente inativo sem salvar")
        void deveRejeitarClienteInativo() throws NotFoundException {
            cliente.setAtivo(false);

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cliente com ID %d está inativo e não pode realizar pedidos.", CLIENTE_ID);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar usuario inexistente sem salvar")
        void deveRejeitarUsuarioInexistente() throws NotFoundException {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Usuario não encontrado com o ID: %s", USUARIO_ID);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar usuario inativo sem salvar")
        void deveRejeitarUsuarioInativo() throws NotFoundException {
            usuario.setAtivo(false);

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Usuario com ID %d está inativo e não pode ser responsável por pedidos.", USUARIO_ID);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar salgado inexistente sem salvar")
        void deveRejeitarSalgadoInexistente() throws NotFoundException {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Salgado não encontrado com o ID: %s", SALGADO_ID);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar salgado inativo sem salvar")
        void deveRejeitarSalgadoInativo() throws NotFoundException {
            salgado.setAtivo(false);

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Salgado com ID %d está inativo e não pode ser adicionado ao pedido.", SALGADO_ID);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de cliente")
        void deveRetornarErroDeCliente() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(clienteRepository.findById(CLIENTE_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.registrar(request)).isSameAs(erroDoBanco);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de usuario")
        void deveRetornarErroDeUsuario() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.registrar(request)).isSameAs(erroDoBanco);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de salgado")
        void deveRetornarErroDeSalgado() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.registrar(request)).isSameAs(erroDoBanco);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de pedido")
        void deveRetornarErroDePedido() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            when(pedidoRepository.save(any(Pedido.class))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.registrar(request)).isSameAs(erroDoBanco);

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }
    }

    @Nested
    @DisplayName("Atualizar pedido")
    class Atualizar {
        @Test
        @DisplayName("Deve substituir itens e atualizar os dados de um pedido em andamento")
        void deveAtualizar() throws NotFoundException {
            ItemPedido itemAnterior = pedido.getItens().getFirst();
            request = criarRequest(TipoEntrega.ENTREGA, pedido.getFrete(),
                    List.of(new ItemPedidoDto(SALGADO_ID, 25, TipoPreco.PROCESSADO)));

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.atualizar(PEDIDO_ID, request);

            assertThat(resposta.id()).isEqualTo(PEDIDO_ID);
            assertThat(resposta.status()).isEqualTo(StatusPedido.EM_ANDAMENTO);
            assertThat(resposta.dataPedido()).isEqualTo(DATA_PEDIDO);
            assertThat(resposta.dataEntrega()).isEqualTo(DATA_ENTREGA);
            assertThat(resposta.formaPagamento()).isEqualTo(FormaPagamento.PIX);
            assertThat(pedido.getCliente()).isSameAs(cliente);
            assertThat(pedido.getUsuarioResponsavel()).isSameAs(usuario);
            assertThat(resposta.enderecoEntrega().getLogradouro()).isEqualTo("Rua B");
            assertThat(resposta.valorTotal()).isEqualByComparingTo("25.00");
            assertThat(pedido.getItens()).hasSize(1).doesNotContain(itemAnterior);
            assertThat(pedido.getItens().getFirst().getPedido()).isSameAs(pedido);
            assertThat(resposta.itens()).containsExactly(new ItemPedidoResponse(null, PEDIDO_ID,
                    "Coxinha", 25, TipoPreco.PROCESSADO, new BigDecimal("0.80"), new BigDecimal("20.00")));
            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(pedidoRepository).save(pedido);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve remover endereço ao mudar o pedido para retirada")
        void deveRemoverEnderecoNaRetirada() throws NotFoundException {
            request = criarRequest(TipoEntrega.RETIRADA, pedido.getFrete(), request.itens());

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.atualizar(PEDIDO_ID, request);

            assertThat(resposta.tipoEntrega()).isEqualTo(TipoEntrega.RETIRADA);
            assertThat(resposta.enderecoEntrega()).isNull();
            assertThat(pedido.getEnderecoEntrega()).isNull();
            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(pedidoRepository).save(pedido);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar cliente inexistente sem salvar")
        void deveRejeitarClienteInexistente() throws NotFoundException {
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Cliente não encontrado com o ID: %s", CLIENTE_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar cliente inativo sem salvar")
        void deveRejeitarClienteInativo() throws NotFoundException {
            cliente.setAtivo(false);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cliente com ID %d está inativo e não pode realizar pedidos.", CLIENTE_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar usuario inexistente sem salvar")
        void deveRejeitarUsuarioInexistente() throws NotFoundException {
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Usuario não encontrado com o ID: %s", USUARIO_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar usuario inativo sem salvar")
        void deveRejeitarUsuarioInativo() throws NotFoundException {
            usuario.setAtivo(false);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Usuario com ID %d está inativo e não pode ser responsável por pedidos.", USUARIO_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar salgado inexistente sem salvar")
        void deveRejeitarSalgadoInexistente() throws NotFoundException {
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Salgado não encontrado com o ID: %s", SALGADO_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar salgado inativo sem salvar")
        void deveRejeitarSalgadoInativo() throws NotFoundException {
            salgado.setAtivo(false);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Salgado com ID %d está inativo e não pode ser adicionado ao pedido.", SALGADO_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar atualização de pedido CONCLUIDO")
        void deveRejeitarPedidoCONCLUIDO() throws NotFoundException {
            pedido.setStatus(StatusPedido.CONCLUIDO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Somente pedidos com status EM_ANDAMENTO podem ser atualizados. Pedido ID %d tem status %s", pedido.getId(), pedido.getStatus());

            assertThat(criarResponse(pedido)).isEqualTo(anterior);
            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar atualização de pedido CANCELADO")
        void deveRejeitarPedidoCANCELADO() throws NotFoundException {
            pedido.setStatus(StatusPedido.CANCELADO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Somente pedidos com status EM_ANDAMENTO podem ser atualizados. Pedido ID %d tem status %s", pedido.getId(), pedido.getStatus());

            assertThat(criarResponse(pedido)).isEqualTo(anterior);
            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar pedido inexistente")
        void deveRejeitarPedidoInexistente() throws NotFoundException {
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Pedido não encontrado com o ID: %s", PEDIDO_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de cliente")
        void deveRetornarErroDeCliente() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request)).isSameAs(erroDoBanco);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de usuario")
        void deveRetornarErroDeUsuario() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request)).isSameAs(erroDoBanco);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de salgado")
        void deveRetornarErroDeSalgado() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request)).isSameAs(erroDoBanco);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do repositório de pedido")
        void deveRetornarErroDePedido() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            when(pedidoRepository.save(any(Pedido.class))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request)).isSameAs(erroDoBanco);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco ao buscar pedido")
        void deveRetornarErroNaBusca() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findById(PEDIDO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isSameAs(erroDoBanco);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }
    }

    @Nested
    @DisplayName("Listar pedidos com filtro")
    class ListarComFiltro {
        @Test
        @DisplayName("Deve repassar todos os filtros e preservar dados da página")
        void deveListarComFiltros() {
            PedidoFiltroDto filtro = criarFiltro(DATA_ENTREGA, DATA_ENTREGA.plusDays(2));
            LocalDateTime inicio = DATA_ENTREGA.toLocalDate().atStartOfDay();
            LocalDateTime fim = DATA_ENTREGA.plusDays(2).toLocalDate().atTime(23, 59, 59);
            Pedido segundoPedido = criarPedido(20L);

            when(pedidoRepository.findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, inicio, fim, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao))
                    .thenReturn(new PageImpl<>(List.of(pedido, segundoPedido), paginacao, 5));

            Page<PedidoListagemDto> resposta = pedidoService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).containsExactly(criarResponse(pedido), criarResponse(segundoPedido));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(pedidoRepository).findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, inicio, fim, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve usar o mesmo dia quando somente o início for informado")
        void deveFiltrarUmDia() throws NotFoundException {
            PedidoFiltroDto filtro = criarFiltro(DATA_ENTREGA, null);

            when(pedidoRepository.findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, DATA_ENTREGA.toLocalDate().atStartOfDay(), DATA_ENTREGA.toLocalDate().atTime(23, 59, 59), TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao)).thenReturn(Page.empty(paginacao));

            Page<PedidoListagemDto> resposta = pedidoService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();

            verify(pedidoRepository).findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, DATA_ENTREGA.toLocalDate().atStartOfDay(), DATA_ENTREGA.toLocalDate().atTime(23, 59, 59), TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia sem datas de entrega")
        void deveListarSemDatas() throws NotFoundException {
            PedidoFiltroDto filtro = criarFiltro(null, null);

            when(pedidoRepository.findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, null, null, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao)).thenReturn(Page.empty(paginacao));

            Page<PedidoListagemDto> resposta = pedidoService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();

            verify(pedidoRepository).findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, null, null, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve manter limites nulos quando somente o fim for informado")
        void deveIgnorarFimSemInicio() throws NotFoundException {
            PedidoFiltroDto filtro = criarFiltro(null, DATA_ENTREGA);

            when(pedidoRepository.findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, null, null, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao)).thenReturn(Page.empty(paginacao));

            Page<PedidoListagemDto> resposta = pedidoService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();

            verify(pedidoRepository).findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, null, null, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na listagem")
        void deveRetornarErroQuandoBancoFalhar() throws NotFoundException {
            PedidoFiltroDto filtro = criarFiltro(null, null);
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, null, null, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.listarComFiltro(filtro, paginacao)).isSameAs(erroDoBanco);

            verify(pedidoRepository).findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus",
                    DATA_PEDIDO, null, null, TipoEntrega.ENTREGA, FormaPagamento.PIX,
                    USUARIO_ID, "Lucia", paginacao);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }
    }

    @Nested
    @DisplayName("Buscar pedido por ID")
    class BuscarPorId {
        @Test
        @DisplayName("Deve retornar os dados completos do pedido encontrado")
        void deveBuscar() throws NotFoundException {
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            PedidoListagemDto resposta = pedidoService.buscarPorId(PEDIDO_ID);

            assertThat(resposta).isEqualTo(criarResponse(pedido));
            assertThat(resposta.id()).isEqualTo(PEDIDO_ID);
            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve retornar pedido cancelado com retirada sem endereço")
        void deveBuscarPedidoCancelado() throws NotFoundException {
            pedido.setStatus(StatusPedido.CANCELADO);
            pedido.setTipoEntrega(TipoEntrega.RETIRADA);
            pedido.setEnderecoEntrega(null);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            PedidoListagemDto resposta = pedidoService.buscarPorId(PEDIDO_ID);

            assertThat(resposta.status()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(resposta.enderecoEntrega()).isNull();
            assertThat(resposta).isEqualTo(criarResponse(pedido));
            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar pedido inexistente")
        void deveRejeitarPedidoInexistente() throws NotFoundException {
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.buscarPorId(PEDIDO_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Pedido não encontrado com o ID: %s", PEDIDO_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco ao buscar pedido")
        void deveRetornarErroNaBusca() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findById(PEDIDO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.buscarPorId(PEDIDO_ID))
                    .isSameAs(erroDoBanco);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }
    }

    @Nested
    @DisplayName("Alterar status do pedido")
    class AlterarStatus {
        @Test
        @DisplayName("Deve rejeitar pedido inexistente")
        void deveRejeitarPedidoInexistente() throws NotFoundException {
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.CONCLUIDO)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Pedido não encontrado com o ID: %s", PEDIDO_ID);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve concluir pedido preservando os demais dados")
        void deveConcluirPedido() throws NotFoundException {
            pedido.setStatus(StatusPedido.EM_ANDAMENTO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.CONCLUIDO));

            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
            assertThat(pedido.getValorTotal()).isEqualTo(anterior.valorTotal());
            assertThat(pedido.getDataEntrega()).isEqualTo(anterior.dataEntrega());
            assertThat(pedido.getCliente()).isSameAs(cliente);
            assertThat(pedido.getUsuarioResponsavel()).isSameAs(usuario);
            assertThat(criarResponse(pedido).itens()).isEqualTo(anterior.itens());
            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve cancelar pedido preservando os demais dados")
        void deveCancelarPedido() throws NotFoundException {
            pedido.setStatus(StatusPedido.EM_ANDAMENTO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.CANCELADO));

            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(pedido.getValorTotal()).isEqualTo(anterior.valorTotal());
            assertThat(pedido.getDataEntrega()).isEqualTo(anterior.dataEntrega());
            assertThat(pedido.getCliente()).isSameAs(cliente);
            assertThat(pedido.getUsuarioResponsavel()).isSameAs(usuario);
            assertThat(criarResponse(pedido).itens()).isEqualTo(anterior.itens());
            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve reabrir pedido preservando os demais dados")
        void deveReabrirPedido() throws NotFoundException {
            pedido.setStatus(StatusPedido.CONCLUIDO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.EM_ANDAMENTO));

            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EM_ANDAMENTO);
            assertThat(pedido.getValorTotal()).isEqualTo(anterior.valorTotal());
            assertThat(pedido.getDataEntrega()).isEqualTo(anterior.dataEntrega());
            assertThat(pedido.getCliente()).isSameAs(cliente);
            assertThat(pedido.getUsuarioResponsavel()).isSameAs(usuario);
            assertThat(criarResponse(pedido).itens()).isEqualTo(anterior.itens());
            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(pedidoRepository, never()).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar status repetido EM_ANDAMENTO")
        void deveRejeitarStatusRepetidoEM_ANDAMENTO() throws NotFoundException {
            pedido.setStatus(StatusPedido.EM_ANDAMENTO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.EM_ANDAMENTO)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("O pedido já está com o status EM_ANDAMENTO");

            assertThat(criarResponse(pedido)).isEqualTo(anterior);
            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar status repetido CONCLUIDO")
        void deveRejeitarStatusRepetidoCONCLUIDO() throws NotFoundException {
            pedido.setStatus(StatusPedido.CONCLUIDO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.CONCLUIDO)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("O pedido já está com o status CONCLUIDO");

            assertThat(criarResponse(pedido)).isEqualTo(anterior);
            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar status repetido CANCELADO")
        void deveRejeitarStatusRepetidoCANCELADO() throws NotFoundException {
            pedido.setStatus(StatusPedido.CANCELADO);
            PedidoListagemDto anterior = criarResponse(pedido);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.CANCELADO)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("O pedido já está com o status CANCELADO");

            assertThat(criarResponse(pedido)).isEqualTo(anterior);
            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco ao buscar pedido")
        void deveRetornarErroNaBusca() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(pedidoRepository.findById(PEDIDO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> pedidoService.alterarStatus(PEDIDO_ID, new AlterarStatusPedidoDto(StatusPedido.CONCLUIDO)))
                    .isSameAs(erroDoBanco);

            verify(pedidoRepository).findById(PEDIDO_ID);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }
    }

    private CriacaoPedidoRequest criarRequest(TipoEntrega tipoEntrega, BigDecimal frete, List<ItemPedidoDto> itens) {
        return new CriacaoPedidoRequest(CLIENTE_ID, itens,
                new EnderecoDto("Rua B", "20", "Casa", "02002000", "Centro", "São Paulo", "SP"),
                DATA_PEDIDO, DATA_ENTREGA, tipoEntrega, FormaPagamento.PIX, USUARIO_ID, frete);
    }

    private Pedido criarPedido(Long id) {
        Pedido entidade = Pedido.builder().id(id).cliente(cliente).usuarioResponsavel(usuario)
                .itens(new ArrayList<>()).enderecoEntrega(cliente.getEndereco())
                .frete(new BigDecimal("5.00")).valorTotal(new BigDecimal("65.00"))
                .dataPedido(DATA_PEDIDO.minusDays(1)).dataEntrega(DATA_ENTREGA.minusDays(1))
                .status(StatusPedido.EM_ANDAMENTO).tipoEntrega(TipoEntrega.ENTREGA)
                .formaPagamento(FormaPagamento.DINHEIRO).build();
        ItemPedido item = new ItemPedido();
        item.setId(100L);
        item.setPedido(entidade);
        item.setSalgado(salgado);
        item.setQuantidade(100);
        item.setTipoPreco(TipoPreco.CONGELADO);
        item.setPrecoUnitario(new BigDecimal("0.60"));
        item.setSubTotal(new BigDecimal("60.00"));
        entidade.getItens().add(item);
        return entidade;
    }

    private PedidoFiltroDto criarFiltro(LocalDateTime inicio, LocalDateTime fim) {
        return new PedidoFiltroDto(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus", DATA_PEDIDO,
                inicio, fim, TipoEntrega.ENTREGA, FormaPagamento.PIX, USUARIO_ID, "Lucia");
    }

    private PedidoListagemDto criarResponse(Pedido entidade) {
        List<ItemPedidoResponse> itens = entidade.getItens().stream().map(item -> new ItemPedidoResponse(
                item.getId(), entidade.getId(), item.getSalgado().getNome(), item.getQuantidade(),
                item.getTipoPreco(), item.getPrecoUnitario(), item.getSubTotal())).toList();
        Endereco endereco = entidade.getEnderecoEntrega();
        EnderecoDto enderecoDto = endereco == null ? null : new EnderecoDto(endereco.getLogradouro(),
                endereco.getNumero(), endereco.getComplemento(), endereco.getCep(), endereco.getBairro(),
                endereco.getCidade(), endereco.getUf());
        return new PedidoListagemDto(entidade.getId(),
                new ClientePedidoDto(entidade.getCliente().getId(), entidade.getCliente().getNome()),
                entidade.getStatus(), itens, entidade.getFrete(), entidade.getValorTotal(), entidade.getDataPedido(),
                entidade.getDataEntrega(), entidade.getTipoEntrega(), enderecoDto, entidade.getFormaPagamento(),
                new UsuarioPedidoDto(entidade.getUsuarioResponsavel().getId(), entidade.getUsuarioResponsavel().getNome()));
    }

}
