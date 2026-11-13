# Guia para agentes — Salgados da Lúcia

## Contexto e arquitetura

Sistema de gestão de clientes, salgados, estoque, pedidos, compras e usuários. Consulte o README para execução e a wiki do repositório para requisitos, regras e documentação detalhada. Confirme comportamentos no código e nos testes; documentação pode estar desatualizada.

- Backend em `backend`: Java 21, Spring Boot, Maven Wrapper, JPA, MySQL e Flyway. Organização por funcionalidade em `br.com.salgadosdalucia.api`.
- Frontend em `frontend`: HTML, CSS e JavaScript sem framework ou build npm, com templates e scripts por módulo. Sirva por HTTP; o carregamento usa `fetch`.
- `infra` e os workflows descrevem execução em containers e verificações de integração. Sua presença não significa que alterações operacionais foram solicitadas.

## Convenções de implementação

- Acompanhe os padrões do módulo afetado: controllers para contratos HTTP, serviços para regras e transações, repositories para persistência e DTOs/mappers para entrada e saída. Valide os dados de entrada e mantenha validações de negócio nos serviços.
- Evolua o banco por novas migrações Flyway. Não reescreva migrações já aplicadas para corrigir o esquema.
- Preserve contratos e permissões existentes salvo quando sua alteração fizer parte do pedido.
- No frontend, respeite o carregamento dinâmico de `app.js` e a inicialização pelo evento `pagina:carregada`.
- Reutilize os auxiliares de `frontend/js` para API, notificações, listagens, modais e CEP. `apiJson` retorna `{ response, data }` no sucesso e lança `ApiError` em falhas; preserve as mensagens específicas retornadas pelo backend.
- Use telas existentes como referência, considerando diferenças intencionais entre os domínios. Mantenha o padrão de edição pelo formulário de cadastro onde aplicável.
- Faça mudanças focadas. Preserve alterações locais e não implemente funcionalidades incidentais fora do escopo; comunique os problemas encontrados.
- Ao criar branches de trabalho, nunca use o prefixo `codex/`. Escolha o prefixo pela finalidade: `feature/` para funcionalidades e capacidades novas, `bugfix/` para correções, `hotfix/` para correções urgentes em produção, `docs/` para documentação, `refactor/` para refatoração, `test/` para testes e `chore/` para manutenção. Use uma descrição curta em minúsculas com hífens após o prefixo. Não renomeie branches existentes fora do pedido.
- Nas criações ou atualizações solicitadas de issues/cards e PRs no GitHub, inclua `matheus-vsm` em Assignees e preencha Labels com classificações pertinentes já existentes no repositório. Preserve responsáveis e labels existentes, salvo mudança solicitada. Para itens de Projects vinculados a issues/PRs, confira esses campos no item de origem. Não crie labels nem altere itens alheios à tarefa apenas para aplicar a convenção; informe impedimentos de acesso ou ausência de classificação adequada.

## Validação

Execute verificações proporcionais ao impacto e informe o que foi realmente verificado. A partir da raiz, em PowerShell:

```powershell
# Testes do frontend; não exigem instalação npm
node --test frontend/tests/*.test.cjs

# Testes e verificações do backend
.\backend\mvnw.cmd -f backend/pom.xml verify

# Revisão de espaços e conflitos no diff
git diff --check
```

Para mudanças localizadas, selecione os testes pertinentes. Alterações visuais exigem conferência no navegador nos tamanhos relevantes; testes simulados de DOM não comprovam layout. Identifique validações não executadas e seus motivos, sem declarar sucesso com base em resultados antigos.

Consulte `.github/workflows/ci.yml` para os checks de integração HTTP, containers e scripts. Use ambientes isolados ao executar integração; não reutilize dados reais ou remova volumes existentes para reproduzir o CI.

## Documentação e skills

Mantenha o README como ponto de entrada e a wiki como documentação detalhada. Preserve funcionalidades planejadas, distinguindo-as do que está implementado. Registre evidências e o commit de referência em revisões da documentação; evite contagens de testes e outros números que envelhecem neste guia.

As skills locais em `.agents/skills` são selecionáveis automaticamente quando pertinentes:

- [salgados-padronizar-telas](.agents/skills/salgados-padronizar-telas/SKILL.md): criação e ajustes de telas, formulários, edição, listagens e notificações.
- [salgados-atualizar-card-pr](.agents/skills/salgados-atualizar-card-pr/SKILL.md): revisão de escopo de tarefas e redação de cards e PRs a partir das branches.
- [salgados-sincronizar-documentacao](.agents/skills/salgados-sincronizar-documentacao/SKILL.md): atualização ou conferência do README, wiki, `AGENTS.md` e skills locais conforme mudanças na implementação, nas convenções ou nos procedimentos.

Os procedimentos dessas três skills não utilizam o Notion, nem para buscar contexto, nem para salvar resultados. Use repositório, conversa e GitHub conforme o pedido. A disponibilidade de uma skill não autoriza publicar conteúdo, criar PRs ou realizar operações externas fora do que foi solicitado.
