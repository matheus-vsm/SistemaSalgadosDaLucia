# Docker e operação do MVP

## Desenvolvimento com containers

Inicie o Docker Desktop em modo Linux. Copie `.env.example` para `.env` e preencha as senhas e o segredo JWT
com valores próprios. Os exemplos não fornecem credenciais padrão. Encerre o Nginx local se ele ocupar a porta 5500.
Variáveis já definidas no terminal têm prioridade sobre `.env`; confira especialmente se `DB_USER_MYSQL` não
está herdando `root` do desenvolvimento. O MySQL do Compose exige um usuário de aplicação diferente de root.

```powershell
Copy-Item .env.example .env
# Edite .env antes dos próximos comandos.
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 backend
```

Abra http://127.0.0.1:5500. As chamadas usam `BACKEND_URL=http://backend:8080` dentro da rede Docker.
O banco usa `DB_HOST=db`, sem publicar a porta 3306. O fluxo Windows/Nginx do README continua disponível.
Esta primeira etapa mantém o provisionamento manual do administrador descrito no README; a próxima entrega
adiciona um comando próprio de primeiro acesso e o perfil de produção.

```powershell
docker compose stop
docker compose start
docker compose down
```

`down` preserva o volume do banco. Não use `down -v` para atualizar: ele remove os dados.
As variáveis de inicialização do MySQL só criam banco e usuário no primeiro uso do volume; mudar `.env` não troca
automaticamente a senha de um banco existente. Recriar imagens não substitui migrações Flyway.

Os healthchecks verificam banco e frontend. Na primeira entrega, o backend pode levar alguns segundos após
iniciar para ficar disponível; seu healthcheck específico será incluído na etapa de preparação para produção.
