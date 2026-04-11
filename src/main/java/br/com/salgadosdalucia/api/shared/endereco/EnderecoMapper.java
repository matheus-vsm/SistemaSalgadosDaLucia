package br.com.salgadosdalucia.api.shared.endereco;

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
