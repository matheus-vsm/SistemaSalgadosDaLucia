package br.com.salgadosdalucia.api.exception;

// RuntimeException para não poluir o Service com 'throws'
// trata erros de negócio. por ex: if (!cliente.isAtivo()) { throw new NegocioException("Cliente com ID " + request.clienteId() + " está inativo."); }
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
