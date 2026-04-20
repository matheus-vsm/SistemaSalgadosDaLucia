package br.com.salgadosdalucia.api.estoque;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Validated
public class EstoqueController {

    private final EstoqueService service;

}
