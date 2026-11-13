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
Os containers usam o perfil `prod`: Flyway cria o esquema, a URL JDBC não cria o banco automaticamente,
SQL detalhado/Swagger/H2 ficam desabilitados. O serviço MySQL cria o banco e o usuário no primeiro uso do volume.
Fora de containers, `DB_HOST` e `DB_PORT` têm padrões `localhost` e `3306`.

## Primeiro administrador

Após `docker compose up -d --build`, informe credenciais próprias nas variáveis abaixo no seu terminal, sem colocá-las
no histórico de comandos nem no `.env` permanente. No PowerShell, solicite a senha sem eco:

```powershell
$env:BOOTSTRAP_ADMIN_NAME = Read-Host 'Nome do administrador'
$env:BOOTSTRAP_ADMIN_USERNAME = Read-Host 'Username do administrador'
$adminPassword = Read-Host 'Senha do administrador' -AsSecureString
$env:BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminPassword).Password
try {
    docker compose run --rm --no-deps -e "SPRING_PROFILES_ACTIVE=prod,bootstrap-admin" -e BOOTSTRAP_ADMIN_NAME -e BOOTSTRAP_ADMIN_USERNAME -e BOOTSTRAP_ADMIN_PASSWORD backend
} finally {
    Remove-Item Env:BOOTSTRAP_ADMIN_NAME, Env:BOOTSTRAP_ADMIN_USERNAME, Env:BOOTSTRAP_ADMIN_PASSWORD
    $adminPassword = $null
}
```

O processo executa sem abrir porta HTTP, cria um usuário ativo com BCrypt e perfil ADMIN e encerra.
Uma segunda execução com o mesmo username falha, sem trocar senha ou sobrescrever o registro. Nenhum administrador
é criado na subida normal. O limite da senha no BCrypt é 72 bytes UTF-8; preencha nome e username conforme o cadastro.
Para Linux, o procedimento está na seção de implantação, quando a infraestrutura estiver configurada.

```powershell
docker compose stop
docker compose start
docker compose down
```

`down` preserva o volume do banco. Não use `down -v` para atualizar: ele remove os dados.
As variáveis de inicialização do MySQL só criam banco e usuário no primeiro uso do volume; mudar `.env` não troca
automaticamente a senha de um banco existente. Recriar imagens não substitui migrações Flyway.

Os healthchecks verificam banco, readiness da API e frontend. A subida aguarda a saúde de cada dependência.
A saúde da API é acessível apenas na rede interna, em `/api/salgados-da-lucia-kojima/actuator/health/readiness`;
o Nginx bloqueia o prefixo Actuator. Os detalhes de saúde não são expostos. Tokens expiram por instante UTC,
independentemente do fuso do servidor; `TZ=America/Sao_Paulo` mantém os horários locais de negócio nos containers.

## Pendências da aplicação antes da publicação

Esta entrega prepara a implantação, mas não substitui a revisão de regras e segurança da aplicação.
O controle de autenticação de usuário inativo segue acompanhado em [#92](https://github.com/matheus-vsm/SistemaSalgadosDaLucia/issues/92).
No teste integrado, editar um pedido sem `enderecoEntrega` retornou 500 por acesso a DTO nulo no mapeamento.
O smoke cobre edição com endereço completo; o caso sem endereço precisa de correção específica antes de liberar esse fluxo.

## CI/CD e versões

O workflow CI testa Maven/Node, scripts shell e o conjunto Docker com MySQL real e banco descartável.
Executa em pull requests (incluindo bases encadeadas), pushes na main e chamadas do workflow de publicação.
As credenciais de CI são aleatórias e temporárias; não são credenciais de implantação.

Após integrar e revisar a sequência de PRs, uma tag `vX.Y.Z` cujo commit pertença ao histórico da main dispara
novamente os testes e publica imagens linux/amd64 no GHCR. As referências de deploy usam digest, não tags mutáveis.
Não há criação automática de GitHub Release nem de tags. Nenhuma imagem é publicada em push de branch comum.

O deploy SSH só é executado quando a variável de repositório `PRODUCTION_DEPLOY_ENABLED` estiver exatamente `true`.
Mantenha-a ausente/desabilitada até concluir a infraestrutura, HTTPS, backup e restauração de teste.
O ambiente GitHub `production` deverá conter:

- `PRODUCTION_SSH_HOST`, `PRODUCTION_SSH_USER` e, opcionalmente, `PRODUCTION_SSH_PORT` (padrão 22).
- `PRODUCTION_SSH_KEY`: chave dedicada ao deploy.
- `PRODUCTION_SSH_KNOWN_HOSTS`: chave pública do servidor, conferida por canal independente. Não desabilite a verificação SSH.

A VPS precisará de acesso de leitura às imagens no GHCR caso os pacotes sejam privados. O workflow usa GITHUB_TOKEN
somente para publicar; não copia esse token para a VPS. Segredos de banco/JWT ficam em `/opt/salgados/shared/app.env`,
fora das imagens e do arquivo de release. Diretórios e arquivos operacionais terão acesso restrito ao usuário responsável.

As releases ficam em `/opt/salgados/releases/<commit>`. Os scripts recebem as imagens exatas, validam o Compose,
executam backup antes de migrar e aguardam healthchecks. `current`, `previous` e `candidate` registram o estado da atualização.
Uma falha não dispara rollback de banco nem remove volumes. A aplicação pode ter breve indisponibilidade durante a atualização.

```bash
# Na VPS, após a preparação da infraestrutura da próxima etapa:
bash /opt/salgados/releases/COMMIT/infra/deploy/deploy.sh --check /opt/salgados/releases/COMMIT
bash /opt/salgados/current/infra/deploy/rollback.sh
```

Só use rollback se a migração aplicada mantiver compatibilidade com a versão anterior. Uma migração destrutiva exige
plano específico e restauração explicitamente supervisionada. Os arquivos em `infra/production` e o [guia da VPS](vps.md)
contêm configuração de certificados, backup e procedimentos de ativação. O deploy permanece desabilitado enquanto
servidor, domínio, HTTPS e recuperação externa não forem configurados e validados.
