# Prompt para atualizar o GitHub Projects no Work

Copie o conteúdo abaixo para uma conversa no Work com acesso ao GitHub Projects.

```text
Atualize meu GitHub Projects para refletir a preparação do MVP já implementada pelo Codex.

Projeto: https://github.com/users/matheus-vsm/projects/3
Repositório: https://github.com/matheus-vsm/SistemaSalgadosDaLucia

Primeiro releia as issues e PRs abaixo, confira o estado atual e procure os cards existentes. Preserve campos,
assignees, estimativas e histórico. Reutilize o campo Status e as colunas existentes; não crie esquema ou quadro novo.

Ordem e vínculos:
1. Pré-requisito Nginx: issue #78, branch feature/nginx, PR #102 com base main.
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/issues/78
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/pull/102
2. Containerização (antiga etapa 2): issue #94, branch codex/containerizacao, PR #103 com base feature/nginx.
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/issues/94
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/pull/103
3. Backend para produção (antiga etapa 1): issue #100, branch codex/backend-producao, PR #104 com base codex/containerizacao.
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/issues/100
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/pull/104
4. CI/CD (etapa 3): issue #95, branch codex/ci-cd, PR #105 com base codex/backend-producao.
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/issues/95
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/pull/105
5. Infraestrutura (etapa 4): issue #101, branch codex/infra-producao, PR #106 com base codex/ci-cd.
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/issues/101
   https://github.com/matheus-vsm/SistemaSalgadosDaLucia/pull/106

Adicione as issues #100 e #101 ao projeto se ainda não estiverem nele. Reutilize os cards de #94, #95 e #78.
Não crie novas issues, cards duplicados ou cards separados para cada PR. Vincule os PRs às issues usando os
recursos existentes e mantenha visíveis as dependências na ordem #78 → #94 → #100 → #95 → #101.

Se os PRs continuarem abertos e as implementações aguardando revisão, coloque os respectivos cards em In review.
Não feche issues nem mova para Done apenas porque há código. Se algo mudou desde a implementação, use o estado
atual verificado e explique qualquer divergência; não assuma que houve merge ou implantação.

As issues contêm checklists e evidências. Consulte também docs/validacao-mvp.md na branch codex/infra-producao.
As validações incluíram 169 testes Maven, 81 testes frontend, smoke HTTP autenticado com MySQL real, HTTPS local
e recuperação criptografada Restic com login no banco restaurado. Confira o CI mais recente em cada PR.

Mantenha pendentes os itens externos: VPS, domínio/DNS, armazenamento S3, emissão/renovação pública de certificados,
backup e restauração externa e ativação de deploy. Não marque implantação como concluída. Permanecem também as
pendências de negócio documentadas: autenticação de usuário inativo (#92) e edição de pedido sem endereço.

Nesta tarefa, altere apenas a organização e os vínculos no Projects. Não implemente código, faça merges, crie tags
ou releases, habilite deploy, altere permissões/segredos, agende rotinas ou contrate serviços.

No final, mostre os links dos cards, seus estados e as pendências que continuam abertas.
```
