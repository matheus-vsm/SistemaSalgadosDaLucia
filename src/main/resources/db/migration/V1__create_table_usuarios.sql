CREATE TABLE usuarios (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome    VARCHAR(100)    NOT NULL,
    login   VARCHAR(50)     NOT NULL UNIQUE,
    senha   VARCHAR(255)    NOT NULL,
    perfil  VARCHAR(20)     NOT NULL
);