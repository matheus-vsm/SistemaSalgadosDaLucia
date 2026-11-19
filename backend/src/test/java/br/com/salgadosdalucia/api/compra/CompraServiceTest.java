package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.*;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompraService")
class CompraServiceTest {

    private static final Long COMPRA_ID = 10L;
    private static final LocalDate DATA_COMPRA = LocalDate.of(2026, 4, 15);

    @Mock
    private CompraRepository compraRepository;
    @InjectMocks
    private CompraService compraService;

    private Compra compra;
    private CriacaoCompraRequest request;
    private Pageable paginacao;

    @BeforeEach
    void setUp() {
        compra = criarCompra(COMPRA_ID);
        request = new CriacaoCompraRequest(List.of(
                new ItemCompraRequest("Farinha", 3, new BigDecimal("4.25")),
                new ItemCompraRequest("Óleo", 2, new BigDecimal("7.10"))),
                DATA_COMPRA, "Compra para produção");
        paginacao = PageRequest.of(0, 2);
    }

    @Nested
    @DisplayName("Registrar compra")
    class RegistrarCompra {
        @ParameterizedTest
        @CsvSource({"1, 0.01, 0.01", "3, 0.10, 0.30", "7, 12.34, 86.38", "137, 7.29, 998.73"})
        @DisplayName("Deve calcular um item sem perda de centavos e retornar os IDs atribuídos ao salvar")
        void deveCalcularCompraComUmItem(int quantidade, String unitario, String totalEsperado) {
            var dados = new CriacaoCompraRequest(List.of(new ItemCompraRequest("Arroz", quantidade,
                    new BigDecimal(unitario))), DATA_COMPRA, "Reposição");

            when(compraRepository.save(any(Compra.class))).thenAnswer(invocacao -> {
                Compra nova = invocacao.getArgument(0);
                assertThat(nova.getId()).isNull();
                assertThat(nova.getItens()).singleElement().satisfies(item -> {
                    assertThat(item.getId()).isNull();
                    assertThat(item.getCompra()).isSameAs(nova);
                    assertThat(item.getNome()).isEqualTo("Arroz");
                    assertThat(item.getQuantidade()).isEqualTo(quantidade);
                    assertThat(item.getValorUnitario()).isEqualByComparingTo(unitario);
                    assertThat(item.getSubTotal()).isEqualByComparingTo(totalEsperado);
                    item.setId(501L);
                });

                assertThat(nova.getValorTotal()).isEqualByComparingTo(totalEsperado);
                nova.setId(52L);
                return nova;
            });

            CompraResponse resposta = compraService.registrarCompra(dados);

            assertThat(resposta).isEqualTo(new CompraResponse(52L, List.of(new ItemCompraResponse(
                    501L, "Arroz", quantidade, new BigDecimal(unitario), new BigDecimal(totalEsperado))),
                    new BigDecimal(totalEsperado), DATA_COMPRA, "Reposição"));
            verify(compraRepository).save(any(Compra.class));
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve somar subtotais distintos de vários itens sem arredondamento indevido")
        void deveSomarVariosItensComCentavos() {
            var dados = new CriacaoCompraRequest(itensComCentavos(), DATA_COMPRA, "Itens variados");

            when(compraRepository.save(any(Compra.class))).thenAnswer(invocacao -> {
                Compra nova = invocacao.getArgument(0);
                nova.setId(52L);
                nova.getItens().forEach(item -> assertThat(item.getCompra()).isSameAs(nova));
                return nova;
            });

            CompraResponse resposta = compraService.registrarCompra(dados);

            assertThat(resposta.itens()).containsExactly(
                    new ItemCompraResponse(null, "Farinha", 3, new BigDecimal("0.10"), new BigDecimal("0.30")),
                    new ItemCompraResponse(null, "Arroz", 2, new BigDecimal("0.20"), new BigDecimal("0.40")),
                    new ItemCompraResponse(null, "Sal", 1, new BigDecimal("0.01"), new BigDecimal("0.01")));
            assertThat(resposta.valorTotal()).isEqualByComparingTo("0.71");
            assertThat(resposta.id()).isEqualTo(52L);
            verify(compraRepository).save(any(Compra.class));
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve cadastrar itens, calcular valores e retornar os dados salvos")
        void deveRegistrarCompra() throws NotFoundException {
            // quando alguém chamar save passando qualquer objeto do tipo Compra
            when(compraRepository.save(any(Compra.class)))
                    // thenAnswer permite definir o comportamento do mock usando os dados recebidos na chamada.
                    // a variável invocacao representa essa chamada ao save.
                    .thenAnswer(invocacao -> { // executa o código a seguir
                        Compra salva = invocacao.getArgument(0);
                        salva.setId(COMPRA_ID);
                        salva.getItens().get(0).setId(100L);
                        salva.getItens().get(1).setId(101L);
                        return salva;
                    });

            CompraResponse resposta = compraService.registrarCompra(request);

            // captura a compra enviada ao banco para conferir os itens e seus vínculos
            ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
            verify(compraRepository).save(captor.capture());

            Compra salva = captor.getValue();
            assertThat(salva.getDataCompra()).isEqualTo(DATA_COMPRA);
            assertThat(salva.getObservacao()).isEqualTo(request.observacao());
            assertThat(salva.getValorTotal()).isEqualByComparingTo("26.95");
            assertThat(salva.getItens()).hasSize(2);

            ItemCompra farinha = salva.getItens().getFirst();
            assertThat(farinha.getCompra()).isSameAs(salva);
            assertThat(farinha.getNome()).isEqualTo("Farinha");
            assertThat(farinha.getQuantidade()).isEqualTo(3);
            assertThat(farinha.getValorUnitario()).isEqualByComparingTo("4.25");
            assertThat(farinha.getSubTotal()).isEqualByComparingTo("12.75");

            ItemCompra oleo = salva.getItens().get(1);
            assertThat(oleo.getCompra()).isSameAs(salva);
            assertThat(oleo.getSubTotal()).isEqualByComparingTo("14.20");
            assertThat(resposta).isEqualTo(criarResponse(salva));
            assertThat(resposta.id()).isEqualTo(COMPRA_ID);
            assertThat(resposta.itens().get(0).id()).isEqualTo(100L);
            assertThat(resposta.itens().get(1).id()).isEqualTo(101L);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve cadastrar a compra e calcular os valores dos itens - Segunda forma")
        void deveRegistrarCompra2() {
            CompraResponse resposta = compraService.registrarCompra(request);

            // captura a compra enviada ao repositório.
            ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
            verify(compraRepository).save(captor.capture());

            Compra compraSalva = captor.getValue();

            assertThat(compraSalva.getDataCompra()).isEqualTo(DATA_COMPRA);
            assertThat(compraSalva.getObservacao()).isEqualTo(request.observacao());
            assertThat(compraSalva.getValorTotal()).isEqualByComparingTo("26.95");
            assertThat(compraSalva.getItens()).hasSize(2);

            ItemCompra farinha = compraSalva.getItens().getFirst();
            assertThat(farinha.getCompra()).isSameAs(compraSalva);
            assertThat(farinha.getNome()).isEqualTo("Farinha");
            assertThat(farinha.getQuantidade()).isEqualTo(3);
            assertThat(farinha.getValorUnitario()).isEqualByComparingTo("4.25");
            assertThat(farinha.getSubTotal()).isEqualByComparingTo("12.75");

            ItemCompra oleo = compraSalva.getItens().get(1);
            assertThat(oleo.getCompra()).isSameAs(compraSalva);
            assertThat(oleo.getNome()).isEqualTo("Óleo");
            assertThat(oleo.getQuantidade()).isEqualTo(2);
            assertThat(oleo.getValorUnitario()).isEqualByComparingTo("7.10");
            assertThat(oleo.getSubTotal()).isEqualByComparingTo("14.20");

            assertThat(resposta).isEqualTo(criarResponse(compraSalva));

            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve encaminhar data nula ao save sem simular o callback de persistência")
        void deveRegistrarSemData() throws NotFoundException {
            CriacaoCompraRequest dados = new CriacaoCompraRequest(request.itens(), null, null);

            when(compraRepository.save(any(Compra.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            CompraResponse resposta = compraService.registrarCompra(dados);

            ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
            verify(compraRepository).save(captor.capture());

            Compra salva = captor.getValue();
            assertThat(salva.getDataCompra()).isNull();
            assertThat(resposta.dataCompra()).isNull();
            assertThat(resposta.observacao()).isNull();
            assertThat(resposta).isEqualTo(criarResponse(salva));
            verifyNoMoreInteractions(compraRepository);
        }

        @Nested
        @DisplayName("Caracterizar chamadas diretas fora do contrato de validação do DTO")
        class ForaDoContrato {
            @Test
            @DisplayName("Deve caracterizar total zero na chamada direta com lista vazia, sem validar o DTO")
            void deveCalcularCompraSemItens() throws NotFoundException {
                CriacaoCompraRequest dados = new CriacaoCompraRequest(List.of(), DATA_COMPRA, "Sem itens");

                when(compraRepository.save(any(Compra.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

                CompraResponse resposta = compraService.registrarCompra(dados);

                ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
                verify(compraRepository).save(captor.capture());

                Compra salva = captor.getValue();
                assertThat(resposta.itens()).isEmpty();
                assertThat(resposta.valorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(resposta).isEqualTo(criarResponse(salva));
                verifyNoMoreInteractions(compraRepository);
            }
        }

        @Test
        @DisplayName("Deve calcular centavos sem perder precisão")
        void deveCalcularValorMinimo() throws NotFoundException {
            CriacaoCompraRequest dados = new CriacaoCompraRequest(List.of(new ItemCompraRequest("Sal", 3, new BigDecimal("0.01"))), DATA_COMPRA, null);

            when(compraRepository.save(any(Compra.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            CompraResponse resposta = compraService.registrarCompra(dados);

            ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
            verify(compraRepository).save(captor.capture());

            Compra salva = captor.getValue();
            assertThat(resposta.valorTotal()).isEqualByComparingTo("0.03");
            assertThat(resposta.itens().getFirst().subTotal()).isEqualByComparingTo("0.03");
            assertThat(resposta).isEqualTo(criarResponse(salva));
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco sem repetir o cadastro")
        void deveRetornarErroAoSalvar() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco = new DataAccessResourceFailureException("Banco indisponível");

            when(compraRepository.save(any(Compra.class))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> compraService.registrarCompra(request)).isSameAs(erroDoBanco);

            verify(compraRepository).save(any(Compra.class));
            verifyNoMoreInteractions(compraRepository);
        }
    }

    @Nested
    @DisplayName("Editar compra")
    class EditarCompra {
        @ParameterizedTest
        @CsvSource({"1, 0.01, 0.01", "3, 0.10, 0.30", "7, 12.34, 86.38", "137, 7.29, 998.73"})
        @DisplayName("Deve recalcular um único item sem acumular o total anterior e preservar ID e data omitida")
        void deveRecalcularUmItem(int quantidade, String unitario, String totalEsperado) {
            var dados = new CriacaoCompraRequest(List.of(new ItemCompraRequest("Arroz", quantidade,
                    new BigDecimal(unitario))), null, null);

            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));
            when(compraRepository.save(compra)).thenAnswer(invocacao -> {
                assertThat(compra.getItens()).singleElement().satisfies(item -> {
                    assertThat(item.getId()).isNull();
                    assertThat(item.getCompra()).isSameAs(compra);
                    item.setId(502L);
                });
                return compra;
            });

            CompraResponse resposta = compraService.editarCompra(COMPRA_ID, dados);

            assertThat(resposta).isEqualTo(new CompraResponse(COMPRA_ID, List.of(new ItemCompraResponse(
                    502L, "Arroz", quantidade, new BigDecimal(unitario), new BigDecimal(totalEsperado))),
                    new BigDecimal(totalEsperado), DATA_COMPRA.minusDays(1), null));
            assertThat(compra.getValorTotal()).isEqualByComparingTo(totalEsperado);
            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository).save(compra);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve substituir vários itens antigos e a data sem acumular subtotais anteriores")
        void deveSubstituirVariosItens() {
            compra.getItens().add(ItemCompra.builder().id(333L).compra(compra).nome("Antigo")
                    .quantidade(1).valorUnitario(new BigDecimal("990.00")).subTotal(new BigDecimal("990.00")).build());
            compra.setValorTotal(new BigDecimal("1000.00"));

            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));
            when(compraRepository.save(compra)).thenAnswer(invocacao -> {
                assertThat(compra.getItens()).hasSize(3).allSatisfy(item -> {
                    assertThat(item.getId()).isNull();
                    assertThat(item.getCompra()).isSameAs(compra);
                });
                compra.getItens().get(0).setId(600L);
                compra.getItens().get(1).setId(601L);
                compra.getItens().get(2).setId(602L);
                return compra;
            });

            CompraResponse resposta = compraService.editarCompra(COMPRA_ID,
                    new CriacaoCompraRequest(itensComCentavos(), DATA_COMPRA, "Novos itens"));

            assertThat(resposta.itens()).containsExactly(
                    new ItemCompraResponse(600L, "Farinha", 3, new BigDecimal("0.10"), new BigDecimal("0.30")),
                    new ItemCompraResponse(601L, "Arroz", 2, new BigDecimal("0.20"), new BigDecimal("0.40")),
                    new ItemCompraResponse(602L, "Sal", 1, new BigDecimal("0.01"), new BigDecimal("0.01")));
            assertThat(resposta.id()).isEqualTo(COMPRA_ID);
            assertThat(resposta.valorTotal()).isEqualByComparingTo("0.71");
            assertThat(resposta.dataCompra()).isEqualTo(DATA_COMPRA);
            assertThat(resposta.observacao()).isEqualTo("Novos itens");
            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository).save(compra);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve substituir os itens antigos e recalcular o total")
        void deveEditarCompra() throws NotFoundException {
            ItemCompra itemAntigo = compra.getItens().getFirst();
            List<ItemCompra> listaOriginal = compra.getItens();

            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));
            when(compraRepository.save(compra)).thenReturn(compra);

            CompraResponse resposta = compraService.editarCompra(COMPRA_ID, request);

            assertThat(compra.getId()).isEqualTo(COMPRA_ID);
            assertThat(compra.getDataCompra()).isEqualTo(DATA_COMPRA);
            assertThat(compra.getObservacao()).isEqualTo(request.observacao());
            assertThat(compra.getItens()).isSameAs(listaOriginal).hasSize(2).doesNotContain(itemAntigo);
            assertThat(compra.getItens().get(0).getCompra()).isSameAs(compra);
            assertThat(compra.getItens().get(1).getCompra()).isSameAs(compra);
            assertThat(compra.getItens().get(0).getNome()).isEqualTo("Farinha");
            assertThat(compra.getItens().get(1).getNome()).isEqualTo("Óleo");
            assertThat(compra.getItens().get(0).getQuantidade()).isEqualTo(3);
            assertThat(compra.getItens().get(0).getValorUnitario()).isEqualByComparingTo("4.25");
            assertThat(compra.getItens().get(0).getSubTotal()).isEqualByComparingTo("12.75");
            assertThat(compra.getItens().get(1).getSubTotal()).isEqualByComparingTo("14.20");
            assertThat(compra.getValorTotal()).isEqualByComparingTo("26.95");
            assertThat(resposta).isEqualTo(criarResponse(compra));
            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository).save(compra);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve manter data anterior e remover observação quando forem recebidos valores nulos")
        void deveManterDataERemoverObservacao() throws NotFoundException {
            CriacaoCompraRequest dados = new CriacaoCompraRequest(request.itens(), null, null);

            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));
            when(compraRepository.save(compra)).thenReturn(compra);

            CompraResponse resposta = compraService.editarCompra(COMPRA_ID, dados);

            assertThat(resposta.dataCompra()).isEqualTo(DATA_COMPRA.minusDays(1));
            assertThat(resposta.observacao()).isNull();
            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository).save(compra);
            verifyNoMoreInteractions(compraRepository);
        }

        @Nested
        @DisplayName("Caracterizar chamadas diretas fora do contrato de validação do DTO")
        class ForaDoContrato {
            @Test
            @DisplayName("Deve caracterizar remoção dos itens na chamada direta com lista vazia, sem validar o DTO")
            void deveZerarTotalSemItens() throws NotFoundException {
                CriacaoCompraRequest dados = new CriacaoCompraRequest(List.of(), DATA_COMPRA, "Sem itens");

                when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));
                when(compraRepository.save(compra)).thenReturn(compra);

                CompraResponse resposta = compraService.editarCompra(COMPRA_ID, dados);

                assertThat(compra.getItens()).isEmpty();
                assertThat(resposta.itens()).isEmpty();
                assertThat(resposta.valorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
                verify(compraRepository).findById(COMPRA_ID);
                verify(compraRepository).save(compra);
                verifyNoMoreInteractions(compraRepository);
            }
        }

        @Test
        @DisplayName("Deve retornar a entidade devolvida pelo repositório na edição")
        void deveRetornarDadosDevolvidosPeloSave() throws NotFoundException {
            Compra compraSalva = criarCompra(20L);
            compraSalva.setObservacao("Dados retornados pelo banco");

            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));
            when(compraRepository.save(compra)).thenReturn(compraSalva);

            CompraResponse resposta = compraService.editarCompra(COMPRA_ID, request);

            assertThat(resposta).isEqualTo(criarResponse(compraSalva));
            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository).save(compra);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve avisar quando a compra não existir")
        void deveRejeitarCompraInexistente() throws NotFoundException {
            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compraService.editarCompra(COMPRA_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Compra não encontrado com o ID: %s", COMPRA_ID);

            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository, never()).save(any(Compra.class));
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na busca")
        void deveRetornarErroNaBusca() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco = new DataAccessResourceFailureException("Banco indisponível");

            when(compraRepository.findById(COMPRA_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> compraService.editarCompra(COMPRA_ID, request)).isSameAs(erroDoBanco);

            verify(compraRepository).findById(COMPRA_ID);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco ao salvar edição")
        void deveRetornarErroAoSalvar() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco = new DataAccessResourceFailureException("Banco indisponível");

            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));
            when(compraRepository.save(compra)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> compraService.editarCompra(COMPRA_ID, request)).isSameAs(erroDoBanco);

            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository).save(compra);
            verifyNoMoreInteractions(compraRepository);
        }
    }

    @Nested
    @DisplayName("Buscar compra por ID")
    class BuscarPorId {
        @Test
        @DisplayName("Deve retornar os dados completos da compra encontrada")
        void deveBuscarCompra() throws NotFoundException {
            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.of(compra));

            CompraResponse resposta = compraService.buscarPorId(COMPRA_ID);

            assertThat(resposta).isEqualTo(criarResponse(compra));
            assertThat(resposta.id()).isEqualTo(COMPRA_ID);
            assertThat(resposta.itens().getFirst().id()).isEqualTo(100L);
            verify(compraRepository).findById(COMPRA_ID);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve avisar quando a compra não existir")
        void deveRejeitarCompraInexistente() throws NotFoundException {
            when(compraRepository.findById(COMPRA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> compraService.buscarPorId(COMPRA_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Compra não encontrada com id: %s", COMPRA_ID);

            verify(compraRepository).findById(COMPRA_ID);
            verify(compraRepository, never()).save(any(Compra.class));
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na busca")
        void deveRetornarErroNaBusca() throws NotFoundException {
            DataAccessResourceFailureException erroDoBanco = new DataAccessResourceFailureException("Banco indisponível");

            when(compraRepository.findById(COMPRA_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> compraService.buscarPorId(COMPRA_ID)).isSameAs(erroDoBanco);

            verify(compraRepository).findById(COMPRA_ID);
            verifyNoMoreInteractions(compraRepository);
        }
    }

    @Nested
    @DisplayName("Listar compras com filtros")
    class ListarComFiltro {
        @ParameterizedTest
        @MethodSource("br.com.salgadosdalucia.api.compra.CompraServiceTest#filtrosValidos")
        @DisplayName("Deve encaminhar combinações válidas de filtros sem alterar os valores informados")
        void deveEncaminharFiltros(CompraFiltroDto filtro, LocalDate fimEsperado) {
            when(compraRepository.findAllWithFiltros(filtro.dataInicioCompra(), fimEsperado,
                    filtro.nomeItem(), filtro.observacao(), paginacao)).thenReturn(Page.empty(paginacao));

            Page<CompraResponse> resposta = compraService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            assertThat(resposta.getTotalPages()).isZero();
            verify(compraRepository).findAllWithFiltros(filtro.dataInicioCompra(), fimEsperado,
                    filtro.nomeItem(), filtro.observacao(), paginacao);
            verifyNoMoreInteractions(compraRepository);
        }

        @ParameterizedTest
        @CsvSource({"1, 2, 7, false", "3, 1, 7, true", "4, 0, 7, true"})
        @DisplayName("Deve preservar conteúdo e metadados de páginas posteriores, parciais ou vazias")
        void devePreservarPaginas(int pagina, int quantidade, long total, boolean ultima) {
            Pageable requisitada = PageRequest.of(pagina, 2, Sort.by("dataCompra").descending());
            Compra compra2 = criarCompra(21L);
            compra2.setDataCompra(DATA_COMPRA.minusDays(4));
            List<Compra> conteudo = switch (quantidade) {
                case 2 -> List.of(compra, compra2);
                case 1 -> List.of(compra2);
                default -> List.of();
            };
            var filtro = new CompraFiltroDto(DATA_COMPRA.minusDays(10), DATA_COMPRA, null, null);

            when(compraRepository.findAllWithFiltros(filtro.dataInicioCompra(), DATA_COMPRA, null, null, requisitada))
                    .thenReturn(new PageImpl<>(conteudo, requisitada, total));

            Page<CompraResponse> resposta = compraService.listarComFiltro(filtro, requisitada);

            assertThat(resposta.getContent()).containsExactlyElementsOf(conteudo.stream().map(CompraServiceTest.this::criarResponse).toList());
            assertThat(resposta.getPageable()).isEqualTo(requisitada);
            assertThat(resposta.getNumber()).isEqualTo(pagina);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            assertThat(resposta.isLast()).isEqualTo(ultima);
            verify(compraRepository).findAllWithFiltros(filtro.dataInicioCompra(), DATA_COMPRA, null, null, requisitada);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve encaminhar filtros e mapear a página preservando a paginação")
        void deveListarCompras() throws NotFoundException {
            LocalDate fim = DATA_COMPRA.plusDays(5);
            CompraFiltroDto filtro = new CompraFiltroDto(DATA_COMPRA, fim, "Farinha", "produção");
            Compra compra2 = criarCompra(20L);

            when(compraRepository.findAllWithFiltros(DATA_COMPRA, fim, "Farinha", "produção", paginacao))
                    .thenReturn(new PageImpl<>(List.of(compra, compra2), paginacao, 5));

            Page<CompraResponse> resposta = compraService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).containsExactly(criarResponse(compra), criarResponse(compra2));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(compraRepository).findAllWithFiltros(DATA_COMPRA, fim, "Farinha", "produção", paginacao);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve usar o mesmo dia quando apenas o início for informado")
        void deveFiltrarUmDia() throws NotFoundException {
            CompraFiltroDto filtro = new CompraFiltroDto(DATA_COMPRA, null, null, null);

            when(compraRepository.findAllWithFiltros(DATA_COMPRA, DATA_COMPRA, null, null, paginacao))
                    .thenReturn(Page.empty(paginacao));

            Page<CompraResponse> resposta = compraService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            verify(compraRepository).findAllWithFiltros(DATA_COMPRA, DATA_COMPRA, null, null, paginacao);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver filtros")
        void deveListarSemFiltros() throws NotFoundException {
            CompraFiltroDto filtro = new CompraFiltroDto(null, null, null, null);

            when(compraRepository.findAllWithFiltros(null, null, null, null, paginacao))
                    .thenReturn(Page.empty(paginacao));

            Page<CompraResponse> resposta = compraService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            verify(compraRepository).findAllWithFiltros(null, null, null, null, paginacao);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve encaminhar a data final mesmo sem data inicial")
        void deveEncaminharFimSemInicio() throws NotFoundException {
            CompraFiltroDto filtro = new CompraFiltroDto(null, DATA_COMPRA, null, null);

            when(compraRepository.findAllWithFiltros(null, DATA_COMPRA, null, null, paginacao))
                    .thenReturn(Page.empty(paginacao));

            Page<CompraResponse> resposta = compraService.listarComFiltro(filtro, paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            verify(compraRepository).findAllWithFiltros(null, DATA_COMPRA, null, null, paginacao);
            verifyNoMoreInteractions(compraRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na listagem")
        void deveRetornarErroNaListagem() throws NotFoundException {
            CompraFiltroDto filtro = new CompraFiltroDto(null, null, null, null);
            DataAccessResourceFailureException erroDoBanco = new DataAccessResourceFailureException("Banco indisponível");

            when(compraRepository.findAllWithFiltros(null, null, null, null, paginacao)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> compraService.listarComFiltro(filtro, paginacao)).isSameAs(erroDoBanco);

            verify(compraRepository).findAllWithFiltros(null, null, null, null, paginacao);
            verifyNoMoreInteractions(compraRepository);
        }
    }

    static Stream<Arguments> filtrosValidos() {
        return Stream.of(
                Arguments.of(new CompraFiltroDto(null, null, null, null), null),
                Arguments.of(new CompraFiltroDto(null, null, "Farinha", null), null),
                Arguments.of(new CompraFiltroDto(null, null, null, "produção"), null),
                Arguments.of(new CompraFiltroDto(DATA_COMPRA, null, "Óleo", null), DATA_COMPRA),
                Arguments.of(new CompraFiltroDto(DATA_COMPRA.minusDays(2), DATA_COMPRA, null, "produção"), DATA_COMPRA));
    }

    private List<ItemCompraRequest> itensComCentavos() {
        return List.of(new ItemCompraRequest("Farinha", 3, new BigDecimal("0.10")),
                new ItemCompraRequest("Arroz", 2, new BigDecimal("0.20")),
                new ItemCompraRequest("Sal", 1, new BigDecimal("0.01")));
    }

    private Compra criarCompra(Long id) {
        Compra entidade = Compra.builder().id(id).itens(new ArrayList<>())
                .dataCompra(DATA_COMPRA.minusDays(1)).observacao("Compra anterior")
                .valorTotal(new BigDecimal("10.00")).build();
        ItemCompra item = ItemCompra.builder().id(100L).compra(entidade).nome("Sal")
                .quantidade(2).valorUnitario(new BigDecimal("5.00"))
                .subTotal(new BigDecimal("10.00")).build();
        entidade.getItens().add(item);
        return entidade;
    }

    private CompraResponse criarResponse(Compra entidade) {
        List<ItemCompraResponse> itens = entidade.getItens().stream()
                .map(item -> new ItemCompraResponse(item.getId(), item.getNome(), item.getQuantidade(),
                        item.getValorUnitario(), item.getSubTotal())).toList();
        return new CompraResponse(entidade.getId(), itens, entidade.getValorTotal(),
                entidade.getDataCompra(), entidade.getObservacao());
    }

}
