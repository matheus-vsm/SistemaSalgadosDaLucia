package br.com.salgadosdalucia.api.enums;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enums")
@RequiredArgsConstructor
public class EnumController {

    private final EnumService enumService;

    @GetMapping(value = "/{entidade}")
    public Map<String, List<EnumDTO>> listarEnums(@PathVariable String entidade) {
        return enumService.listarEnums(entidade);
    }

}
