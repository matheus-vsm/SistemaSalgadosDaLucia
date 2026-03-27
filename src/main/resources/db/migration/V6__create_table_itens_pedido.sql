CREATE TABLE itens_pedido (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id       BIGINT          NOT NULL,
    salgado_id      BIGINT          NOT NULL,
    quantidade      INT             NOT NULL,
    preco_unitario  DECIMAL(10,2)   NOT NULL,
    sub_total       DECIMAL(10,2)   NOT NULL,
    tipo_preco      VARCHAR(20)     NOT NULL,
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT fk_item_pedido_salgado FOREIGN KEY (salgado_id) REFERENCES salgados(id)
);