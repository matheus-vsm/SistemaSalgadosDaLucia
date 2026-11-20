package br.com.salgadosdalucia.api.salgado;

import br.com.salgadosdalucia.api.estoque.Estoque;
import br.com.salgadosdalucia.api.estoque.EstoqueRepository;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.salgado.dto.EstoqueSalgadoDto;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoRequest;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoResponse;
import br.com.salgadosdalucia.api.salgado.enums.Categoria;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalgadoService")
class SalgadoServiceTest {

    private static final Long ID = 1L;

    @Mock
    private SalgadoRepository salgadoRepository;
    @Mock
    private EstoqueRepository estoqueRepository;
    @InjectMocks
    private SalgadoService salgadoService;

    private Salgado salgado;
    private SalgadoRequest dto;
    private Estoque estoque;
    private Pageable paginacao;

    @BeforeEach
    void setUp() {
        salgado = criarSalgado(ID, "Coxinha", true);
        dto = criarDto();
        estoque = criarEstoque(10L, salgado, 150);
        paginacao = PageRequest.of(0, 2);
    }

    @Nested
    @DisplayName("Cadastrar salgado")
    class Cadastrar {
        @ParameterizedTest
        @CsvSource({"FRITO, 61.23, 87.65", "ASSADO, 42.10, 95.43", "FRITO, 0.01, 0.02"})
        @DisplayName("Deve preservar categoria e preços distintos e criar estoque para a entidade devolvida ao salvar")
        void deveCadastrarComEstoqueVinculadoAoSalgadoSalvo(Categoria categoria, String congelado, String processado) {
            SalgadoRequest dados = new SalgadoRequest("Salgado especial", "Recheio especial", categoria,
                    new BigDecimal(congelado), new BigDecimal(processado));
            Salgado persistido = new Salgado(37L, "Salgado especial", "Recheio especial", categoria,
                    new BigDecimal(congelado), new BigDecimal(processado), true);
            Estoque estoqueSalvo = new Estoque(94L, persistido, 0);

            when(salgadoRepository.save(any(Salgado.class))).thenReturn(persistido);
            when(estoqueRepository.save(any(Estoque.class))).thenReturn(estoqueSalvo);

            SalgadoResponse resposta = salgadoService.cadastrar(dados);

            ArgumentCaptor<Salgado> salgadoEnviado = ArgumentCaptor.forClass(Salgado.class);
            ArgumentCaptor<Estoque> estoqueEnviado = ArgumentCaptor.forClass(Estoque.class);
            var ordem = inOrder(salgadoRepository, estoqueRepository);
            ordem.verify(salgadoRepository).save(salgadoEnviado.capture());
            ordem.verify(estoqueRepository).save(estoqueEnviado.capture());

            assertThat(salgadoEnviado.getValue()).usingRecursiveComparison().isEqualTo(new Salgado(null,
                    "Salgado especial", "Recheio especial", categoria, new BigDecimal(congelado), new BigDecimal(processado), true));
            assertThat(estoqueEnviado.getValue().getId()).isNull();
            assertThat(estoqueEnviado.getValue().getSalgado()).isSameAs(persistido);
            assertThat(estoqueEnviado.getValue().getQuantidade()).isZero();
            assertThat(resposta).isEqualTo(new SalgadoResponse(37L, "Salgado especial", "Recheio especial", categoria,
                    new BigDecimal(congelado), new BigDecimal(processado), true, new EstoqueSalgadoDto(94L, 0)));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve salvar um salgado ativo e criar seu estoque zerado")
        void deveCadastrar() {
            Estoque estoqueSalvo = criarEstoque(10L, salgado, 0);

            when(salgadoRepository.save(any(Salgado.class))).thenReturn(salgado);
            when(estoqueRepository.save(any(Estoque.class))).thenReturn(estoqueSalvo);

            SalgadoResponse resposta = salgadoService.cadastrar(dto);

            // captura os objetos enviados aos repositórios para conferir os dados salvos
            ArgumentCaptor<Salgado> salgadoEnviado = ArgumentCaptor.forClass(Salgado.class);
            verify(salgadoRepository).save(salgadoEnviado.capture());

            Salgado salgadoSalvo = salgadoEnviado.getValue();
            assertThat(salgadoSalvo.getId()).isNull();
            assertThat(salgadoSalvo.getNome()).isEqualTo(dto.nome());
            assertThat(salgadoSalvo.getDescricao()).isEqualTo(dto.descricao());
            assertThat(salgadoSalvo.getCategoria()).isEqualTo(dto.categoria());
            assertThat(salgadoSalvo.getPrecoCentoCongelado()).isEqualTo(dto.precoCentoCongelado());
            assertThat(salgadoSalvo.getPrecoCentoProcessado()).isEqualTo(dto.precoCentoProcessado());
            assertThat(salgadoSalvo.isAtivo()).isTrue();

            ArgumentCaptor<Estoque> estoqueEnviado = ArgumentCaptor.forClass(Estoque.class);
            verify(estoqueRepository).save(estoqueEnviado.capture());
            assertThat(estoqueEnviado.getValue().getId()).isNull();
            assertThat(estoqueEnviado.getValue().getSalgado()).isSameAs(salgado);
            assertThat(estoqueEnviado.getValue().getQuantidade()).isZero();
            assertThat(resposta).isEqualTo(criarResponse(salgado, estoqueSalvo));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro ao salvar salgado sem criar estoque")
        void deveRetornarErroQuandoSalvarSalgadoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.save(any(Salgado.class))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.cadastrar(dto)).isSameAs(erroDoBanco);

            verify(salgadoRepository).save(any(Salgado.class));
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro ao salvar o estoque")
        void deveRetornarErroQuandoSalvarEstoqueFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(salgadoRepository.save(any(Salgado.class))).thenReturn(salgado);
            when(estoqueRepository.save(any(Estoque.class))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.cadastrar(dto)).isSameAs(erroDoBanco);

            verify(salgadoRepository).save(any(Salgado.class));
            verify(estoqueRepository).save(any(Estoque.class));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }
    }

    @Nested
    @DisplayName("Listar salgados")
    class ListarSalgados {
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("Deve retornar todos os salgados com estoque nulo quando a consulta em lote não encontrar estoques")
        void deveListarSemEstoques(boolean ativo) {
            salgado.setAtivo(ativo);
            Salgado segundo = criarSalgado(23L, "Esfiha", ativo);

            when(salgadoRepository.findAllByAtivo(paginacao, ativo))
                    .thenReturn(new PageImpl<>(List.of(salgado, segundo), paginacao, 2));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID, 23L))).thenReturn(List.of());

            Page<SalgadoResponse> resposta = salgadoService.listarSalgados(paginacao, ativo);

            assertThat(resposta.getContent()).containsExactly(criarResponse(salgado, null), criarResponse(segundo, null));
            assertThat(resposta.getTotalElements()).isEqualTo(2);
            verify(salgadoRepository).findAllByAtivo(paginacao, ativo);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID, 23L));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @ParameterizedTest
        @CsvSource({"true, 1, 2, 7, false", "false, 3, 1, 7, true", "true, 4, 0, 7, true"})
        @DisplayName("Deve preservar páginas posteriores e associar estoques pelo ID sem confundir saldo zero com ausência")
        void devePreservarPaginas(boolean ativo, int pagina, int quantidade, long total, boolean ultima) {
            Pageable requisitada = PageRequest.of(pagina, 2, Sort.by("nome").descending());
            salgado.setAtivo(ativo);
            Salgado segundo = new Salgado(23L, "Esfiha", "Carne", Categoria.ASSADO,
                    new BigDecimal("73.21"), new BigDecimal("92.34"), ativo);
            Estoque segundoEstoque = new Estoque(81L, segundo, 0);
            List<Salgado> conteudo = switch (quantidade) {
                case 2 -> List.of(segundo, salgado);
                case 1 -> List.of(segundo);
                default -> List.of();
            };

            when(salgadoRepository.findAllByAtivo(requisitada, ativo))
                    .thenReturn(new PageImpl<>(conteudo, requisitada, total));
            if (quantidade == 2) {
                when(estoqueRepository.findAllBySalgadoIdIn(List.of(23L, ID))).thenReturn(List.of(estoque, segundoEstoque));
            } else if (quantidade == 1) {
                when(estoqueRepository.findAllBySalgadoIdIn(List.of(23L))).thenReturn(List.of(segundoEstoque));
            }

            Page<SalgadoResponse> resposta = salgadoService.listarSalgados(requisitada, ativo);

            List<SalgadoResponse> esperados = switch (quantidade) {
                case 2 -> List.of(criarResponse(segundo, segundoEstoque), criarResponse(salgado, estoque));
                case 1 -> List.of(criarResponse(segundo, segundoEstoque));
                default -> List.of();
            };
            assertThat(resposta.getContent()).containsExactlyElementsOf(esperados);
            assertThat(resposta.getPageable()).isEqualTo(requisitada);
            assertThat(resposta.getNumber()).isEqualTo(pagina);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            assertThat(resposta.isLast()).isEqualTo(ultima);
            verify(salgadoRepository).findAllByAtivo(requisitada, ativo);
            if (quantidade == 0) {
                verifyNoInteractions(estoqueRepository);
            } else {
                verify(estoqueRepository).findAllBySalgadoIdIn(quantidade == 2 ? List.of(23L, ID) : List.of(23L));
            }
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar os salgados com seus estoques e os dados da página")
        void deveListarAtivos() {
            Salgado segundoSalgado = criarSalgado(2L, "Bolinha de queijo", true);
            Estoque segundoEstoque = criarEstoque(20L, segundoSalgado, 80);

            when(salgadoRepository.findAllByAtivo(paginacao, true))
                    .thenReturn(new PageImpl<>(List.of(salgado, segundoSalgado), paginacao, 5));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID, 2L)))
                    .thenReturn(List.of(segundoEstoque, estoque));

            Page<SalgadoResponse> resposta = salgadoService.listarSalgados(paginacao, true);

            assertThat(resposta.getContent()).containsExactly(
                    criarResponse(salgado, estoque), criarResponse(segundoSalgado, segundoEstoque));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(salgadoRepository).findAllByAtivo(paginacao, true);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID, 2L));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve listar salgados inativos com seus estoques")
        void deveListarInativos() {
            salgado.setAtivo(false);
            Salgado segundoSalgado = criarSalgado(2L, "Coxinha de queijo", false);
            Estoque segundoEstoque = criarEstoque(20L, segundoSalgado, 80);

            when(salgadoRepository.findAllByAtivo(paginacao, false))
                    .thenReturn(new PageImpl<>(List.of(salgado, segundoSalgado), paginacao, 5));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId())))
                    .thenReturn(List.of(segundoEstoque, estoque));

            Page<SalgadoResponse> resposta = salgadoService.listarSalgados(paginacao, false);

            assertThat(resposta.getContent()).containsExactly(
                    criarResponse(salgado, estoque), criarResponse(segundoSalgado, segundoEstoque));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(salgadoRepository).findAllByAtivo(paginacao, false);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId()));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar estoque nulo apenas para o salgado sem estoque")
        void deveRetornarSalgadoSemEstoque() {
            Salgado segundoSalgado = criarSalgado(2L, "Bolinha de queijo", true);

            when(salgadoRepository.findAllByAtivo(paginacao, true))
                    .thenReturn(new PageImpl<>(List.of(salgado, segundoSalgado), paginacao, 2));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId()))).thenReturn(List.of(estoque));

            Page<SalgadoResponse> resposta = salgadoService.listarSalgados(paginacao, true);

            assertThat(resposta.getContent()).containsExactly(
                    criarResponse(salgado, estoque), criarResponse(segundoSalgado, null));
            verify(salgadoRepository).findAllByAtivo(paginacao, true);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId()));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia sem consultar estoque")
        void deveRetornarPaginaVazia() {
            when(salgadoRepository.findAllByAtivo(paginacao, true)).thenReturn(Page.empty(paginacao));

            Page<SalgadoResponse> resposta = salgadoService.listarSalgados(paginacao, true);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            verify(salgadoRepository).findAllByAtivo(paginacao, true);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da consulta de salgados sem consultar estoque")
        void deveRetornarErroQuandoConsultaSalgadosFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findAllByAtivo(paginacao, true)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.listarSalgados(paginacao, true)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findAllByAtivo(paginacao, true);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da consulta dos estoques")
        void deveRetornarErroQuandoConsultaEstoqueFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findAllByAtivo(paginacao, true)).thenReturn(new PageImpl<>(List.of(salgado)));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.listarSalgados(paginacao, true)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findAllByAtivo(paginacao, true);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }
    }

    @Nested
    @DisplayName("Buscar salgados por nome")
    class BuscarPorNome {
        @ParameterizedTest
        @CsvSource({"true, 1, 1, 5", "false, 1, 1, 5", "true, 3, 0, 5", "false, 3, 0, 5"})
        @DisplayName("Deve encaminhar nome e status juntos e carregar somente o estoque da página")
        void deveBuscarPorNomeEStatus(boolean ativo, int numero, int quantidade, long total) {
            Pageable pagina = PageRequest.of(numero, 2, Sort.by("nome"));
            salgado.setAtivo(ativo);
            List<Salgado> conteudo = quantidade == 0 ? List.of() : List.of(salgado);

            when(salgadoRepository.findByNomeContainingIgnoreCaseAndAtivo(pagina, "Cox", ativo))
                    .thenReturn(new PageImpl<>(conteudo, pagina, total));
            if (quantidade > 0) {
                when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID))).thenReturn(List.of(estoque));
            }

            Page<SalgadoResponse> resposta = salgadoService.buscarPorNome(pagina, "Cox", ativo);

            assertThat(resposta.getContent()).containsExactlyElementsOf(
                    quantidade == 0 ? List.of() : List.of(criarResponse(salgado, estoque)));
            assertThat(resposta.getPageable()).isEqualTo(pagina);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(salgadoRepository).findByNomeContainingIgnoreCaseAndAtivo(pagina, "Cox", ativo);
            if (quantidade == 0) {
                verifyNoInteractions(estoqueRepository);
            } else {
                verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID));
            }
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @ParameterizedTest
        @CsvSource({"1, 2, 7, false", "3, 1, 7, true", "4, 0, 7, true"})
        @DisplayName("Deve preservar a paginação da busca e consultar somente os estoques dos salgados da página")
        void devePreservarPaginaDaBusca(int pagina, int quantidade, long total, boolean ultima) {
            Pageable requisitada = PageRequest.of(pagina, 2, Sort.by("nome").descending());
            Salgado segundo = criarSalgado(29L, "Coxinha de queijo", true);
            List<Salgado> conteudo = switch (quantidade) {
                case 2 -> List.of(segundo, salgado);
                case 1 -> List.of(segundo);
                default -> List.of();
            };

            when(salgadoRepository.findByNomeContainingIgnoreCaseAndAtivo(requisitada, "Coxinha", true))
                    .thenReturn(new PageImpl<>(conteudo, requisitada, total));
            if (quantidade == 2) {
                when(estoqueRepository.findAllBySalgadoIdIn(List.of(29L, ID))).thenReturn(List.of(estoque));
            } else if (quantidade == 1) {
                when(estoqueRepository.findAllBySalgadoIdIn(List.of(29L))).thenReturn(List.of());
            }

            Page<SalgadoResponse> resposta = salgadoService.buscarPorNome(requisitada, "Coxinha", true);

            List<SalgadoResponse> esperados = switch (quantidade) {
                case 2 -> List.of(criarResponse(segundo, null), criarResponse(salgado, estoque));
                case 1 -> List.of(criarResponse(segundo, null));
                default -> List.of();
            };

            assertThat(resposta.getContent()).containsExactlyElementsOf(esperados);
            assertThat(resposta.getPageable()).isEqualTo(requisitada);
            assertThat(resposta.getNumber()).isEqualTo(pagina);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            assertThat(resposta.isLast()).isEqualTo(ultima);
            verify(salgadoRepository).findByNomeContainingIgnoreCaseAndAtivo(requisitada, "Coxinha", true);
            if (quantidade == 0) {
                verifyNoInteractions(estoqueRepository);
            } else {
                verify(estoqueRepository).findAllBySalgadoIdIn(quantidade == 2 ? List.of(29L, ID) : List.of(29L));
            }
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve buscar pelo nome informado e retornar os salgados com seus estoques")
        void deveBuscarPorNome() {
            Salgado segundoSalgado = criarSalgado(2L, "Coxinha de calabresa", true);
            Estoque segundoEstoque = criarEstoque(20L, segundoSalgado, 80);

            when(salgadoRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true))
                    .thenReturn(new PageImpl<>(List.of(salgado, segundoSalgado), paginacao, 5));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId())))
                    .thenReturn(List.of(segundoEstoque, estoque));

            Page<SalgadoResponse> resposta = salgadoService.buscarPorNome(paginacao, "Cox", true);

            assertThat(resposta.getContent()).containsExactly(
                    criarResponse(salgado, estoque), criarResponse(segundoSalgado, segundoEstoque));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(salgadoRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId()));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar estoque nulo apenas para o salgado sem estoque")
        void deveRetornarSalgadoSemEstoque() {
            Salgado segundoSalgado = criarSalgado(2L, "Coxinha de calabresa", true);

            when(salgadoRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true))
                    .thenReturn(new PageImpl<>(List.of(salgado, segundoSalgado), paginacao, 2));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId()))).thenReturn(List.of(estoque));

            Page<SalgadoResponse> resposta = salgadoService.buscarPorNome(paginacao, "Cox", true);

            assertThat(resposta.getContent()).containsExactly(
                    criarResponse(salgado, estoque), criarResponse(segundoSalgado, null));
            verify(salgadoRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID, segundoSalgado.getId()));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia sem consultar estoque")
        void deveRetornarPaginaVazia() {
            when(salgadoRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true)).thenReturn(Page.empty(paginacao));

            Page<SalgadoResponse> resposta = salgadoService.buscarPorNome(paginacao, "Cox", true);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            verify(salgadoRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da consulta de salgados sem consultar estoque")
        void deveRetornarErroQuandoConsultaSalgadosFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.buscarPorNome(paginacao, "Cox", true)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da consulta dos estoques")
        void deveRetornarErroQuandoConsultaEstoqueFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true)).thenReturn(new PageImpl<>(List.of(salgado)));
            when(estoqueRepository.findAllBySalgadoIdIn(List.of(ID))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.buscarPorNome(paginacao, "Cox", true)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findByNomeContainingIgnoreCaseAndAtivo(paginacao, "Cox", true);
            verify(estoqueRepository).findAllBySalgadoIdIn(List.of(ID));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }
    }

    @Nested
    @DisplayName("Buscar salgado por ID")
    class BuscarPorId {
        @Test
        @DisplayName("Deve distinguir IDs de salgado e estoque e preservar todos os campos e o saldo zero")
        void deveRetornarCamposDoSalgadoEncontrado() {
            Salgado encontrado = new Salgado(37L, "Empada", "Frango", Categoria.ASSADO,
                    new BigDecimal("50.17"), new BigDecimal("71.99"), false);

            when(salgadoRepository.findById(37L)).thenReturn(Optional.of(encontrado));
            when(estoqueRepository.findBySalgadoId(37L)).thenReturn(Optional.of(new Estoque(503L, encontrado, 0)));

            SalgadoResponse resposta = salgadoService.buscarPorId(37L);

            assertThat(resposta).isEqualTo(new SalgadoResponse(37L, "Empada", "Frango", Categoria.ASSADO,
                    new BigDecimal("50.17"), new BigDecimal("71.99"), false, new EstoqueSalgadoDto(503L, 0)));
            verify(salgadoRepository).findById(37L);
            verify(estoqueRepository).findBySalgadoId(37L);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve buscar salgado ativo")
        void deveBuscarSalgadoAtivo() {
            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenReturn(Optional.of(estoque));

            SalgadoResponse resposta = salgadoService.buscarPorId(ID);

            assertThat(resposta.ativo()).isTrue();
            assertThat(resposta).isEqualTo(criarResponse(salgado, estoque));
            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verify(salgadoRepository, never()).save(any(Salgado.class));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve buscar salgado inativo sem ativá-lo")
        void deveBuscarSalgadoInativo() {
            salgado.setAtivo(false);

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenReturn(Optional.of(estoque));

            SalgadoResponse resposta = salgadoService.buscarPorId(ID);

            assertThat(resposta.ativo()).isFalse();
            assertThat(resposta).isEqualTo(criarResponse(salgado, estoque));
            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verify(salgadoRepository, never()).save(any(Salgado.class));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar os dados com estoque nulo quando não houver estoque")
        void deveRetornarSalgadoSemEstoque() {
            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenReturn(Optional.empty());

            SalgadoResponse resposta = salgadoService.buscarPorId(ID);

            assertThat(resposta.estoque()).isNull();
            assertThat(resposta).isEqualTo(criarResponse(salgado, null));
            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve informar quando o salgado não existir")
        void deveRejeitarSalgadoInexistente() {
            when(salgadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> salgadoService.buscarPorId(ID))
                    .isInstanceOf(NotFoundException.class).hasMessage("Salgado não encontrado com o ID: %d", ID);

            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da busca do salgado sem consultar estoque")
        void deveRetornarErroQuandoBuscarSalgadoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.buscarPorId(ID)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da busca do estoque")
        void deveRetornarErroQuandoBuscarEstoqueFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.buscarPorId(ID)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }
    }

    @Nested
    @DisplayName("Atualizar salgado")
    class Atualizar {
        @ParameterizedTest
        @CsvSource({"true, FRITO, 0", "false, ASSADO, 137", "true, ASSADO, 2147483647", "false, FRITO, 1"})
        @DisplayName("Deve atualizar todos os dados mantendo ID, situação e o estoque existente intactos")
        void deveAtualizarSemAlterarEstoque(boolean ativo, Categoria categoria, int saldo) {
            salgado.setAtivo(ativo);
            estoque.setQuantidade(saldo);
            SalgadoRequest novosDados = new SalgadoRequest("Salgado atualizado", "Novo recheio", categoria,
                    new BigDecimal("67.89"), new BigDecimal("93.21"));

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenReturn(Optional.of(estoque));

            SalgadoResponse resposta = salgadoService.atualizar(ID, novosDados);

            assertThat(resposta).isEqualTo(new SalgadoResponse(ID, "Salgado atualizado", "Novo recheio", categoria,
                    new BigDecimal("67.89"), new BigDecimal("93.21"), ativo, new EstoqueSalgadoDto(10L, saldo)));
            assertThat(criarResponse(salgado, estoque)).isEqualTo(resposta);
            assertThat(estoque.getId()).isEqualTo(10L);
            assertThat(estoque.getQuantidade()).isEqualTo(saldo);
            assertThat(estoque.getSalgado()).isSameAs(salgado);
            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve atualizar salgado ativo")
        void deveAtualizarSalgadoAtivo() {
            SalgadoRequest novosDados = new SalgadoRequest("Esfiha", "Esfiha de carne", Categoria.ASSADO,
                    new BigDecimal("65.00"), new BigDecimal("85.00"));

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenReturn(Optional.of(estoque));

            SalgadoResponse resposta = salgadoService.atualizar(ID, novosDados);

            assertThat(resposta.id()).isEqualTo(ID);
            assertThat(resposta.nome()).isEqualTo(novosDados.nome());
            assertThat(resposta.descricao()).isEqualTo(novosDados.descricao());
            assertThat(resposta.categoria()).isEqualTo(novosDados.categoria());
            assertThat(resposta.precoCentoCongelado()).isEqualTo(novosDados.precoCentoCongelado());
            assertThat(resposta.precoCentoProcessado()).isEqualTo(novosDados.precoCentoProcessado());
            assertThat(resposta.estoque()).isEqualTo(new EstoqueSalgadoDto(estoque.getId(), estoque.getQuantidade()));
            assertThat(resposta.ativo()).isTrue();
            assertThat(resposta).isEqualTo(criarResponse(salgado, estoque));
            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verify(salgadoRepository, never()).save(any(Salgado.class));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve atualizar salgado inativo sem ativá-lo")
        void deveAtualizarSalgadoInativo() {
            salgado.setAtivo(false);
            SalgadoRequest novosDados = new SalgadoRequest("Esfiha", "Esfiha de carne", Categoria.ASSADO,
                    new BigDecimal("65.00"), new BigDecimal("85.00"));

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenReturn(Optional.of(estoque));

            SalgadoResponse resposta = salgadoService.atualizar(ID, novosDados);

            assertThat(resposta.id()).isEqualTo(ID);
            assertThat(resposta.nome()).isEqualTo(novosDados.nome());
            assertThat(resposta.descricao()).isEqualTo(novosDados.descricao());
            assertThat(resposta.categoria()).isEqualTo(novosDados.categoria());
            assertThat(resposta.precoCentoCongelado()).isEqualTo(novosDados.precoCentoCongelado());
            assertThat(resposta.precoCentoProcessado()).isEqualTo(novosDados.precoCentoProcessado());
            assertThat(resposta.estoque()).isEqualTo(new EstoqueSalgadoDto(estoque.getId(), estoque.getQuantidade()));
            assertThat(resposta.ativo()).isFalse();
            assertThat(resposta).isEqualTo(criarResponse(salgado, estoque));
            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verify(salgadoRepository, never()).save(any(Salgado.class));
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar os dados com estoque nulo quando não houver estoque")
        void deveRetornarSalgadoSemEstoque() {
            SalgadoRequest novosDados = new SalgadoRequest("Esfiha", "Esfiha de carne", Categoria.ASSADO,
                    new BigDecimal("65.00"), new BigDecimal("85.00"));

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenReturn(Optional.empty());

            SalgadoResponse resposta = salgadoService.atualizar(ID, novosDados);

            assertThat(resposta.estoque()).isNull();
            assertThat(resposta).isEqualTo(criarResponse(salgado, null));
            assertThat(resposta.nome()).isEqualTo(novosDados.nome());
            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve informar quando o salgado não existir")
        void deveRejeitarSalgadoInexistente() {
            when(salgadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> salgadoService.atualizar(ID, dto))
                    .isInstanceOf(NotFoundException.class).hasMessage("Salgado não encontrado com o ID: %d", ID);

            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da busca do salgado sem consultar estoque")
        void deveRetornarErroQuandoBuscarSalgadoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.atualizar(ID, dto)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro da busca do estoque")
        void deveRetornarErroQuandoBuscarEstoqueFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));
            when(estoqueRepository.findBySalgadoId(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.atualizar(ID, dto)).isSameAs(erroDoBanco);

            verify(salgadoRepository).findById(ID);
            verify(estoqueRepository).findBySalgadoId(ID);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }
    }

    @Nested
    @DisplayName("Atualizar status do salgado")
    class AtualizarStatus {
        @Test
        @DisplayName("Deve preservar dados e estoque ao desativar, rejeitar repetição e ativar novamente")
        void deveAlternarStatusPreservandoDadosEEstoque() throws BadRequestException {
            SalgadoResponse anterior = criarResponse(salgado, estoque);

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));

            salgadoService.atualizarStatus(ID, new AlterarStatusDto(false));

            assertThat(salgado.isAtivo()).isFalse();
            SalgadoResponse aposDesativar = criarResponse(salgado, estoque);
            assertThat(aposDesativar).usingRecursiveComparison().ignoringFields("ativo").isEqualTo(anterior);
            assertThatThrownBy(() -> salgadoService.atualizarStatus(ID, new AlterarStatusDto(false)))
                    .isInstanceOf(BadRequestException.class).hasMessage("Salgado Coxinha já está inativo.");
            assertThat(criarResponse(salgado, estoque)).isEqualTo(aposDesativar);

            salgadoService.atualizarStatus(ID, new AlterarStatusDto(true));

            assertThat(criarResponse(salgado, estoque)).isEqualTo(anterior);
            assertThat(estoque.getSalgado()).isSameAs(salgado);
            verify(salgadoRepository, times(3)).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository);
        }

        @Test
        @DisplayName("Deve ativar salgado inativo preservando seus dados")
        void deveAtivarSalgado() throws BadRequestException {
            salgado.setAtivo(false);
            SalgadoResponse anterior = criarResponse(salgado, estoque);

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));

            salgadoService.atualizarStatus(ID, new AlterarStatusDto(true));

            assertThat(salgado.isAtivo()).isTrue();
            assertThat(salgado.getId()).isEqualTo(anterior.id());
            assertThat(salgado.getNome()).isEqualTo(anterior.nome());
            assertThat(salgado.getDescricao()).isEqualTo(anterior.descricao());
            assertThat(salgado.getCategoria()).isEqualTo(anterior.categoria());
            assertThat(salgado.getPrecoCentoCongelado()).isEqualTo(anterior.precoCentoCongelado());
            assertThat(salgado.getPrecoCentoProcessado()).isEqualTo(anterior.precoCentoProcessado());
            verify(salgadoRepository).findById(ID);
            verify(salgadoRepository, never()).save(any(Salgado.class));
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve avisar quando o salgado já está ativo")
        void deveRejeitarSalgadoJaAtivo() {
            SalgadoResponse anterior = criarResponse(salgado, estoque);

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));

            assertThatThrownBy(() -> salgadoService.atualizarStatus(ID, new AlterarStatusDto(true)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Salgado %s já está ativo.", salgado.getNome());

            assertThat(criarResponse(salgado, estoque)).isEqualTo(anterior);
            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve desativar salgado ativo preservando seus dados")
        void deveDesativarSalgado() throws BadRequestException {
            SalgadoResponse anterior = criarResponse(salgado, estoque);

            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));

            salgadoService.atualizarStatus(ID, new AlterarStatusDto(false));

            assertThat(salgado.isAtivo()).isFalse();
            assertThat(salgado.getId()).isEqualTo(anterior.id());
            assertThat(salgado.getNome()).isEqualTo(anterior.nome());
            assertThat(salgado.getDescricao()).isEqualTo(anterior.descricao());
            assertThat(salgado.getCategoria()).isEqualTo(anterior.categoria());
            assertThat(salgado.getPrecoCentoCongelado()).isEqualTo(anterior.precoCentoCongelado());
            assertThat(salgado.getPrecoCentoProcessado()).isEqualTo(anterior.precoCentoProcessado());
            verify(salgadoRepository).findById(ID);
            verify(salgadoRepository, never()).save(any(Salgado.class));
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve avisar quando o salgado já está inativo")
        void deveRejeitarSalgadoJaInativo() {
            salgado.setAtivo(false);

            SalgadoResponse anterior = criarResponse(salgado, estoque);
            when(salgadoRepository.findById(ID)).thenReturn(Optional.of(salgado));

            assertThatThrownBy(() -> salgadoService.atualizarStatus(ID, new AlterarStatusDto(false)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Salgado %s já está inativo.", salgado.getNome());

            assertThat(criarResponse(salgado, estoque)).isEqualTo(anterior);
            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve rejeitar alteração de status de salgado inexistente")
        void deveRejeitarSalgadoInexistente() {
            when(salgadoRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> salgadoService.atualizarStatus(ID, new AlterarStatusDto(false)))
                    .isInstanceOf(NotFoundException.class).hasMessage("Salgado não encontrado com o ID: %d", ID);

            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro ao buscar salgado para alterar status")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");
            when(salgadoRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> salgadoService.atualizarStatus(ID, new AlterarStatusDto(false)))
                    .isSameAs(erroDoBanco);

            verify(salgadoRepository).findById(ID);
            verifyNoInteractions(estoqueRepository);
            verifyNoMoreInteractions(salgadoRepository, estoqueRepository);
        }
    }

    private SalgadoRequest criarDto() {
        return new SalgadoRequest("Coxinha", "Coxinha de frango", Categoria.FRITO,
                new BigDecimal("60.00"), new BigDecimal("80.00"));
    }

    private Salgado criarSalgado(Long id, String nome, boolean ativo) {
        return new Salgado(id, nome, "Coxinha de frango", Categoria.FRITO,
                new BigDecimal("60.00"), new BigDecimal("80.00"), ativo);
    }

    private Estoque criarEstoque(Long id, Salgado salgadoDoEstoque, int quantidade) {
        return new Estoque(id, salgadoDoEstoque, quantidade);
    }

    private SalgadoResponse criarResponse(Salgado entidadeSalgado, Estoque estoqueDoSalgado) {
        EstoqueSalgadoDto estoqueDto = estoqueDoSalgado == null ? null
                : new EstoqueSalgadoDto(estoqueDoSalgado.getId(), estoqueDoSalgado.getQuantidade());
        return new SalgadoResponse(entidadeSalgado.getId(), entidadeSalgado.getNome(),
                entidadeSalgado.getDescricao(), entidadeSalgado.getCategoria(),
                entidadeSalgado.getPrecoCentoCongelado(), entidadeSalgado.getPrecoCentoProcessado(),
                entidadeSalgado.isAtivo(), estoqueDto);
    }

    @Nested
    @DisplayName("Limites de tamanho das colunas")
    class LimitesDeTamanho {
        @ParameterizedTest
        @CsvSource({"nome,100", "descricao,255"})
        @DisplayName("Deve aceitar o limite e rejeitar um caractere excedente na propriedade correta")
        void deveRespeitarLimite(String campo, int limite) {
            try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                for (int tamanho : new int[]{limite, limite + 1}) {
                    String valor = "a".repeat(tamanho);
                    var entrada = new SalgadoRequest(campo.equals("nome") ? valor : "Coxinha",
                        campo.equals("descricao") ? valor : "Frango", Categoria.FRITO, BigDecimal.ONE, BigDecimal.ONE);
                String propriedade = campo;
                    var violacoes = factory.getValidator().validate(entrada);
                    if (tamanho == limite) {
                        assertThat(violacoes).isEmpty();
                    } else {
                        assertThat(violacoes).singleElement().satisfies(violacao -> {
                            assertThat(violacao.getPropertyPath().toString()).isEqualTo(propriedade);
                            assertThat(violacao.getConstraintDescriptor().getAnnotation().annotationType())
                                    .isEqualTo(jakarta.validation.constraints.Size.class);
                        });
                    }
                }
            }
        }
    }

}
