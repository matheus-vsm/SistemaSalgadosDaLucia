package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.perfil.Perfil;
import br.com.salgadosdalucia.api.perfil.PerfilRepository;
import br.com.salgadosdalucia.api.perfil.PerfilUsuarioNome;
import br.com.salgadosdalucia.api.usuario.dto.AlterarSenhaUsuarioDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    private static final Long ID = 1L;
    private static final String SENHA_CRIPTOGRAFADA = "senha-criptografada";
    private static final String NOVA_SENHA_CRIPTOGRAFADA = "nova-senha-criptografada";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private Perfil perfil;
    private UsuarioRequest request;
    private Pageable paginacao;
    private AlterarSenhaUsuarioDto alteracaoSenha;

    @BeforeEach
    void setUp() {
        perfil = new Perfil();
        usuario = criarUsuario(ID, "Alice", true);
        request = new UsuarioRequest("Alice", "alice", "senha123", PerfilUsuarioNome.FUNCIONARIO);
        paginacao = PageRequest.of(0, 2);
        alteracaoSenha = new AlterarSenhaUsuarioDto("senha123", "novaSenha123", "novaSenha123");
    }

    @Nested
    @DisplayName("Buscar usuário pelo username")
    class LoadUserByUsername {
        @Test
        @DisplayName("Deve devolver o usuário inativo como desabilitado para a decisão do provider de autenticação")
        void devePreservarSituacaoDoUsuarioInativo() {
            usuario.setAtivo(false);
            when(usuarioRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));

            UserDetails resposta = usuarioService.loadUserByUsername("alice");

            assertThat(resposta).isSameAs(usuario);
            assertThat(resposta.isEnabled()).isFalse();
            verify(usuarioRepository).findByUsernameIgnoreCase("alice");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve retornar o usuário encontrado pelo username recebido")
        void deveBuscarUsuario() {
            when(usuarioRepository.findByUsernameIgnoreCase("ALICE")).thenReturn(Optional.of(usuario));

            UserDetails resposta = usuarioService.loadUserByUsername("ALICE");

            assertThat(resposta).isSameAs(usuario);
            verify(usuarioRepository).findByUsernameIgnoreCase("ALICE");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve avisar quando o username não existir")
        void deveRejeitarUsuarioInexistente() {
            when(usuarioRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.loadUserByUsername("alice"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("Usuário não encontrado com username: alice");

            verify(usuarioRepository).findByUsernameIgnoreCase("alice");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na busca por username")
        void deveRetornarErroNaBusca() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(usuarioRepository.findByUsernameIgnoreCase("alice")).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> usuarioService.loadUserByUsername("alice")).isSameAs(erroDoBanco);

            verify(usuarioRepository).findByUsernameIgnoreCase("alice");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Cadastrar usuário")
    class Cadastrar {
        @ParameterizedTest
        @CsvSource({"alice, true", "ALICE, true", "AlIcE, false"})
        @DisplayName("Deve rejeitar username encontrado antes de criptografar, consultar perfil ou salvar, inclusive se estiver inativo")
        void deveRejeitarUsernameExistente(String username, boolean ativo) {
            usuario.setAtivo(ativo);
            UsuarioResponse anterior = criarResponse(usuario);
            String senhaAnterior = usuario.getSenha();
            UsuarioRequest dados = new UsuarioRequest("Outra pessoa", username, "outraSenha123", PerfilUsuarioNome.FUNCIONARIO);
            // O repositório é simulado: o teste verifica a reação à duplicidade, não a comparação SQL de maiúsculas.
            when(usuarioRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.cadastrar(dados))
                    .isInstanceOf(BusinessException.class).hasMessage("Nome de Usuário já está em uso.");

            assertThat(criarResponse(usuario)).isEqualTo(anterior);
            assertThat(usuario.getSenha()).isEqualTo(senhaAnterior);
            verify(usuarioRepository).findByUsernameIgnoreCase(username);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(passwordEncoder, perfilRepository);
        }

        @ParameterizedTest
        @EnumSource(PerfilUsuarioNome.class)
        @DisplayName("Deve cadastrar com o perfil solicitado e persistir somente a senha criptografada")
        void deveCadastrarComPerfilSolicitado(PerfilUsuarioNome nomePerfil) {
            UsuarioRequest dados = new UsuarioRequest("Beatriz", "bia.ops", "senhaRecebida123", nomePerfil);
            Perfil selecionado = new Perfil();

            when(usuarioRepository.findByUsernameIgnoreCase("bia.ops")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("senhaRecebida123")).thenReturn("hash-da-senha-recebida");
            when(perfilRepository.findByNome(nomePerfil)).thenReturn(selecionado);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> {
                Usuario novo = invocacao.getArgument(0);
                assertThat(novo.getId()).isNull();
                novo.setId(47L);
                return novo;
            });

            Usuario resposta = usuarioService.cadastrar(dados);

            ArgumentCaptor<Usuario> enviado = ArgumentCaptor.forClass(Usuario.class);
            var ordem = inOrder(usuarioRepository, passwordEncoder, perfilRepository);
            ordem.verify(usuarioRepository).findByUsernameIgnoreCase("bia.ops");
            ordem.verify(passwordEncoder).encode("senhaRecebida123");
            ordem.verify(perfilRepository).findByNome(nomePerfil);
            ordem.verify(usuarioRepository).save(enviado.capture());
            assertThat(resposta).isSameAs(enviado.getValue());
            assertThat(resposta.getId()).isEqualTo(47L);
            assertThat(resposta.getNome()).isEqualTo("Beatriz");
            assertThat(resposta.getUsername()).isEqualTo("bia.ops");
            assertThat(resposta.getSenha()).isEqualTo("hash-da-senha-recebida").isNotEqualTo(dados.senha());
            assertThat(resposta.getPerfis()).containsExactly(selecionado);
            assertThat(resposta.isAtivo()).isTrue();
            verifyNoMoreInteractions(usuarioRepository, passwordEncoder, perfilRepository);
        }

        @Test
        @DisplayName("Deve interromper cadastro quando a consulta de username falhar, antes de criptografar ou salvar")
        void deveInterromperNaBuscaDeUsername() {
            var falha = new DataAccessResourceFailureException("Consulta de username indisponível");
            when(usuarioRepository.findByUsernameIgnoreCase(request.username())).thenThrow(falha);

            assertThatThrownBy(() -> usuarioService.cadastrar(request)).isSameAs(falha);

            verify(usuarioRepository).findByUsernameIgnoreCase(request.username());
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(passwordEncoder, perfilRepository);
        }

        @Test
        @DisplayName("Deve salvar usuário ativo com perfil e senha criptografada")
        void deveCadastrar() {
            when(passwordEncoder.encode(request.senha())).thenReturn(SENHA_CRIPTOGRAFADA);
            when(perfilRepository.findByNome(request.perfilUsuarioNome())).thenReturn(perfil);
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

            Usuario resposta = usuarioService.cadastrar(request);

            // cptura o usuário enviado ao repositório para conferir os dados do cadastro
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());

            Usuario salvo = captor.getValue();
            assertThat(salvo.getId()).isNull();
            assertThat(salvo.getNome()).isEqualTo(request.nome());
            assertThat(salvo.getUsername()).isEqualTo(request.username());
            assertThat(salvo.getSenha()).isEqualTo(SENHA_CRIPTOGRAFADA);
            assertThat(salvo.getPerfis()).containsExactly(perfil);
            assertThat(salvo.isAtivo()).isTrue();
            assertThat(resposta).isSameAs(usuario);
            verify(usuarioRepository).findByUsernameIgnoreCase(request.username());
            verify(passwordEncoder).encode(request.senha());
            verify(perfilRepository).findByNome(request.perfilUsuarioNome());
            verifyNoMoreInteractions(usuarioRepository, perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve interromper cadastro quando a criptografia falhar")
        void deveRetornarErroAoCriptografar() {
            IllegalStateException erro = new IllegalStateException("Falha ao criptografar");

            when(passwordEncoder.encode(request.senha())).thenThrow(erro);

            assertThatThrownBy(() -> usuarioService.cadastrar(request)).isSameAs(erro);

            verify(passwordEncoder).encode(request.senha());
            verifyNoMoreInteractions(passwordEncoder);
            verify(usuarioRepository).findByUsernameIgnoreCase(request.username());
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco no cadastro")
        void deveRetornarErroAoSalvar() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(passwordEncoder.encode(request.senha())).thenReturn(SENHA_CRIPTOGRAFADA);
            when(perfilRepository.findByNome(request.perfilUsuarioNome())).thenReturn(perfil);
            when(usuarioRepository.save(any(Usuario.class))).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> usuarioService.cadastrar(request)).isSameAs(erroDoBanco);

            verify(passwordEncoder).encode(request.senha());
            verify(perfilRepository).findByNome(request.perfilUsuarioNome());
            verify(usuarioRepository).save(any(Usuario.class));
            verify(usuarioRepository).findByUsernameIgnoreCase(request.username());
            verifyNoMoreInteractions(usuarioRepository, perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve interromper cadastro quando a busca do perfil falhar")
        void deveRetornarErroAoBuscarPerfil() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(passwordEncoder.encode(request.senha())).thenReturn(SENHA_CRIPTOGRAFADA);
            when(perfilRepository.findByNome(request.perfilUsuarioNome())).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> usuarioService.cadastrar(request)).isSameAs(erroDoBanco);

            verify(passwordEncoder).encode(request.senha());
            verify(perfilRepository).findByNome(request.perfilUsuarioNome());
            verify(usuarioRepository).findByUsernameIgnoreCase(request.username());
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoMoreInteractions(perfilRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Listar usuários")
    class Listar {
        @ParameterizedTest
        @CsvSource({"true, 1, 2, 7, false", "false, 3, 1, 7, true", "true, 4, 0, 7, true"})
        @DisplayName("Deve preservar perfis, ordem e metadados nas páginas posteriores, parciais ou vazias")
        void devePreservarPaginaRetornada(boolean ativo, int pagina, int quantidade, long total, boolean ultima) {
            Pageable requisitada = PageRequest.of(pagina, 2, Sort.by("nome").descending());
            usuario.setAtivo(ativo);
            Usuario segundo = Usuario.builder().id(87L).nome("Zoé").username("zoe.ops")
                    .senha("hash-de-outro-usuario").perfis(List.of(new Perfil())).ativo(ativo).build();
            List<Usuario> conteudo = switch (quantidade) {
                case 2 -> List.of(segundo, usuario);
                case 1 -> List.of(segundo);
                default -> List.of();
            };

            when(usuarioRepository.findAllByAtivo(requisitada, ativo)).thenReturn(new PageImpl<>(conteudo, requisitada, total));

            Page<UsuarioResponse> resposta = usuarioService.listar(requisitada, ativo);

            assertThat(resposta.getContent()).containsExactlyElementsOf(conteudo.stream().map(UsuarioServiceTest.this::criarResponse).toList());
            assertThat(resposta.getPageable()).isEqualTo(requisitada);
            assertThat(resposta.getNumber()).isEqualTo(pagina);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            assertThat(resposta.isLast()).isEqualTo(ultima);
            verify(usuarioRepository).findAllByAtivo(requisitada, ativo);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve preservar os dados e a paginação com filtro true")
        void deveListarUsuariosAtivos() {
            Usuario segundoUsuario = criarUsuario(2L, "Matheus", true);
            when(usuarioRepository.findAllByAtivo(paginacao, true))
                    .thenReturn(new PageImpl<>(List.of(usuario, segundoUsuario), paginacao, 5));

            Page<UsuarioResponse> resposta = usuarioService.listar(paginacao, true);

            assertThat(resposta.getContent()).containsExactly(criarResponse(usuario), criarResponse(segundoUsuario));
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            verify(usuarioRepository).findAllByAtivo(paginacao, true);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve retornar página vazia com filtro false")
        void deveListarUsuariosInativos() {
            when(usuarioRepository.findAllByAtivo(paginacao, false))
                    .thenReturn(Page.empty(paginacao));

            Page<UsuarioResponse> resposta = usuarioService.listar(paginacao, false);

            assertThat(resposta.getContent()).isEmpty();
            assertThat(resposta.getTotalElements()).isZero();
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            verify(usuarioRepository).findAllByAtivo(paginacao, false);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na consulta")
        void deveRetornarErroNaConsulta() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(usuarioRepository.findAllByAtivo(paginacao, true)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> usuarioService.listar(paginacao, true)).isSameAs(erroDoBanco);

            verify(usuarioRepository).findAllByAtivo(paginacao, true);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Buscar usuário por nome")
    class BuscarPorNome {
        @ParameterizedTest
        @CsvSource({"1, 2, 7, false", "3, 1, 7, true", "4, 0, 7, true"})
        @DisplayName("Deve remover espaços externos da busca e preservar o conteúdo e a paginação recebidos")
        void devePreservarPaginasDaBusca(int pagina, int quantidade, long total, boolean ultima) {
            Pageable requisitada = PageRequest.of(pagina, 2, Sort.by("nome").descending());
            Usuario primeiro = criarUsuario(91L, "Ana Maria", true);
            primeiro.setUsername("ana.m");
            Usuario segundo = criarUsuario(92L, "Ana Luisa", true);
            segundo.setUsername("luisa.a");
            List<Usuario> conteudo = switch (quantidade) {
                case 2 -> List.of(primeiro, segundo);
                case 1 -> List.of(segundo);
                default -> List.of();
            };

            when(usuarioRepository.findByNomeContainingIgnoreCase(requisitada, "Ana"))
                    .thenReturn(new PageImpl<>(conteudo, requisitada, total));

            Page<UsuarioResponse> resposta = usuarioService.buscarPorNome(requisitada, "  Ana  ");

            assertThat(resposta.getContent()).containsExactlyElementsOf(conteudo.stream().map(UsuarioServiceTest.this::criarResponse).toList());
            assertThat(resposta.getPageable()).isEqualTo(requisitada);
            assertThat(resposta.getNumber()).isEqualTo(pagina);
            assertThat(resposta.getSize()).isEqualTo(2);
            assertThat(resposta.getTotalElements()).isEqualTo(total);
            assertThat(resposta.getTotalPages()).isEqualTo(4);
            assertThat(resposta.isLast()).isEqualTo(ultima);
            verify(usuarioRepository).findByNomeContainingIgnoreCase(requisitada, "Ana");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve preservar os dados e a paginação removendo espaços do nome")
        void deveBuscarPorNomeRemovendoEspacos() {
            when(usuarioRepository.findByNomeContainingIgnoreCase(paginacao, "Alice"))
                    .thenReturn(new PageImpl<>(List.of(usuario), paginacao, 5));

            Page<UsuarioResponse> resposta = usuarioService.buscarPorNome(paginacao, "  Alice  ");

            assertThat(resposta.getContent()).containsExactly(criarResponse(usuario));
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getTotalPages()).isEqualTo(3);
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            verify(usuarioRepository).findByNomeContainingIgnoreCase(paginacao, "Alice");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve retornar página contendo todos os usuarios cadastrados quando o nome for em branco")
        void deveBuscarPorNomeEmBranco() {
            Usuario segundoUsuario = criarUsuario(2L, "Matheus", true);
            Usuario terceiroUsuario = criarUsuario(3L, "Luan", true);

            when(usuarioRepository.findByNomeContainingIgnoreCase(paginacao, ""))
                    .thenReturn(new PageImpl<>(List.of(usuario, segundoUsuario, terceiroUsuario), paginacao, 5));

            Page<UsuarioResponse> resposta = usuarioService.buscarPorNome(paginacao, "   ");

            assertThat(resposta.getContent()).containsExactlyInAnyOrder(
                    criarResponse(usuario),
                    criarResponse(segundoUsuario),
                    criarResponse(terceiroUsuario)
            );
            assertThat(resposta.getTotalElements()).isEqualTo(5);
            assertThat(resposta.getPageable()).isEqualTo(paginacao);
            verify(usuarioRepository).findByNomeContainingIgnoreCase(paginacao, "");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na consulta")
        void deveRetornarErroNaConsulta() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(usuarioRepository.findByNomeContainingIgnoreCase(paginacao, "Alice")).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> usuarioService.buscarPorNome(paginacao, "Alice")).isSameAs(erroDoBanco);

            verify(usuarioRepository).findByNomeContainingIgnoreCase(paginacao, "Alice");
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Buscar usuário por ID")
    class BuscarPorId {
        @Test
        @DisplayName("Deve preservar ID, nome, username distinto, perfis e situação do usuário encontrado")
        void deveRetornarCamposDoUsuarioEncontrado() {
            Perfil outroPerfil = new Perfil();
            Usuario encontrado = Usuario.builder().id(43L).nome("Beatriz").username("bia.ops")
                    .senha("hash-de-beatriz").perfis(List.of(outroPerfil, perfil)).ativo(false).build();

            when(usuarioRepository.findById(43L)).thenReturn(Optional.of(encontrado));

            UsuarioResponse resposta = usuarioService.buscarPorId(43L);

            assertThat(resposta).isEqualTo(new UsuarioResponse(43L, "Beatriz", "bia.ops", List.of(outroPerfil, perfil), false));
            verify(usuarioRepository).findById(43L);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve retornar os dados do usuário ativo")
        void deveBuscarPorIdAtivo() {
            when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

            UsuarioResponse resposta = usuarioService.buscarPorId(ID);

            assertThat(resposta).isEqualTo(criarResponse(usuario));
            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve retornar os dados do usuário já inativo")
        void deveBuscarPorIdInativo() {
            usuario.setAtivo(false);

            when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

            UsuarioResponse resposta = usuarioService.buscarPorId(ID);

            assertThat(resposta).isEqualTo(criarResponse(usuario));
            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve avisar quando o usuário não existir")
        void deveRejeitarUsuarioInexistente() {
            when(usuarioRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.buscarPorId(ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Usuario não encontrado com o ID: %d", ID);

            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na busca")
        void deveRetornarErroNaBusca() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(usuarioRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> usuarioService.buscarPorId(ID)).isSameAs(erroDoBanco);

            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Desativar usuário")
    class Desativar {
        @Test
        @DisplayName("Deve preservar dados, senha e perfis ao desativar e repetir a desativação")
        void devePreservarDadosEmDesativacoesSucessivas() {
            UsuarioResponse anterior = criarResponse(usuario);
            String senhaAnterior = usuario.getSenha();

            when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

            usuarioService.desativar(ID);

            assertThat(usuario.isAtivo()).isFalse();
            assertThat(criarResponse(usuario)).usingRecursiveComparison().ignoringFields("ativo").isEqualTo(anterior);

            usuarioService.desativar(ID);

            assertThat(usuario.isAtivo()).isFalse();
            assertThat(criarResponse(usuario)).usingRecursiveComparison().ignoringFields("ativo").isEqualTo(anterior);
            assertThat(usuario.getSenha()).isEqualTo(senhaAnterior);
            verify(usuarioRepository, times(2)).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve desativar um usuário ativo")
        void deveDesativarAtivo() {
            when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

            usuarioService.desativar(ID);

            assertThat(usuario.isAtivo()).isFalse();
            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve manter o usuário inativo ao desativar usuário já inativo")
        void deveDesativarInativo() {
            usuario.setAtivo(false);

            when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

            usuarioService.desativar(ID);

            assertThat(usuario.isAtivo()).isFalse();
            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve avisar quando o usuário não existir")
        void deveRejeitarUsuarioInexistente() {
            when(usuarioRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.desativar(ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Usuário não encontrado com id: 1");

            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Deve devolver falha do banco na busca")
        void deveRetornarErroNaBusca() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(usuarioRepository.findById(ID)).thenThrow(erroDoBanco);

            assertThatThrownBy(() -> usuarioService.desativar(ID)).isSameAs(erroDoBanco);

            verify(usuarioRepository).findById(ID);
            verifyNoMoreInteractions(usuarioRepository);
            verifyNoInteractions(perfilRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Alterar senha do usuário")
    class AlterarSenha {
        @Test
        @DisplayName("Deve verificar a senha e gravar o hash novo no ID do usuário recebido, preservando seus demais dados")
        void deveAlterarSenhaDoUsuarioCorreto() {
            Usuario alvo = Usuario.builder().id(47L).nome("Beatriz").username("bia.ops")
                    .senha("hash-atual-47").perfis(List.of(perfil)).ativo(true).build();
            UsuarioResponse anterior = criarResponse(alvo);
            AlterarSenhaUsuarioDto dados = new AlterarSenhaUsuarioDto("Atual!47", "Nova!47", "Nova!47");

            when(passwordEncoder.matches("Atual!47", "hash-atual-47")).thenReturn(true);
            when(passwordEncoder.encode("Nova!47")).thenReturn("hash-novo-47");

            usuarioService.alterarSenha(dados, alvo);

            var ordem = inOrder(passwordEncoder, usuarioRepository);
            ordem.verify(passwordEncoder).matches("Atual!47", "hash-atual-47");
            ordem.verify(passwordEncoder).encode("Nova!47");
            ordem.verify(usuarioRepository).alterarSenha(47L, "hash-novo-47");
            assertThat(criarResponse(alvo)).isEqualTo(anterior);
            verifyNoMoreInteractions(passwordEncoder, usuarioRepository);
            verifyNoInteractions(perfilRepository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"NovaSenha123", "novaSenha123 ", " novaSenha123"})
        @DisplayName("Deve exigir confirmação exatamente igual à nova senha antes de criptografar ou gravar")
        void deveExigirConfirmacaoExata(String confirmacao) {
            AlterarSenhaUsuarioDto dados = new AlterarSenhaUsuarioDto("senha123", "novaSenha123", confirmacao);

            when(passwordEncoder.matches("senha123", SENHA_CRIPTOGRAFADA)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.alterarSenha(dados, usuario))
                    .isInstanceOf(BusinessException.class).hasMessage("Nova senha e confirmação não coincidem!");

            verify(passwordEncoder).matches("senha123", SENHA_CRIPTOGRAFADA);
            verifyNoMoreInteractions(passwordEncoder);
            verifyNoInteractions(usuarioRepository, perfilRepository);
        }

        @Test
        @DisplayName("Deve validar senha atual e salvar a nova senha criptografada")
        void deveAlterarSenha() {
            when(passwordEncoder.matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA)).thenReturn(true);
            when(passwordEncoder.encode(alteracaoSenha.novaSenha())).thenReturn(NOVA_SENHA_CRIPTOGRAFADA);

            usuarioService.alterarSenha(alteracaoSenha, usuario);

            verify(passwordEncoder).matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA);
            verify(passwordEncoder).encode(alteracaoSenha.novaSenha());
            verify(usuarioRepository).alterarSenha(ID, NOVA_SENHA_CRIPTOGRAFADA);
            verifyNoMoreInteractions(usuarioRepository, passwordEncoder);
            verifyNoInteractions(perfilRepository);
        }

        @Test
        @DisplayName("Deve rejeitar alteração quando senha atual estiver incorreta")
        void deveRejeitarSenhaAtualIncorreta() {
            when(passwordEncoder.matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA)).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.alterarSenha(alteracaoSenha, usuario))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Senha atual incorreta!");

            verify(passwordEncoder).matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA);
            verifyNoMoreInteractions(passwordEncoder);
            verifyNoInteractions(usuarioRepository, perfilRepository);
        }

        @Test
        @DisplayName("Deve rejeitar alteração quando confirmação for diferente")
        void deveRejeitarConfirmacaoDiferente() {
            when(passwordEncoder.matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.alterarSenha(new AlterarSenhaUsuarioDto("senha123", "novaSenha123", "outraSenha"), usuario))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nova senha e confirmação não coincidem!");

            verify(passwordEncoder).matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA);
            verifyNoMoreInteractions(passwordEncoder);
            verifyNoInteractions(usuarioRepository, perfilRepository);
        }

        @Test
        @DisplayName("Deve rejeitar alteração quando senha atual e confirmação estiverem incorretas")
        void deveRejeitarSenhaAtualAntesDaConfirmacao() {
            when(passwordEncoder.matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA)).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.alterarSenha(new AlterarSenhaUsuarioDto("senha123", "novaSenha123", "outraSenha"), usuario))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Senha atual incorreta!");

            verify(passwordEncoder).matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA);
            verifyNoMoreInteractions(passwordEncoder);
            verifyNoInteractions(usuarioRepository, perfilRepository);
        }

        @Test
        @DisplayName("Deve devolver falha do banco ao atualizar a senha")
        void deveRetornarErroAoAtualizarSenha() {
            DataAccessResourceFailureException erroDoBanco =
                    new DataAccessResourceFailureException("Banco indisponível");

            when(passwordEncoder.matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA)).thenReturn(true);
            when(passwordEncoder.encode(alteracaoSenha.novaSenha())).thenReturn(NOVA_SENHA_CRIPTOGRAFADA);
            doThrow(erroDoBanco).when(usuarioRepository).alterarSenha(ID, NOVA_SENHA_CRIPTOGRAFADA);

            assertThatThrownBy(() -> usuarioService.alterarSenha(alteracaoSenha, usuario)).isSameAs(erroDoBanco);

            verify(passwordEncoder).matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA);
            verify(passwordEncoder).encode(alteracaoSenha.novaSenha());
            verify(usuarioRepository).alterarSenha(ID, NOVA_SENHA_CRIPTOGRAFADA);
            verifyNoMoreInteractions(usuarioRepository, passwordEncoder);
            verifyNoInteractions(perfilRepository);
        }

        @Test
        @DisplayName("Deve interromper alteração quando a verificação falhar")
        void deveRetornarErroNoMatches() {
            IllegalStateException erro = new IllegalStateException("Falha no encoder");

            when(passwordEncoder.matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA)).thenThrow(erro);

            assertThatThrownBy(() -> usuarioService.alterarSenha(alteracaoSenha, usuario)).isSameAs(erro);

            verify(passwordEncoder).matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA);
            verifyNoMoreInteractions(passwordEncoder);
            verifyNoInteractions(usuarioRepository, perfilRepository);
        }

        @Test
        @DisplayName("Deve interromper alteração quando a criptografia falhar")
        void deveRetornarErroNoEncode() {
            when(passwordEncoder.matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA)).thenReturn(true);

            IllegalStateException erro = new IllegalStateException("Falha no encoder");
            when(passwordEncoder.encode(alteracaoSenha.novaSenha())).thenThrow(erro);

            assertThatThrownBy(() -> usuarioService.alterarSenha(alteracaoSenha, usuario)).isSameAs(erro);

            verify(passwordEncoder).matches(alteracaoSenha.senhaAtual(), SENHA_CRIPTOGRAFADA);
            verify(passwordEncoder).encode(alteracaoSenha.novaSenha());
            verifyNoMoreInteractions(passwordEncoder);
            verifyNoInteractions(usuarioRepository, perfilRepository);
        }
    }

    private Usuario criarUsuario(Long id, String nome, boolean ativo) {
        return Usuario.builder().id(id).nome(nome).username(nome.toLowerCase(java.util.Locale.ROOT))
                .senha(SENHA_CRIPTOGRAFADA).perfis(List.of(perfil)).ativo(ativo).build();
    }

    private UsuarioResponse criarResponse(Usuario entidade) {
        return new UsuarioResponse(entidade.getId(), entidade.getNome(), entidade.getUsername(),
                entidade.getPerfis(), entidade.isAtivo());
    }

    @Nested
    @DisplayName("Limites de tamanho das colunas")
    class LimitesDeTamanho {
        @ParameterizedTest
        @CsvSource({"nome,100", "username,50"})
        @DisplayName("Deve aceitar o limite e rejeitar um caractere excedente na propriedade correta")
        void deveRespeitarLimite(String campo, int limite) {
            try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                for (int tamanho : new int[]{limite, limite + 1}) {
                    String valor = "a".repeat(tamanho);
                    var entrada = new UsuarioRequest(campo.equals("nome") ? valor : "Ana",
                        campo.equals("username") ? valor : "ana", "senha", PerfilUsuarioNome.FUNCIONARIO);
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

    @Nested
    @DisplayName("Perfil configurado para cadastro")
    class PerfilConfigurado {
        @ParameterizedTest
        @EnumSource(PerfilUsuarioNome.class)
        @DisplayName("Deve informar perfil ausente sem salvar um usuário sem permissões")
        void deveRejeitarPerfilAusente(PerfilUsuarioNome nomePerfil) {
            var entrada = new UsuarioRequest("Ana", "ana", "senha", nomePerfil);
            assertThatThrownBy(() -> usuarioService.cadastrar(entrada))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Perfil de usuário não configurado: " + nomePerfil);
            verify(perfilRepository).findByNome(nomePerfil);
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }
    }

    @Nested
    @DisplayName("Limite de bytes da senha no BCrypt")
    class LimiteDaSenha {
        @ParameterizedTest
        @MethodSource("br.com.salgadosdalucia.api.usuario.UsuarioServiceTest#senhasNoLimite")
        @DisplayName("Deve cadastrar com até 72 bytes preservando espaços e caracteres originais")
        void deveAceitarCadastro(String senha) {
            var entrada = new UsuarioRequest("Ana", "ana", senha, PerfilUsuarioNome.FUNCIONARIO);
            when(perfilRepository.findByNome(entrada.perfilUsuarioNome())).thenReturn(perfil);
            when(passwordEncoder.encode(senha)).thenReturn("hash");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
            var salvo = usuarioService.cadastrar(entrada);
            assertThat(salvo.getSenha()).isEqualTo("hash");
            verify(passwordEncoder).encode(senha);
        }

        @ParameterizedTest
        @MethodSource("br.com.salgadosdalucia.api.usuario.UsuarioServiceTest#senhasExcedentes")
        @DisplayName("Deve rejeitar senha acima de 72 bytes no cadastro antes de codificar ou salvar")
        void deveRejeitarCadastro(String senha) {
            var entrada = new UsuarioRequest("Ana", "ana", senha, PerfilUsuarioNome.FUNCIONARIO);
            assertThatThrownBy(() -> usuarioService.cadastrar(entrada))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("A senha deve ter no máximo 72 bytes em UTF-8. Caracteres acentuados e emojis podem ocupar mais de um byte.");
            verifyNoInteractions(passwordEncoder, perfilRepository);
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @ParameterizedTest
        @MethodSource("br.com.salgadosdalucia.api.usuario.UsuarioServiceTest#senhasNoLimite")
        @DisplayName("Deve alterar para senha de até 72 bytes sem modificar a senha digitada")
        void deveAceitarAlteracao(String senha) {
            var entrada = new AlterarSenhaUsuarioDto("atual", senha, senha);
            when(passwordEncoder.matches("atual", usuario.getSenha())).thenReturn(true);
            when(passwordEncoder.encode(senha)).thenReturn("hash-novo");
            usuarioService.alterarSenha(entrada, usuario);
            verify(passwordEncoder).encode(senha);
            verify(usuarioRepository).alterarSenha(ID, "hash-novo");
        }

        @ParameterizedTest
        @MethodSource("br.com.salgadosdalucia.api.usuario.UsuarioServiceTest#senhasExcedentes")
        @DisplayName("Deve rejeitar nova senha acima de 72 bytes sem codificar ou atualizar o hash")
        void deveRejeitarAlteracao(String senha) {
            var entrada = new AlterarSenhaUsuarioDto("atual", senha, senha);
            when(passwordEncoder.matches("atual", usuario.getSenha())).thenReturn(true);
            assertThatThrownBy(() -> usuarioService.alterarSenha(entrada, usuario))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("A senha deve ter no máximo 72 bytes em UTF-8. Caracteres acentuados e emojis podem ocupar mais de um byte.");
            verify(passwordEncoder).matches("atual", usuario.getSenha());
            verifyNoMoreInteractions(passwordEncoder);
            verifyNoInteractions(usuarioRepository, perfilRepository);
        }
    }

    static Stream<String> senhasNoLimite() {
        return Stream.of("a".repeat(72), "á".repeat(36), "😀".repeat(18), " " + "á".repeat(35) + " ", "curta");
    }

    static Stream<String> senhasExcedentes() {
        return Stream.of("a".repeat(73), "á".repeat(37), "😀".repeat(19), "a".repeat(71) + "á");
    }

}
