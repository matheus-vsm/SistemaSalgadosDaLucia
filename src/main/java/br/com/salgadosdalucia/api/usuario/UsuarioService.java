package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.usuario.dto.AlterarSenhaUsuarioDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;
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

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    @Transactional
    public Usuario cadastrar(UsuarioRequest dados) {
        String senhaCriptografada = passwordEncoder.encode(dados.senha());
        Usuario usuario = UsuarioMapper.mapToEntity(dados, senhaCriptografada);
        return usuarioRepository.save(usuario);
    }

    public Page<UsuarioResponse> listar(Pageable paginacao) {
        return usuarioRepository.findAll(paginacao).map(UsuarioMapper::mapToUsuarioResponse);
    }

    public UsuarioResponse buscarPorId(Long id) throws NotFoundException {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
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

    public void desativar() {
        return;
    }
}
