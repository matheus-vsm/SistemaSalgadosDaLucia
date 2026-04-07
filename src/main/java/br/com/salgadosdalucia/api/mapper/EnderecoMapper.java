package br.com.salgadosdalucia.api.mapper;

import br.com.salgadosdalucia.api.dto.EnderecoDto;
import br.com.salgadosdalucia.api.model.Endereco;

public class EnderecoMapper {

    public static Endereco mapToEntity(EnderecoDto dto) {
        return new Endereco(dto.logradouro(), dto.numero(), dto.complemento(), dto.cep(),
                dto.bairro(), dto.cidade(), dto.uf());
    }

    public static EnderecoDto mapToDto(Endereco endereo) {
        return new EnderecoDto(endereo.getLogradouro(), endereo.getNumero(), endereo.getComplemento(),
                endereo.getCep(), endereo.getBairro(), endereo.getCidade(), endereo.getUf());
    }

}
