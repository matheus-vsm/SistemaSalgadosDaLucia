package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import br.com.salgadosdalucia.api.usuario.dto.AlterarSenhaUsuarioDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;
import br.com.salgadosdalucia.api.perfil.PerfilRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    private final PerfilRepository perfilRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com username: " + username));
    }

    @Transactional
    public Usuario cadastrar(UsuarioRequest dados) {
        String senhaCriptografada = passwordEncoder.encode(dados.senha());
        var perfil = perfilRepository.findByNome(dados.perfilUsuarioNome());
        Usuario usuario = UsuarioMapper.mapToEntity(dados, senhaCriptografada, perfil);
        return usuarioRepository.save(usuario);
    }

    public Page<UsuarioResponse> listar(Pageable paginacao) {
        return usuarioRepository.findAllByAtivoTrue(paginacao).map(UsuarioMapper::mapToUsuarioResponse);
    }

    public UsuarioResponse buscarPorId(Long id) throws NotFoundException {
        Usuario usuario = ValidacaoEntidadeHelper.buscarEntidadePorId(usuarioRepository, id, "Usuario");
        return UsuarioMapper.mapToUsuarioResponse(usuario);
    }

    @Transactional
    public void alterarSenha(AlterarSenhaUsuarioDto dados, Usuario usuarioLogado) {
        if (!passwordEncoder.matches(dados.senhaAtual(), usuarioLogado.getSenha())) {
            throw new BusinessException("Senha atual incorreta!");
        }
        if (!dados.novaSenha().equals(dados.novaSenhaConfirmacao())) {
            throw new BusinessException("Nova senha e confirmação não coincidem!");
        }
        String novaSenhaCriptografada = passwordEncoder.encode(dados.novaSenha());
        usuarioRepository.alterarSenha(usuarioLogado.getId(), novaSenhaCriptografada);
    }

    @Transactional
    public void desativar(Long id) {
        usuarioRepository.findById(id).ifPresentOrElse(
                usuario -> usuario.setAtivo(false),
                () -> { throw new EntityNotFoundException("Usuário não encontrado com id: " + id); }
        );
    }

}
