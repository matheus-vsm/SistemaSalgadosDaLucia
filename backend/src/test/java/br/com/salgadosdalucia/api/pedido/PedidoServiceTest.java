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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.*;

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
        @ParameterizedTest
        @CsvSource({
                "CONGELADO, 60.49, 3, 0.6049, 1.81",
                "CONGELADO, 60.50, 3, 0.605, 1.82",
                "CONGELADO, 60.51, 3, 0.6051, 1.82",
                "PROCESSADO, 55.55, 150, 0.5555, 83.33",
                "PROCESSADO, 1.00, 7, 0.01, 0.07",
                "CONGELADO, 60.50, 50, 0.605, 30.25",
                "CONGELADO, 60.50, 100, 0.605, 60.50",
                "PROCESSADO, 3.35, 30, 0.0335, 1.01",
                "CONGELADO, 0.01, 49, 0.0001, 0.00",
                "CONGELADO, 0.01, 50, 0.0001, 0.01"
        })
        @DisplayName("Deve preservar o preço unitário exato e aplicar HALF_UP somente ao subtotal")
        void deveCalcularPrecoPorTipo(TipoPreco tipo, String cento, int quantidade, String unitario, String subtotal) {
            salgado.setPrecoCentoCongelado(new BigDecimal(tipo == TipoPreco.CONGELADO ? cento : "135.00"));
            salgado.setPrecoCentoProcessado(new BigDecimal(tipo == TipoPreco.PROCESSADO ? cento : "246.00"));
            request = criarRequest(TipoEntrega.RETIRADA, null, List.of(new ItemPedidoDto(SALGADO_ID, quantidade, tipo)), null);

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.registrar(request);

            assertThat(resposta.itens()).containsExactly(new ItemPedidoResponse(null, null, "Coxinha", quantidade,
                    tipo, new BigDecimal(unitario), new BigDecimal(subtotal)));
            assertThat(resposta.valorTotal()).isEqualByComparingTo(subtotal);
            assertThat(resposta.frete()).isNull();
            assertThat(resposta.enderecoEntrega()).isNull();
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(pedidoRepository).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve registrar produtos distintos, preservar seus vínculos e somar o frete apenas uma vez")
        void deveRegistrarProdutosDistintos() {
            Salgado segundo = new Salgado(41L, "Esfiha", "Queijo", Categoria.FRITO,
                    new BigDecimal("72.50"), new BigDecimal("91.49"), true);
            request = criarRequest(TipoEntrega.ENTREGA, new BigDecimal("2.35"), List.of(
                    new ItemPedidoDto(SALGADO_ID, 50, TipoPreco.CONGELADO),
                    new ItemPedidoDto(41L, 3, TipoPreco.PROCESSADO)));

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            when(salgadoRepository.findById(41L)).thenReturn(Optional.of(segundo));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> {
                Pedido novo = invocacao.getArgument(0);
                assertThat(novo.getId()).isNull();
                assertThat(novo.getItens()).hasSize(2).allSatisfy(item -> {
                    assertThat(item.getId()).isNull();
                    assertThat(item.getPedido()).isSameAs(novo);
                });
                assertThat(novo.getItens().get(0).getSalgado()).isSameAs(salgado);
                assertThat(novo.getItens().get(1).getSalgado()).isSameAs(segundo);
                novo.setId(87L);
                novo.getItens().get(0).setId(501L);
                novo.getItens().get(1).setId(502L);
                return novo;
            });

            CriacaoPedidoResponse resposta = pedidoService.registrar(request);

            assertThat(resposta.id()).isEqualTo(87L);
            assertThat(resposta.itens()).containsExactly(
                    new ItemPedidoResponse(501L, 87L, "Coxinha", 50, TipoPreco.CONGELADO, new BigDecimal("0.60"), new BigDecimal("30.00")),
                    new ItemPedidoResponse(502L, 87L, "Esfiha", 3, TipoPreco.PROCESSADO, new BigDecimal("0.9149"), new BigDecimal("2.74")));
            assertThat(resposta.valorTotal()).isEqualByComparingTo("35.09");
            assertThat(resposta.frete()).isEqualByComparingTo("2.35");
            assertThat(resposta.status()).isEqualTo(StatusPedido.EM_ANDAMENTO);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(salgadoRepository).findById(41L);
            verify(pedidoRepository).save(any(Pedido.class));
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Deve interromper o cadastro sem salvar ou consultar o terceiro item quando o segundo for inexistente ou inativo")
        void deveInterromperNoSegundoItemInvalido(boolean segundoExiste) {
            request = criarRequest(TipoEntrega.ENTREGA, new BigDecimal("2.00"), List.of(
                    new ItemPedidoDto(SALGADO_ID, 3, TipoPreco.CONGELADO),
                    new ItemPedidoDto(37L, 2, TipoPreco.PROCESSADO),
                    new ItemPedidoDto(77L, 1, TipoPreco.CONGELADO)));

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            Salgado segundo = new Salgado(37L, "Kibe", "Carne", Categoria.FRITO,
                    new BigDecimal("70.00"), new BigDecimal("90.00"), false);

            when(salgadoRepository.findById(37L)).thenReturn(segundoExiste ? Optional.of(segundo) : Optional.empty());

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(segundoExiste ? BusinessException.class : NotFoundException.class)
                    .hasMessage(segundoExiste ? "Salgado com ID 37 está inativo e não pode ser adicionado ao pedido."
                            : "Salgado não encontrado com o ID: 37");

            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(salgadoRepository).findById(37L);
            verifyNoInteractions(pedidoRepository);
            verifyNoMoreInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar entrega sem endereço mesmo quando o cliente possui endereço")
        void deveRejeitarEntregaSemEndereco() throws NotFoundException {
            request = criarRequest(TipoEntrega.ENTREGA, request.frete(), request.itens(), null);

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            assertThatThrownBy(() -> pedidoService.registrar(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Endereço de entrega é obrigatório para pedidos com entrega.");

            verify(pedidoRepository, never()).save(any(Pedido.class));
        }

        @Test
        @DisplayName("Deve registrar retirada com endereço nulo")
        void deveRegistrarRetiradaSemEndereco() throws NotFoundException {
            request = criarRequest(TipoEntrega.RETIRADA, null, request.itens(), null);
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            assertThat(pedidoService.registrar(request).enderecoEntrega()).isNull();
            verify(pedidoRepository).save(any(Pedido.class));
        }

        @Test
        @DisplayName("Deve registrar pedido com preço congelado, frete e endereço informado")
        void deveRegistrar() throws NotFoundException {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> {
                Pedido novo = invocacao.getArgument(0);
                assertThat(novo.getId()).isNull();
                novo.setId(PEDIDO_ID);
                novo.getItens().getFirst().setId(501L);
                return novo;
            });

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
            assertThat(resposta.id()).isEqualTo(PEDIDO_ID);
            assertThat(resposta.tipoEntrega()).isEqualTo(TipoEntrega.ENTREGA);
            assertThat(resposta.formaPagamento()).isEqualTo(FormaPagamento.PIX);
            assertThat(resposta.dataPedido()).isEqualTo(DATA_PEDIDO);
            assertThat(resposta.dataEntrega()).isEqualTo(DATA_ENTREGA);
            assertThat(resposta.enderecoEntrega()).usingRecursiveComparison().isEqualTo(request.enderecoEntrega());
            assertThat(resposta.enderecoEntrega()).isNotSameAs(cliente.getEndereco());
            assertThat(resposta.frete()).isEqualByComparingTo("10.00");
            assertThat(resposta.valorTotal()).isEqualByComparingTo("40.00");
            assertThat(resposta.itens()).containsExactly(new ItemPedidoResponse(501L, PEDIDO_ID,
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
        @DisplayName("Deve preservar a fração de centavo no unitário e somar o frete ao subtotal arredondado")
        void deveArredondarSomenteSubtotal() throws NotFoundException {
            salgado.setPrecoCentoCongelado(new BigDecimal("60.50"));

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.registrar(request);

            assertThat(resposta.itens().getFirst().precoUnitario()).isEqualByComparingTo("0.605");
            assertThat(resposta.itens().getFirst().subTotal()).isEqualByComparingTo("30.25");
            assertThat(resposta.valorTotal()).isEqualByComparingTo("40.25");
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
        @ParameterizedTest
        @CsvSource(value = {"12.75, 42.75", "NULL, 30.00"}, nullValues = "NULL")
        @DisplayName("Deve substituir ou remover o frete anterior e recalcular o total")
        void deveAplicarFreteInformado(String novoFrete, String totalEsperado) throws NotFoundException {
            request = criarRequest(TipoEntrega.ENTREGA,
                    novoFrete == null ? null : new BigDecimal(novoFrete),
                    List.of(new ItemPedidoDto(SALGADO_ID, 50, TipoPreco.CONGELADO)));

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.atualizar(PEDIDO_ID, request);

            assertThat(resposta.frete()).isEqualTo(request.frete());
            assertThat(pedido.getFrete()).isEqualTo(request.frete());
            assertThat(resposta.valorTotal()).isEqualByComparingTo(totalEsperado);
            verify(pedidoRepository).save(pedido);
        }

        @ParameterizedTest
        @CsvSource({
                "CONGELADO, 60.49, 3, 0.6049, 1.81",
                "CONGELADO, 60.50, 3, 0.605, 1.82",
                "CONGELADO, 60.51, 3, 0.6051, 1.82",
                "PROCESSADO, 55.55, 150, 0.5555, 83.33",
                "PROCESSADO, 1.00, 7, 0.01, 0.07",
                "CONGELADO, 60.50, 50, 0.605, 30.25",
                "CONGELADO, 60.50, 100, 0.605, 60.50",
                "PROCESSADO, 3.35, 30, 0.0335, 1.01",
                "CONGELADO, 0.01, 49, 0.0001, 0.00",
                "CONGELADO, 0.01, 50, 0.0001, 0.01"
        })
        @DisplayName("Deve recalcular pelo catálogo atual arredondando apenas o novo subtotal")
        void deveRecalcularPrecoPorTipo(TipoPreco tipo, String cento, int quantidade, String unitario, String subtotal) {
            pedido.setFrete(null);
            salgado.setPrecoCentoCongelado(new BigDecimal(tipo == TipoPreco.CONGELADO ? cento : "135.00"));
            salgado.setPrecoCentoProcessado(new BigDecimal(tipo == TipoPreco.PROCESSADO ? cento : "246.00"));
            request = criarRequest(TipoEntrega.RETIRADA, null, List.of(new ItemPedidoDto(SALGADO_ID, quantidade, tipo)), null);
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.atualizar(PEDIDO_ID, request);

            assertThat(resposta.itens()).containsExactly(new ItemPedidoResponse(null, PEDIDO_ID, "Coxinha", quantidade,
                    tipo, new BigDecimal(unitario), new BigDecimal(subtotal)));
            assertThat(resposta.id()).isEqualTo(PEDIDO_ID);
            assertThat(resposta.status()).isEqualTo(StatusPedido.EM_ANDAMENTO);
            assertThat(resposta.valorTotal()).isEqualByComparingTo(subtotal);
            assertThat(resposta.enderecoEntrega()).isNull();
            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(pedidoRepository).save(pedido);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve substituir os itens, atualizar o endereço e retornar novos IDs sem acumular o total antigo")
        void deveAtualizarProdutosDistintos() {
            salgado.setPrecoCentoCongelado(new BigDecimal("65.50"));
            Salgado segundo = new Salgado(41L, "Esfiha", "Queijo", Categoria.FRITO,
                    new BigDecimal("72.50"), new BigDecimal("91.49"), true);
            pedido.setFrete(new BigDecimal("2.35"));
            EnderecoDto endereco = new EnderecoDto("Avenida Central", "81", "Fundos", "30110010",
                    "Funcionários", "Belo Horizonte", "MG");
            request = criarRequest(TipoEntrega.ENTREGA, new BigDecimal("2.35"), List.of(
                    new ItemPedidoDto(SALGADO_ID, 50, TipoPreco.CONGELADO),
                    new ItemPedidoDto(41L, 3, TipoPreco.PROCESSADO)), endereco);

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            when(salgadoRepository.findById(41L)).thenReturn(Optional.of(segundo));
            when(pedidoRepository.save(pedido)).thenAnswer(invocacao -> {
                assertThat(pedido.getItens()).hasSize(2).allSatisfy(item -> {
                    assertThat(item.getId()).isNull();
                    assertThat(item.getPedido()).isSameAs(pedido);
                });
                assertThat(pedido.getItens().get(0).getSalgado()).isSameAs(salgado);
                assertThat(pedido.getItens().get(1).getSalgado()).isSameAs(segundo);
                pedido.getItens().get(0).setId(501L);
                pedido.getItens().get(1).setId(502L);
                return pedido;
            });

            CriacaoPedidoResponse resposta = pedidoService.atualizar(PEDIDO_ID, request);

            assertThat(resposta.id()).isEqualTo(PEDIDO_ID);
            assertThat(resposta.itens()).containsExactly(
                    new ItemPedidoResponse(501L, PEDIDO_ID, "Coxinha", 50, TipoPreco.CONGELADO, new BigDecimal("0.655"), new BigDecimal("32.75")),
                    new ItemPedidoResponse(502L, PEDIDO_ID, "Esfiha", 3, TipoPreco.PROCESSADO, new BigDecimal("0.9149"), new BigDecimal("2.74")));
            assertThat(resposta.valorTotal()).isEqualByComparingTo("37.84");
            assertThat(resposta.frete()).isEqualByComparingTo("2.35");
            assertThat(resposta.enderecoEntrega()).usingRecursiveComparison().isEqualTo(endereco);
            assertThat(cliente.getEndereco()).usingRecursiveComparison().isEqualTo(
                    new Endereco("Rua A", "10", null, "01001000", "Centro", "Sao Bernardo", "SP"));
            assertThat(resposta.status()).isEqualTo(StatusPedido.EM_ANDAMENTO);
            assertThat(resposta.dataPedido()).isEqualTo(DATA_PEDIDO);
            assertThat(resposta.dataEntrega()).isEqualTo(DATA_ENTREGA);
            assertThat(resposta.formaPagamento()).isEqualTo(FormaPagamento.PIX);
            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(pedidoRepository).save(pedido);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(salgadoRepository).findById(41L);
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Deve interromper a edição sem salvar ou consultar o terceiro item quando o segundo for inexistente ou inativo")
        void deveInterromperNoSegundoItemInvalido(boolean segundoExiste) {
            request = criarRequest(TipoEntrega.ENTREGA, pedido.getFrete(), List.of(
                    new ItemPedidoDto(SALGADO_ID, 3, TipoPreco.CONGELADO),
                    new ItemPedidoDto(37L, 2, TipoPreco.PROCESSADO),
                    new ItemPedidoDto(77L, 1, TipoPreco.CONGELADO)));

            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            Salgado segundo = new Salgado(37L, "Kibe", "Carne", Categoria.FRITO,
                    new BigDecimal("70.00"), new BigDecimal("90.00"), false);
            when(salgadoRepository.findById(37L)).thenReturn(segundoExiste ? Optional.of(segundo) : Optional.empty());

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(segundoExiste ? BusinessException.class : NotFoundException.class)
                    .hasMessage(segundoExiste ? "Salgado com ID 37 está inativo e não pode ser adicionado ao pedido."
                            : "Salgado não encontrado com o ID: 37");

            verify(pedidoRepository).findById(PEDIDO_ID);
            verify(clienteRepository).findById(CLIENTE_ID);
            verify(usuarioRepository).findById(USUARIO_ID);
            verify(salgadoRepository).findById(SALGADO_ID);
            verify(salgadoRepository).findById(37L);
            verifyNoMoreInteractions(pedidoRepository, clienteRepository, usuarioRepository, salgadoRepository);
        }

        @Test
        @DisplayName("Deve rejeitar entrega sem endereço sem reutilizar o endereço anterior ou do cliente")
        void deveRejeitarEntregaSemEndereco() throws NotFoundException {
            request = criarRequest(TipoEntrega.ENTREGA, request.frete(), request.itens(), null);
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            assertThatThrownBy(() -> pedidoService.atualizar(PEDIDO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Endereço de entrega é obrigatório para pedidos com entrega.");

            verify(pedidoRepository, never()).save(any(Pedido.class));
        }

        @Test
        @DisplayName("Deve mudar retirada para entrega com o endereço informado")
        void deveMudarRetiradaParaEntrega() throws NotFoundException {
            pedido.setTipoEntrega(TipoEntrega.RETIRADA);
            pedido.setEnderecoEntrega(null);
            when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));

            CriacaoPedidoResponse resposta = pedidoService.atualizar(PEDIDO_ID, request);

            assertThat(resposta.tipoEntrega()).isEqualTo(TipoEntrega.ENTREGA);
            assertThat(resposta.enderecoEntrega()).usingRecursiveComparison().isEqualTo(request.enderecoEntrega());
            verify(pedidoRepository).save(pedido);
        }

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
            request = criarRequest(TipoEntrega.RETIRADA, pedido.getFrete(), request.itens(), null);

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
        @ParameterizedTest
        @CsvSource({
                "2026-11-20T15:30, , 2026-11-20T00:00, 2026-11-20T23:59:59",
                "2026-11-20T21:45, 2026-11-23T06:10, 2026-11-20T00:00, 2026-11-23T23:59:59",
                "2026-12-31T10:15, 2027-01-01T18:45, 2026-12-31T00:00, 2027-01-01T23:59:59"
        })
        @DisplayName("Deve expandir períodos válidos para os limites dos dias e preservar os demais filtros")
        void deveNormalizarPeriodoDeEntrega(String inicio, String fim, String limiteInicial, String limiteFinal) {
            PedidoFiltroDto filtro = criarFiltro(LocalDateTime.parse(inicio), fim == null ? null : LocalDateTime.parse(fim));
            LocalDateTime esperadoInicio = LocalDateTime.parse(limiteInicial);
            LocalDateTime esperadoFim = LocalDateTime.parse(limiteFinal);

            when(pedidoRepository.findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus", DATA_PEDIDO,
                    esperadoInicio, esperadoFim, TipoEntrega.ENTREGA, FormaPagamento.PIX, USUARIO_ID, "Lucia", paginacao))
                    .thenReturn(Page.empty(paginacao));

            Page<PedidoListagemDto> resposta = pedidoService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            assertThat(resposta.getTotalPages()).isZero();
            verify(pedidoRepository).findWithFiltros(StatusPedido.EM_ANDAMENTO, CLIENTE_ID, "Matheus", DATA_PEDIDO,
                    esperadoInicio, esperadoFim, TipoEntrega.ENTREGA, FormaPagamento.PIX, USUARIO_ID, "Lucia", paginacao);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

        @ParameterizedTest
        @CsvSource({"1, 2, 7, false", "3, 1, 7, true", "4, 0, 7, true"})
        @DisplayName("Deve preservar conteúdo, ordem e metadados de páginas posteriores, parciais e vazias")
        void devePreservarPaginaRetornada(int pagina, int quantidade, long total, boolean ultima) {
            Pageable requisitada = PageRequest.of(pagina, 2, Sort.by("dataEntrega").descending());
            Pedido segundo = criarPedido(83L);
            segundo.setTipoEntrega(TipoEntrega.RETIRADA);
            segundo.setEnderecoEntrega(null);
            segundo.setStatus(StatusPedido.CANCELADO);
            List<Pedido> conteudo = switch (quantidade) {
                case 2 -> List.of(segundo, pedido);
                case 1 -> List.of(pedido);
                default -> List.of();
            };
            PedidoFiltroDto filtro = new PedidoFiltroDto(null, null, null, null, null, null, null, null, null, null);

            when(pedidoRepository.findWithFiltros(null, null, null, null, null, null, null, null, null, null, requisitada))
                    .thenReturn(new PageImpl<>(conteudo, requisitada, total));

            Page<PedidoListagemDto> resposta = pedidoService.listarComFiltro(filtro, requisitada);

            assertThat(resposta.getContent()).containsExactlyElementsOf(conteudo.stream().map(PedidoServiceTest.this::criarResponse).toList());
            assertThat(resposta.getPageable()).isEqualTo(requisitada);
            assertThat(resposta.getNumber()).isEqualTo(pagina);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            assertThat(resposta.hasPrevious()).isTrue();
            assertThat(resposta.isLast()).isEqualTo(ultima);
            verify(pedidoRepository).findWithFiltros(null, null, null, null, null, null, null, null, null, null, requisitada);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

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
        @DisplayName("Caracterização do comportamento atual: ignora o fim informado sem início")
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
        @DisplayName("Deve distinguir IDs de pedido, cliente, responsável e item ao retornar todos os dados")
        void deveRetornarDadosDasEntidadesCorretas() {
            Pedido encontrado = criarPedido(123L);
            encontrado.setCliente(new Cliente(54L, "Ana", "21912345678", true, cliente.getEndereco()));
            encontrado.setUsuarioResponsavel(Usuario.builder().id(28L).nome("Bruno").ativo(true).build());
            encontrado.setStatus(StatusPedido.CONCLUIDO);

            when(pedidoRepository.findById(123L)).thenReturn(Optional.of(encontrado));

            PedidoListagemDto resposta = pedidoService.buscarPorId(123L);

            assertThat(resposta).isEqualTo(new PedidoListagemDto(123L, new ClientePedidoDto(54L, "Ana"),
                    StatusPedido.CONCLUIDO, List.of(new ItemPedidoResponse(100L, 123L, "Coxinha", 100,
                    TipoPreco.CONGELADO, new BigDecimal("0.60"), new BigDecimal("60.00"))),
                    new BigDecimal("5.00"), new BigDecimal("65.00"), DATA_PEDIDO.minusDays(1), DATA_ENTREGA.minusDays(1),
                    TipoEntrega.ENTREGA, new EnderecoDto("Rua A", "10", null, "01001000", "Centro", "Sao Bernardo", "SP"),
                    FormaPagamento.DINHEIRO, new UsuarioPedidoDto(28L, "Bruno")));
            verify(pedidoRepository).findById(123L);
            verifyNoMoreInteractions(pedidoRepository);
            verifyNoInteractions(clienteRepository, usuarioRepository, salgadoRepository);
        }

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
            assertThat(criarResponse(pedido)).usingRecursiveComparison().ignoringFields("status").isEqualTo(anterior);
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
            assertThat(criarResponse(pedido)).usingRecursiveComparison().ignoringFields("status").isEqualTo(anterior);
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
            assertThat(criarResponse(pedido)).usingRecursiveComparison().ignoringFields("status").isEqualTo(anterior);
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

    @Nested
    @DisplayName("Arredondamento dos subtotais do pedido")
    class ArredondamentoSubtotais {
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Deve somar os subtotais já arredondados e acrescentar o frete no cadastro e na edição")
        void deveSomarSubtotaisArredondados(boolean edicao) {
            salgado.setPrecoCentoCongelado(new BigDecimal("0.01"));
            request = criarRequest(TipoEntrega.ENTREGA, new BigDecimal("2.35"), List.of(
                    new ItemPedidoDto(SALGADO_ID, 50, TipoPreco.CONGELADO),
                    new ItemPedidoDto(SALGADO_ID, 50, TipoPreco.CONGELADO)));

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            if (edicao) {
                when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            }

            CriacaoPedidoResponse resposta = edicao
                    ? pedidoService.atualizar(PEDIDO_ID, request) : pedidoService.registrar(request);

            assertThat(resposta.itens()).hasSize(2).allSatisfy(item -> {
                assertThat(item.precoUnitario()).isEqualByComparingTo("0.0001");
                assertThat(item.subTotal()).isEqualByComparingTo("0.01");
            });
            assertThat(resposta.valorTotal()).isEqualByComparingTo("2.37");
            verify(pedidoRepository).save(any(Pedido.class));
        }
    }

    @Nested
    @DisplayName("Validação dos itens recebidos no pedido")
    class ValidacaoItens {
        @ParameterizedTest
        @CsvSource({"id, salgadoId, NotNull", "quantidadeNula, quantidade, NotNull",
                "zero, quantidade, Positive", "negativa, quantidade, Positive", "tipo, tipoPreco, NotNull"})
        @DisplayName("Deve validar os campos de cada item em cascata")
        void deveRejeitarCampoInvalido(String caso, String propriedade, String restricao) {
            ItemPedidoDto item = switch (caso) {
                case "id" -> new ItemPedidoDto(null, 10, TipoPreco.CONGELADO);
                case "quantidadeNula" -> new ItemPedidoDto(SALGADO_ID, null, TipoPreco.CONGELADO);
                case "zero" -> new ItemPedidoDto(SALGADO_ID, 0, TipoPreco.CONGELADO);
                case "negativa" -> new ItemPedidoDto(SALGADO_ID, -1, TipoPreco.CONGELADO);
                default -> new ItemPedidoDto(SALGADO_ID, 10, null);
            };
            var entrada = new CriacaoPedidoRequest(CLIENTE_ID,
                    List.of(new ItemPedidoDto(SALGADO_ID, 1, TipoPreco.CONGELADO), item), null,
                    LocalDate.now(), LocalDateTime.now().plusDays(2), TipoEntrega.RETIRADA,
                    FormaPagamento.PIX, USUARIO_ID, null);

            try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                assertThat(factory.getValidator().validate(entrada))
                        .singleElement().satisfies(violacao -> {
                            assertThat(violacao.getPropertyPath().toString()).isEqualTo("itens[1]." + propriedade);
                            assertThat(violacao.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                                    .isEqualTo(restricao);
                        });
            }
        }

        @Test
        @DisplayName("Deve rejeitar elemento nulo sem confundir com lista ausente")
        void deveRejeitarElementoNulo() {
            var entrada = new CriacaoPedidoRequest(CLIENTE_ID,
                    java.util.Arrays.asList(new ItemPedidoDto(SALGADO_ID, 1, TipoPreco.CONGELADO), null), null,
                    LocalDate.now(), LocalDateTime.now().plusDays(2), TipoEntrega.RETIRADA,
                    FormaPagamento.PIX, USUARIO_ID, null);

            try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                assertThat(factory.getValidator().validate(entrada)).singleElement().satisfies(violacao -> {
                    assertThat(violacao.getPropertyPath().toString()).isEqualTo("itens[1].<list element>");
                    assertThat(violacao.getConstraintDescriptor().getAnnotation())
                            .isInstanceOf(jakarta.validation.constraints.NotNull.class);
                });
            }
        }

        @Test
        @DisplayName("Deve aceitar itens válidos de tipos distintos")
        void deveAceitarItensValidos() {
            var entrada = new CriacaoPedidoRequest(CLIENTE_ID,
                    List.of(new ItemPedidoDto(SALGADO_ID, 1, TipoPreco.CONGELADO),
                            new ItemPedidoDto(99L, 50, TipoPreco.PROCESSADO)), null,
                    LocalDate.now(), LocalDateTime.now().plusDays(2), TipoEntrega.RETIRADA,
                    FormaPagamento.PIX, USUARIO_ID, null);
            try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                assertThat(factory.getValidator().validate(entrada)).isEmpty();
            }
        }
    }

    private CriacaoPedidoRequest criarRequest(TipoEntrega tipoEntrega, BigDecimal frete, List<ItemPedidoDto> itens) {
        return criarRequest(tipoEntrega, frete, itens,
                new EnderecoDto("Rua B", "20", "Casa", "02002000", "Centro", "São Paulo", "SP"));
    }

    private CriacaoPedidoRequest criarRequest(TipoEntrega tipoEntrega, BigDecimal frete, List<ItemPedidoDto> itens,
                                              EnderecoDto endereco) {
        return new CriacaoPedidoRequest(CLIENTE_ID, itens,
                endereco,
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

    @Nested
    @DisplayName("Capacidade monetária do pedido")
    class LimitesMonetarios {
        @ParameterizedTest
        @CsvSource({
                "false,99999999.99,100,1,0.00,99999999.99,aceito",
                "true,99999999.99,100,1,0.00,99999999.99,aceito",
                "false,99999999.99,99,1,1000000.00,99999999.99,aceito",
                "true,99999999.99,99,1,1000000.00,99999999.99,aceito",
                "false,99999999.99,99,1,1000000.01,0,O total do pedido",
                "true,99999999.99,99,1,1000000.01,0,O total do pedido",
                "false,50000000.00,200,1,0.00,0,O subtotal do item do pedido",
                "true,50000000.00,200,1,0.00,0,O subtotal do item do pedido",
                "false,50000000.00,100,2,0.00,0,O total do pedido",
                "true,50000000.00,100,2,0.00,0,O total do pedido",
                "false,0.50,1,2,99999999.98,0,O total do pedido",
                "true,0.50,1,2,99999999.98,0,O total do pedido",
                "false,0.49,1,2,99999999.99,99999999.99,aceito",
                "true,0.49,1,2,99999999.99,99999999.99,aceito"
        })
        @DisplayName("Deve validar subtotal arredondado e total com frete no cadastro e na edição")
        void deveRespeitarCapacidade(boolean edicao, String cento, int quantidade, int numeroItens,
                                    String frete, String esperado, String resultado) {
            salgado.setPrecoCentoCongelado(new BigDecimal(cento));
            var item = new ItemPedidoDto(SALGADO_ID, quantidade, TipoPreco.CONGELADO);
            var entrada = criarRequest(TipoEntrega.ENTREGA, new BigDecimal(frete),
                    java.util.Collections.nCopies(numeroItens, item));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(salgadoRepository.findById(SALGADO_ID)).thenReturn(Optional.of(salgado));
            if (edicao) when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
            if (resultado.equals("aceito")) {
                var resposta = edicao ? pedidoService.atualizar(PEDIDO_ID, entrada) : pedidoService.registrar(entrada);
                assertThat(resposta.valorTotal()).isEqualByComparingTo(esperado);
                verify(pedidoRepository).save(any(Pedido.class));
            } else {
                assertThatThrownBy(() -> {
                    if (edicao) pedidoService.atualizar(PEDIDO_ID, entrada);
                    else pedidoService.registrar(entrada);
                }).isInstanceOf(BusinessException.class)
                        .hasMessage(resultado + " excede o limite de R$ 99.999.999,99.");
                verify(pedidoRepository, never()).save(any(Pedido.class));
            }
        }
    }

}
