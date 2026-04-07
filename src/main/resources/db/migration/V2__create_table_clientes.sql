CREATE TABLE clientes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(100)    NOT NULL,
    telefone        VARCHAR(20)     NOT NULL,
    logradouro      VARCHAR(150),
    numero          VARCHAR(10),
    complemento     VARCHAR(100),
    bairro          VARCHAR(100),
    cep             VARCHAR(10),
    cidade          VARCHAR(100),
    uf              CHAR(2)
);