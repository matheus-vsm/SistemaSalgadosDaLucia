CREATE TABLE salgados (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                    VARCHAR(100)    NOT NULL UNIQUE,
    descricao               VARCHAR(255),
    preco_cento_congelado   DECIMAL(10,2),
    preco_cento_processado  DECIMAL(10,2),
    categoria               VARCHAR(10)     NOT NULL,
    ativo                   TINYINT(1)      NOT NULL DEFAULT 1
);