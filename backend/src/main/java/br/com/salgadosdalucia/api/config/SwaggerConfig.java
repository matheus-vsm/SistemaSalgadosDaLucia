package br.com.salgadosdalucia.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI inicialAssessmentServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Salgados da Lúcia Kojima")
                        .version("1.0.0")
                        .description("API para Gerenciamento de Salgados da Lúcia Kojima")
                        .summary("API para Gerenciamento de Salgados da Lúcia Kojima, com funcionalidades de gerenciamento de clientes, salgados, pedidos, compras e usuarios")
                );
    }
}
