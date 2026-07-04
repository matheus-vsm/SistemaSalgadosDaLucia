package br.com.salgadosdalucia.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) // resolve warning sobre o page. o recomendado na vdd é criar um PageResponse para controlar o page, porém tem que refatorar demais
public class ApiSalgadosdaluciaApplication {
	public static void main(String[] args) {
		SpringApplication.run(ApiSalgadosdaluciaApplication.class, args);
	}
}
