package br.com.salgadosdalucia.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
// resolve warning sobre o page. o recomendado na vdd é criar um PageResponse para controlar o page, porém tem que refatorar demais
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
//@OpenAPIDefinition(info = @Info(title = "API Salgados da Lúcia Kojima", version = "1.0.0", description = "API para Gerenciamento de Salgados da Lúcia Kojima", summary = "API para Gerenciamento de Salgados da Lúcia Kojima, com funcionalidades de gerenciamento de clientes, salgados, pedidos, compras e usuarios"))
public class ApiSalgadosdaluciaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiSalgadosdaluciaApplication.class, args);
    }
}
