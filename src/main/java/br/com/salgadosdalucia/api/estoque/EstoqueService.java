package br.com.salgadosdalucia.api.estoque;

import br.com.salgadosdalucia.api.salgado.SalgadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;

    private final SalgadoRepository salgadoRepository;

}
