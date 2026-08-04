async function buscarEnderecoPorCep(cep) {
    console.log('Buscando endereço...')
    const cepLimpo = cep.replace(/\D/g, '');

    if (cepLimpo.length !== 8) {
        return null;
    }

    const response = await fetch(`https://viacep.com.br/ws/${cepLimpo}/json/`);
    const dados = await response.json();

    if (dados.erro) {
        return null;
    }

    return {
        logradouro: dados.logradouro,
        bairro: dados.bairro,
        cidade: dados.localidade,
        uf: dados.uf
    };
}