package br.com.salgadosdalucia.api.enums;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enums")
@RequiredArgsConstructor
@CrossOrigin
public class EnumController {

    private final EnumService enumService;

    @GetMapping(value = "/{entidade}")
    @Operation(summary = "Listar enums", description = "Lista enums de uma determinada entidade para exibicao no front.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enums listados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Entidade não encontrada.")
    })
    public ResponseEntity<Map<String, List<EnumDTO>>> listarEnums(@PathVariable String entidade) {
        var enums = enumService.listarEnums(entidade);
        return ResponseEntity.ok(enums);
    }

}
