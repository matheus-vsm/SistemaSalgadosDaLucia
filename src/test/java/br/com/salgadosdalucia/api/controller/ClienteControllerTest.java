package br.com.salgadosdalucia.api.controller;

import br.com.salgadosdalucia.api.cliente.*;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
//@SpringBootTest // Sobe a aplicação inteira - Controller, Service, Repository, Banco (H2), Flyway, Segurança, Tudo. USADO PARA TESTE DE INTEGRAÇÃO
@WebMvcTest(ClienteController.class) // Sobe só a camada web - Controller, MockMvc e Jackson. USADO PARA TESTE DE API/UNIDADE
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    // ambos servem para converter objetos Java em JSON e vice-versa, facilitando a escrita de testes que envolvem requisições e respostas JSON.
    @Autowired
    private JacksonTester<Cliente> jsonCliente;

    @Autowired
    private JacksonTester<ClienteDto> jsonClienteDto;

    @Autowired
    private ClienteService clienteService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ClienteService clienteService() {
            return Mockito.mock(ClienteService.class);
        }
    }

    @Test
    @DisplayName("201 - Deve retornar 201 ao cadastrar um cliente de forma correta (1)")
    void deveRetornar201AoCadastrarClienteDeFormaCorreta1() throws Exception {
        // ARRANGE
        ClienteDto dto = new ClienteDto("Matheus", "11999999999",
                new EnderecoDto(
                        "Rua Brasil",
                        "123",
                        null,
                        "12345678",
                        "Centro",
                        "São Paulo",
                        "SP"
                )
        );

        var clienteEsperado = ClienteMapper.mapToEntity(dto);
        var jsonResponse = jsonCliente.write(clienteEsperado).getJson(); // converte o dto para JSON para comparação

        when(clienteService.cadastrar(dto)).thenReturn(clienteEsperado); // simula o comportamento do service para retornar o cliente cadastrado

        // ACT
        var response = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON) // define o corpo da requisição (JSON)
                        .content(jsonClienteDto.write(dto).getJson())) // converte o dto para JSON e envia no corpo da req
                .andReturn().getResponse();

        // ASSERT
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.getContentAsString()).isEqualTo(jsonResponse);
    }

    @Test
    @DisplayName("201 - Deve retornar 201 ao cadastrar um cliente de forma correta (2)")
    void deveRetornar201AoCadastrarClienteDeFormaCorreta2() throws Exception {
        // ARRANGE
        ClienteDto dto = new ClienteDto("Matheus", "11999999999",
                new EnderecoDto(
                        "Rua Brasil",
                        "123",
                        null,
                        "12345678",
                        "Centro",
                        "São Paulo",
                        "SP"
                )
        );

        var clienteEsperado = ClienteMapper.mapToEntity(dto);

        when(clienteService.cadastrar(dto)).thenReturn(clienteEsperado); // simula o comportamento do service para retornar o cliente cadastrado

        var jsonRequest = jsonClienteDto.write(dto).getJson();
        var jsonResponse = jsonCliente.write(clienteEsperado).getJson(); // converte o dto para JSON para comparação

        // ACT + ASSERT
        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(content().json(jsonResponse));
    }

    @Test
    @DisplayName("400 - Deve retornar erro 400 ao cadastrar um cliente quando algum campo obrigatório não for preenchido corretamente")
    void deveRetornarErro400AoCadastrarClienteQuandoAlgumCampoObrigatorioNaoForPreenchidoCorretamente() throws Exception {
        // ARRANGE
        ClienteDto dto = new ClienteDto(null, "11999999999",
                new EnderecoDto(
                        "Rua Brasil",
                        "123",
                        null,
                        "12345678",
                        "Centro",
                        "São Paulo",
                        "SP"
                )
        );

        // ACT + ASSERT
        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON) // define o corpo da requisição (JSON)
                        .content(objectMapper.writeValueAsString(dto))) // converte o dto para JSON e envia no corpo da req
                .andExpect(status().isBadRequest()); // verifica se o status retornado é o 400 BAD REQUEST
    }

    @Test
    void listarClientes() {
    }

    @Test
    void buscarClientePorId() {
    }

    @Test
    void buscarClientePorNome() {
    }

    @Test
    void atualizarCliente() {
    }

    @Test
    void desativarCliente() {
    }
}
