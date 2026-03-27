package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.dto.ClienteDto;
import br.com.salgadosdalucia.api.model.Cliente;
import br.com.salgadosdalucia.api.repositoy.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;

    //public Optional<Cliente> findById(Long id) {}

    public Cliente cadastrar(ClienteDto dto) {
        Cliente cliente = Cliente.builder()
                .nome(dto.nome())
                .telefone(dto.telefone())
                .endereco(dto.endereco())
                .build();
        return repository.save(cliente);
    }

}
