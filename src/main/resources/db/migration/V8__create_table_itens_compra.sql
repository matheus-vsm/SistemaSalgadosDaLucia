CREATE TABLE itens_compra (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    compra_id       BIGINT          NOT NULL,
    nome            VARCHAR(100)    NOT NULL,
    quantidade      INT             NOT NULL,
    valor_unitario  DECIMAL(10,2)   NOT NULL,
    sub_total       DECIMAL(10,2)   NOT NULL,
    CONSTRAINT fk_item_compra_compra FOREIGN KEY (compra_id) REFERENCES compras(id)
);