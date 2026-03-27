CREATE TABLE estoque (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salgado_id  BIGINT NOT NULL UNIQUE,
    quantidade  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_estoque_salgado FOREIGN KEY (salgado_id) REFERENCES salgados(id)
);