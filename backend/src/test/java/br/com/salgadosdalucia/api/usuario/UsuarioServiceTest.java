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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
            verifyNoInteractions(usuarioRepository, perfilRepository);
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
            verifyNoInteractions(usuarioRepository);
            verifyNoMoreInteractions(perfilRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Listar usuários")
    class Listar {
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

}
