package br.com.salgadosdalucia.api.estoque;

import br.com.salgadosdalucia.api.estoque.dto.AtualizarQuantidadeRequest;
import br.com.salgadosdalucia.api.estoque.dto.EstoqueListagemDto;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.salgado.Salgado;
import br.com.salgadosdalucia.api.salgado.enums.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstoqueService")
class EstoqueServiceTest {

    private static final Long SALGADO_ID = 1L;

    @Mock
    private EstoqueRepository estoqueRepository;
    @InjectMocks
    private EstoqueService estoqueService;

    private Salgado salgado;
    private Estoque estoque;
    private Pageable paginacao;

    @BeforeEach
    void setUp() {
        salgado = criarSalgado(SALGADO_ID, "Coxinha", true);
        estoque = criarEstoque(10L, salgado, 150);
        paginacao = PageRequest.of(0, 2);
    }

    @Nested
    @DisplayName("Atualizar quantidade do estoque")
    class AtualizarQuantidade {
        @Test
        @DisplayName("Deve somar a quantidade recebida ao estoque atual")
        void deveAdicionarQuantidade() {
            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            estoqueService.atualizarQuantidade(SALGADO_ID, new AtualizarQuantidadeRequest(50));

            assertThat(estoque.getQuantidade()).isEqualTo(200);
            assertThat(estoque.getId()).isEqualTo(10L);
            assertThat(estoque.getSalgado()).isSameAs(salgado);
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verify(estoqueRepository, never()).save(any(Estoque.class));
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve descontar a quantidade informada quando houver saldo suficiente")
        void deveRetirarQuantidade() {
            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            estoqueService.atualizarQuantidade(SALGADO_ID, new AtualizarQuantidadeRequest(-50));

            assertThat(estoque.getQuantidade()).isEqualTo(100);
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verify(estoqueRepository, never()).save(any(Estoque.class));
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve permitir retirar toda a quantidade e deixar o estoque zerado")
        void deveZerarEstoque() {
            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            estoqueService.atualizarQuantidade(SALGADO_ID, new AtualizarQuantidadeRequest(-150));

            assertThat(estoque.getQuantidade()).isZero();
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve manter a quantidade atual quando a alteração for zero")
        void deveManterQuantidadeQuandoAlteracaoForZero() {
            EstoqueListagemDto anterior = criarResponse(estoque);
            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            estoqueService.atualizarQuantidade(SALGADO_ID, new AtualizarQuantidadeRequest(0));

            assertThat(criarResponse(estoque)).isEqualTo(anterior);
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve permitir adicionar quantidade a um estoque zerado")
        void deveAdicionarQuantidadeAoEstoqueZerado() {
            estoque.setQuantidade(0);

            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            estoqueService.atualizarQuantidade(SALGADO_ID, new AtualizarQuantidadeRequest(50));

            assertThat(estoque.getQuantidade()).isEqualTo(50);
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve rejeitar retirada maior que o saldo sem alterar o estoque")
        void deveRejeitarQuantidadeNegativa() {
            EstoqueListagemDto anterior = criarResponse(estoque);

            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            assertThatThrownBy(() -> estoqueService.atualizarQuantidade(
                    SALGADO_ID, new AtualizarQuantidadeRequest(-151)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("A quantidade no estoque não pode ser negativa.");

            assertThat(criarResponse(estoque)).isEqualTo(anterior);
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verify(estoqueRepository, never()).save(any(Estoque.class));
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve atualizar estoque de salgado inativo sem ativá-lo")
        void deveAtualizarEstoqueDeSalgadoInativo() {
            salgado.setAtivo(false);

            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            estoqueService.atualizarQuantidade(SALGADO_ID, new AtualizarQuantidadeRequest(50));

            assertThat(estoque.getQuantidade()).isEqualTo(200);
            assertThat(salgado.isAtivo()).isFalse();
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve rejeitar atualização quando o estoque não existir")
        void deveRejeitarEstoqueInexistente() {
            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> estoqueService.atualizarQuantidade(
                    SALGADO_ID, new AtualizarQuantidadeRequest(50)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Estoque não encontrado para o salgado com id: %d", SALGADO_ID);

            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco na atualização sem alterar a quantidade")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> estoqueService.atualizarQuantidade(
                    SALGADO_ID, new AtualizarQuantidadeRequest(50))).isSameAs(erroDoBanco);

            assertThat(estoque.getQuantidade()).isEqualTo(150);
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }
    }

    @Nested
    @DisplayName("Listar estoques de salgados ativos")
    class Listar {
        @Test
        @DisplayName("Deve retornar os estoques encontrados e preservar os dados da página")
        void deveListar() {
            Salgado segundoSalgado = criarSalgado(2L, "Esfiha", true);
            Estoque segundoEstoque = criarEstoque(20L, segundoSalgado, 0);

            when(estoqueRepository.findAllSalgadosAtivos(paginacao))
                    .thenReturn(new PageImpl<>(List.of(estoque, segundoEstoque), paginacao, 5));

            Page<EstoqueListagemDto> resposta = estoqueService.listar(paginacao);

            assertThat(resposta.getContent()).containsExactly(criarResponse(estoque), criarResponse(segundoEstoque));
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            verify(estoqueRepository).findAllSalgadosAtivos(paginacao);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver estoques de salgados ativos")
        void deveRetornarPaginaVazia() {
            when(estoqueRepository.findAllSalgadosAtivos(paginacao)).thenReturn(Page.empty(paginacao));

            Page<EstoqueListagemDto> resposta = estoqueService.listar(paginacao);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            assertThat(resposta.getTotalElements()).isZero();
            verify(estoqueRepository).findAllSalgadosAtivos(paginacao);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco na listagem")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(estoqueRepository.findAllSalgadosAtivos(paginacao)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> estoqueService.listar(paginacao)).isSameAs(erroDoBanco);

            verify(estoqueRepository).findAllSalgadosAtivos(paginacao);
            verifyNoMoreInteractions(estoqueRepository);
        }
    }

    @Nested
    @DisplayName("Buscar estoque por ID do salgado")
    class BuscarPorSalgadoId {
        @Test
        @DisplayName("Deve retornar o estoque de um salgado ativo")
        void deveBuscarEstoqueDeSalgadoAtivo() {
            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            EstoqueListagemDto resposta = estoqueService.buscarPorSalgadoId(SALGADO_ID);

            assertThat(resposta.id()).isEqualTo(estoque.getId());
            assertThat(resposta.salgadoId()).isEqualTo(estoque.getSalgado().getId());
            assertThat(resposta.nomeSalgado()).isEqualTo(estoque.getSalgado().getNome());
            assertThat(resposta.quantidade()).isEqualTo(estoque.getQuantidade());
            assertThat(resposta).isEqualTo(criarResponse(estoque));
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve retornar o estoque mesmo quando a quantidade estiver zerada")
        void deveBuscarEstoqueZerado() {
            estoque.setQuantidade(0);

            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            EstoqueListagemDto resposta = estoqueService.buscarPorSalgadoId(SALGADO_ID);

            assertThat(resposta.quantidade()).isZero();
            assertThat(resposta).isEqualTo(criarResponse(estoque));
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve informar quando o estoque não existir para o salgado")
        void deveRejeitarEstoqueInexistente() {
            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> estoqueService.buscarPorSalgadoId(SALGADO_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Estoque não encontrado para o salgado com id: %d", SALGADO_ID);

            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve rejeitar a busca do estoque de um salgado inativo")
        void deveRejeitarSalgadoInativo() {
            salgado.setAtivo(false);
            EstoqueListagemDto anterior = criarResponse(estoque);

            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenReturn(Optional.of(estoque));

            assertThatThrownBy(() -> estoqueService.buscarPorSalgadoId(SALGADO_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Salgado com id: %d está inativo, estoque não disponível.", SALGADO_ID);

            assertThat(criarResponse(estoque)).isEqualTo(anterior);
            assertThat(salgado.isAtivo()).isFalse();
            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve devolver o erro do banco na busca por salgado")
        void deveRetornarErroQuandoBancoFalhar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(estoqueRepository.findBySalgadoId(SALGADO_ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> estoqueService.buscarPorSalgadoId(SALGADO_ID)).isSameAs(erroDoBanco);

            verify(estoqueRepository).findBySalgadoId(SALGADO_ID);
            verifyNoMoreInteractions(estoqueRepository);
        }
    }

    private Salgado criarSalgado(Long id, String nome, boolean ativo) {
        return new Salgado(id, nome, "Salgado de frango", Categoria.FRITO,
                new BigDecimal("60.00"), new BigDecimal("80.00"), ativo);
    }

    private Estoque criarEstoque(Long id, Salgado entidadeSalgado, Integer quantidade) {
        return new Estoque(id, entidadeSalgado, quantidade);
    }

    private EstoqueListagemDto criarResponse(Estoque entidadeEstoque) {
        return new EstoqueListagemDto(entidadeEstoque.getId(), entidadeEstoque.getSalgado().getId(),
                entidadeEstoque.getSalgado().getNome(), entidadeEstoque.getQuantidade());
    }

}
