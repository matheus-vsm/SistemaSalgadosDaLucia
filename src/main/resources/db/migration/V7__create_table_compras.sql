CREATE TABLE compras (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_compra DATE            NOT NULL,
    valor_total DECIMAL(10,2)   NOT NULL,
    observacao  VARCHAR(255)
);