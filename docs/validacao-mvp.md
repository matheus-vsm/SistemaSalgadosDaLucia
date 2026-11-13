# Validação da preparação do MVP

Ambientes de teste isolados, sem publicação em VPS ou uso de dados de produção. Resultados não equivalem a auditoria
completa da aplicação. As branches e PRs seguem a ordem Nginx → containerização → backend → CI/CD → infraestrutura.

## Evidências

- 81 testes Node.js do frontend aprovados.
- 169 testes Maven aprovados, incluindo JWT em UTC/São Paulo e validação do bootstrap de administrador.
- MySQL 8.4 real: 14 migrações Flyway aplicadas e preservadas após recriação dos containers.
- Primeiro administrador criado por comando sem HTTP; repetição recusada e login preservado.
- Smoke HTTP autenticado: login, refresh, clientes, salgados, estoque, pedidos, compras, usuários e restrição de funcionário.
- Smoke de arquivos reais, tipos HTTP, arquivos inexistentes e isolamento do Actuator pelo Nginx.
- ShellCheck, actionlint e testes de bloqueio de deploy aprovados.
- CI hospedado da etapa CI/CD aprovado: [execução 43361271491](https://github.com/matheus-vsm/SistemaSalgadosDaLucia/actions/runs/43361271491).
- Nginx de produção validado localmente com certificado temporário explicitamente confiado pelo cliente: modo inicial
  HTTP 503, desafio ACME, redirecionamento HTTPS, página, saúde interna bloqueada e proxy para Spring real.
- Dump do MySQL de teste armazenado em repositório Restic local criptografado, verificado e recuperado byte a byte.
  Restauração em outro projeto Docker confirmou 14 migrações e login HTTP 200, sem publicar porta de backend/banco.
- Testes de backup comprovam que dump incompleto não cria snapshot, erro de verificação não marca sucesso e
  restauração recusa projeto de produção. Esses testes de bloqueio usam comandos simulados; a recuperação de dados
  acima utilizou MySQL e Restic reais.

## Pendências reais

- Revisar e integrar PRs em sequência; nenhum merge foi realizado pelo agente.
- Contratar/configurar VPS, domínio, DNS, armazenamento S3 e acessos. Nenhuma credencial desses serviços foi criada.
- Emitir certificado público e testar renovação ACME real.
- Validar upload/restauração do armazenamento externo e ativar os timers apenas após comprovar recuperação.
- Validar a publicação GHCR por tag e habilitar deploy somente após os controles de prontidão da VPS.
- Resolver/revisar os casos de negócio registrados em `docs/operacao.md`, incluindo autenticação de inativos (#92)
  e edição de pedido sem endereço. A infraestrutura não corrige esses comportamentos anteriores.
- Completar verificação operacional em navegador com os usuários reais antes de abrir o MVP.

O Projects será atualizado pelo Work, a pedido do usuário, com o prompt entregue junto dos links dos PRs e issues.
