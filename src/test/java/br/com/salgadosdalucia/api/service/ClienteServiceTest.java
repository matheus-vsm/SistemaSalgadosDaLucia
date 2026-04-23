package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.cliente.dto.ClienteDto;
import br.com.salgadosdalucia.api.cliente.ClienteService;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoMapper;
import br.com.salgadosdalucia.api.cliente.Cliente;
import br.com.salgadosdalucia.api.cliente.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    // agora está retornando ClienteResponse
//    @Test
//    @DisplayName("SUCESSO - Deve cadastrar um novo cliente")
//    void deveCadastrarUmNovoUsuarioComSucesso() {
//        /// ARRANGE or GIVEN
//        ClienteDto dto = criarClienteValido();
//
//        Mockito.when(clienteRepository.save(Mockito.any(Cliente.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // ACT or WHEN
//        Cliente clienteCadastrado = clienteService.cadastrar(dto);
//
//        // ASSERT or THEN
//        assertNotNull(clienteCadastrado);
//        assertEquals(dto.nome(), clienteCadastrado.getNome());
//        assertEquals(dto.telefone(), clienteCadastrado.getTelefone());
//        assertEquals(EnderecoMapper.mapToEntity(dto.endereco()), clienteCadastrado.getEndereco());
//        assertTrue(clienteCadastrado.isAtivo());
//
//        Mockito.verify(clienteRepository).save(Mockito.any(Cliente.class)); // garante que o service chamou o save()
//    }
//
//    @Test
//    @DisplayName("SUCESSO - Deve cadastrar um novo cliente sem campos não obrigatórios")
//    void deveCadastrarUmNovoUsuarioComSucessoSemCamposNaoObrigatorios() {
//        /// ARRANGE or GIVEN
//        ClienteDto dto = criarClienteValido();
//        dto = new ClienteDto(dto.nome(), dto.telefone(), new EnderecoDto(
//                dto.endereco().logradouro(),
//                dto.endereco().numero(),
//                null,
//                dto.endereco().cep(),
//                dto.endereco().bairro(),
//                dto.endereco().cidade(),
//                dto.endereco().uf()
//        )
//        );
//
//        Mockito.when(clienteRepository.save(Mockito.any(Cliente.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // ACT or WHEN
//        Cliente clienteCadastrado = clienteService.cadastrar(dto);
//
//        // ASSERT or THEN
//        assertNotNull(clienteCadastrado);
//        assertEquals(dto.nome(), clienteCadastrado.getNome());
//        assertEquals(dto.telefone(), clienteCadastrado.getTelefone());
//        assertEquals(EnderecoMapper.mapToEntity(dto.endereco()), clienteCadastrado.getEndereco());
//    }

    @Test
    void listarTodos() {
    }

    @Test
    void buscarPorId() {
    }

    @Test
    void buscarPorNome() {
    }

    @Test
    void atualizar() {
    }

    @Test
    void desativar() {
    }

    private ClienteDto criarClienteValido() {
        return new ClienteDto("Maria", "11999999999",
                new EnderecoDto(
                        "Rua A",
                        "123",
                        "apto 1",
                        "12345678",
                        "Centro",
                        "São Paulo",
                        "SP"
                )
        );
    }

}