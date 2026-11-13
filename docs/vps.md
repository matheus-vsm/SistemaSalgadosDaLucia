# Primeira publicação e operação na VPS

## Situação desta entrega

Os arquivos estão preparados para uma VPS **Ubuntu Server 24.04 LTS, linux/amd64**, com banco novo.
Não foram contratados servidor, domínio ou armazenamento; nenhum deploy, certificado público, release ou agendamento
foi ativado nesta execução. Complete e registre as verificações abaixo antes de habilitar o deploy automático.
Reveja também as [pendências da aplicação](operacao.md#pendências-da-aplicação-antes-da-publicação).

## 1. Preparar o servidor

Instale Docker Engine e Compose pelo [procedimento oficial para Ubuntu](https://docs.docker.com/engine/install/ubuntu/).
Instale também `restic`, `curl`, `git`, `jq` e `util-linux` pelos pacotes da distribuição. O processo de deploy usa `flock`.
Crie o usuário operacional `salgados`, configure SSH por chave e permita seu acesso ao Docker. O grupo Docker concede
controle administrativo sobre o host; mantenha a chave de deploy dedicada e o acesso à VPS restrito.

Crie os diretórios com proprietário `salgados`, grupo `salgados` e modo 750:

```text
/opt/salgados/incoming
/opt/salgados/releases
/opt/salgados/shared
/opt/salgados/shared/acme
/opt/salgados/shared/letsencrypt
```

Publique apenas SSH, HTTP 80 e HTTPS 443 no firewall da VPS e do provedor. O Compose não publica MySQL nem backend.
Confira isso externamente: portas Docker publicadas podem [contornar regras do UFW](https://docs.docker.com/engine/network/packet-filtering-firewalls/).
Configure o DNS A do domínio para a VPS; publique AAAA somente se IPv6 também estiver funcionando na configuração utilizada.

## 2. Configurar a primeira release e os segredos

Integre os PRs em ordem e confirme o CI. Mantenha `PRODUCTION_DEPLOY_ENABLED` ausente/desabilitado.
Quando decidir publicar, uma tag `vX.Y.Z` na main executará o CI e produzirá as imagens no GHCR. Não existe tag automática.
O resumo do workflow informa os dois digests. Se os pacotes forem privados, autentique o usuário operacional no GHCR
com uma credencial de leitura de pacotes, sem colocar o token em comandos, Git ou arquivos da aplicação.

Na VPS, obtenha o código exato do commit da tag e prepare uma release imutável, como usuário `salgados`:

```bash
# Execute dentro de um clone do repositorio; COMMIT deve ser o SHA completo da versao validada.
set -euo pipefail
RELEASE_SHA=COMMIT
RELEASE="/opt/salgados/releases/$RELEASE_SHA"
test ! -e "$RELEASE" && mkdir -m 750 "$RELEASE"
git archive "$RELEASE_SHA" | tar -x -C "$RELEASE"
```

Crie `$RELEASE/images.env` com `BACKEND_IMAGE` e `FRONTEND_IMAGE` copiados do resumo do workflow, usando
`ghcr.io/...@sha256:...`, nunca uma imagem local ou tag mutável. Não sobrescreva um diretório de release existente.
Copie `infra/production/.env.example` para `/opt/salgados/shared/app.env` e preencha valores reais:

- Senhas distintas de banco/aplicação e root, segredo JWT forte e emissor.
- `DOMAIN` com nome DNS completo, `ACME_EMAIL` real e `STATE_DIR=/opt/salgados/shared`.
- `BACKEND_URL=http://backend:8080` dentro da rede Docker.
- `TLS_ENABLED=false` durante a emissão inicial. Revise os termos da autoridade certificadora antes de definir
  `ACME_TERMS_ACCEPTED=true`; a emissão recusa prosseguir enquanto esse aceite estiver falso.

Use arquivos modo 600, pertencentes ao usuário operacional. Não versione segredos, certificados ou diretórios de estado.
Mantenha-os fora do clone. Variáveis exportadas no terminal têm prioridade sobre os arquivos do Compose; remova valores
de desenvolvimento antes de operar a VPS. Não imprima `docker compose config` completo, pois inclui valores sensíveis.

Defina um auxiliar no terminal para esta release:

```bash
dc() {
  docker compose --env-file /opt/salgados/shared/app.env --env-file "$RELEASE/images.env" \
    -p salgados-production -f "$RELEASE/infra/production/compose.yaml" "$@"
}
dc config --quiet
bash "$RELEASE/infra/deploy/validate-images.sh" "$RELEASE/images.env"
dc pull db backend frontend
dc up -d --no-build --wait --wait-timeout 240 db backend frontend
```

O MySQL cria o banco no primeiro volume, e o Flyway aplica as migrações. As senhas de inicialização não alteram um volume
já existente. No modo inicial, o Nginx permite apenas saúde e desafio ACME por HTTP; o restante retorna 503.
Isso impede enviar credenciais de login por HTTP enquanto não houver certificado.

## 3. Primeiro acesso e HTTPS

Crie o administrador pelo comando explícito. Digite credenciais próprias; a senha não deve aparecer no histórico:

```bash
read -r -p 'Nome: ' BOOTSTRAP_ADMIN_NAME
read -r -p 'Username: ' BOOTSTRAP_ADMIN_USERNAME
read -r -s -p 'Senha: ' BOOTSTRAP_ADMIN_PASSWORD
printf '\n'
export BOOTSTRAP_ADMIN_NAME BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_PASSWORD
dc run --rm --no-deps -e SPRING_PROFILES_ACTIVE=prod,bootstrap-admin \
  -e BOOTSTRAP_ADMIN_NAME -e BOOTSTRAP_ADMIN_USERNAME -e BOOTSTRAP_ADMIN_PASSWORD backend
unset BOOTSTRAP_ADMIN_NAME BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_PASSWORD
```

Após conferir DNS, portas e o aceite dos termos:

```bash
bash "$RELEASE/infra/production/certificate.sh" issue "$RELEASE"
# Edite shared/app.env: TLS_ENABLED=true.
dc up -d --no-build --wait --wait-timeout 240 frontend
dc exec -T frontend nginx -t
bash "$RELEASE/infra/production/certificate.sh" dry-run "$RELEASE"
```

Confirme HTTPS válido externamente, redirecionamento HTTP e login real. Os certificados são persistentes e montados
somente para leitura no Nginx. A renovação usa webroot e recarrega o Nginx após validar a configuração.
Os caminhos da API continuam os mesmos; não é necessário alterar JavaScript para domínio ou endereço da API.
O proxy dos containers consulta o DNS interno do Docker periodicamente para acompanhar mudanças de IP do backend
após recriações, preservando o caminho e os parâmetros completos de cada requisição.

## 4. Backup externo e restauração de teste

Escolha armazenamento compatível com S3 e um repositório Restic dedicado ao sistema, por exemplo no formato
`s3:https://ENDPOINT/BUCKET/CAMINHO`. Endpoint, bucket e credenciais dependem do provedor escolhido.
Crie `/opt/salgados/shared/backup.env` a partir de `backup.env.example`, preenchendo valores literais sem aspas,
sem interpolação e com uma variável por linha. O carregador aceita somente os nomes previstos, sem executar código.

Guarde a senha Restic em `/opt/salgados/shared/restic-password`, modo 600. Guarde também uma cópia segura fora da VPS;
sem ela o backup criptografado não pode ser recuperado. Preserve banco/JWT e configurações em um gerenciador de segredos
separado: o snapshot contém o dump MySQL, não os arquivos de credenciais.

Exporte as variáveis de backup no terminal sem usar `source` em conteúdo não confiável, e inicialize explicitamente o
repositório uma única vez. Para reutilizar o carregador validado:

```bash
set -- "$RELEASE"
source "$RELEASE/infra/production/common.sh"
load_backup_environment
restic init
bash "$RELEASE/infra/production/backup.sh" "$RELEASE"
restic snapshots --tag salgados-mysql
```

O script só cria snapshot após o dump consistente terminar com sucesso. Faz verificação de integridade de metadados
e retenção de 7 diários, 4 semanais e 3 mensais. Arquivos SQL temporários têm acesso restrito e são removidos ao terminar.
Cada atualização da aplicação exige um backup bem-sucedido antes de aplicar migrações.

Selecione um ID de snapshot real e restaure em um projeto novo:

```bash
bash "$RELEASE/infra/production/restore.sh" ID_DO_SNAPSHOT salgados-restore-validacao "$RELEASE"
```

O comando recusa o projeto de produção, recusa volume já existente, cria somente um MySQL isolado e confere migrações
e usuários. Depois confira registros e totais. Para validar login sem publicar nenhuma porta adicional:

```bash
bash "$RELEASE/infra/production/validate-restored-login.sh" salgados-restore-validacao "$RELEASE"
```

O comando sobe apenas o backend isolado e solicita credenciais de um usuário do banco restaurado; os tokens não são
impressos. Registre o teste antes de habilitar a implantação. Encerre e remova somente esse projeto
descartável depois de confirmar a recuperação; nunca remova o volume `salgados-production_mysql-data`.

## 5. Habilitar operação e atualização

Após HTTPS, login, operações principais e restauração externa comprovados, registre a primeira release:

```bash
ln -sfn "$RELEASE" /opt/salgados/current
# Somente apos concluir e registrar as validacoes acima:
touch /opt/salgados/shared/production-ready
```

Instale as quatro unidades de `infra/production/systemd` em `/etc/systemd/system/` após revisar os caminhos e o usuário:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now salgados-backup.timer salgados-certificate.timer
systemctl list-timers 'salgados-*'
```

Isso ativa backup diário às 03h de São Paulo, com pequeno atraso aleatório, e verificação de renovação duas vezes por dia.
Backup diário admite perda de aproximadamente 24 horas de alterações. Nada disso foi agendado nesta entrega.

Configure o ambiente GitHub `production` e seus segredos SSH conforme [CI/CD](operacao.md#cicd-e-versões).
Só então habilite `PRODUCTION_DEPLOY_ENABLED=true`. Próximas tags validadas enviam o código exato e os digests à VPS.
O deploy é serializado, testa a configuração, faz backup, espera saúde e verifica a página pública por HTTPS.
Em falha, a release candidata fica registrada; use o rollback manual da aplicação somente com esquema compatível.
Não há downgrade Flyway nem restauração automática de dados. Não use `down -v` em produção.

## Verificações recorrentes

- `dc ps`, `dc logs --tail 100 backend` e `dc exec -T frontend nginx -t`.
- `journalctl -u salgados-backup.service -u salgados-certificate.service`: tratar execuções com falha.
- `restic snapshots`, data de `shared/last-backup` e testes periódicos de restauração.
- Validade pública do certificado, espaço em disco (`df -h`, `docker system df`) e `journalctl --disk-usage`.

Os logs Docker têm limite de 10 MB por arquivo e 3 arquivos por serviço. O monitoramento externo com notificações
fica como evolução; nesta primeira entrega as falhas devem ser conferidas no GitHub Actions e nos logs do servidor.
