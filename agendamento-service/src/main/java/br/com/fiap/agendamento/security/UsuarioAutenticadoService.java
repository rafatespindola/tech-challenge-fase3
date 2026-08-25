package br.com.fiap.agendamento.security;

import br.com.fiap.agendamento.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultado apenas no login. Depois disso o principal vem do proprio token,
 * sem ida ao banco - e o que torna as requisicoes stateless.
 */
@Service
public class UsuarioAutenticadoService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado loadUserByUsername(String login) {
        return usuarioRepository.findByLogin(login)
                .map(UsuarioAutenticado::de)
                // Mensagem generica de proposito: dizer "login nao existe" entrega
                // a um atacante quais logins sao validos.
                .orElseThrow(() -> new UsernameNotFoundException("credenciais invalidas"));
    }
}
