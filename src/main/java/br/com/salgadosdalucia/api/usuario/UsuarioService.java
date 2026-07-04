package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import lombok.RequiredArgsConstructor;
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
    
}
