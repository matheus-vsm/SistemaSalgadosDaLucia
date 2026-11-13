---
name: salgados-atualizar-card-pr
description: "Revisar o escopo de cards e preparar títulos e descrições de PRs do Sistema Salgados da Lúcia a partir de tarefas, branches e commits. Usar para redação ou atualização solicitada de cards e PRs."
---

# Atualizar cards e PRs do Salgados

## Fontes de contexto

Não usar o Notion durante este procedimento: não consultar páginas, memórias ou skills, nem criar ou atualizar registros. Obter contexto no repositório, na conversa e, quando pertinente, no GitHub. Entregar os resultados na conversa ou no destino solicitado pelo usuário. Se faltar informação, indicar a lacuna ou perguntar ao usuário.

## Análise do escopo

Ao propor ou criar uma branch de trabalho, nunca use `codex/`. Use os prefixos principais conforme a finalidade: `feature/` (funcionalidade ou capacidade nova), `bugfix/` (correção), `hotfix/` (correção urgente em produção), `docs/` (documentação), `refactor/` (refatoração), `test/` (testes) ou `chore/` (manutenção). Complete com uma descrição curta em minúsculas separada por hífens. Esta convenção não autoriza criar ou renomear branches durante um pedido apenas de redação.

1. Leia o `AGENTS.md` na raiz e a tarefa ou PR existente, quando fornecidos. Identifique branch de trabalho, base e período solicitado. Prefira a base declarada no PR; na ausência de PR, confirme a branch de destino pelo contexto e referências Git. Se permanecer ambígua, esclareça antes de atribuir mudanças ao trabalho.
2. Examine commits e o diff a partir do ancestral comum com a base, incluindo arquivos pertinentes para entender o comportamento final. Um título de commit isolado não comprova a entrega. Respeite filtros de data pedidos e diferencie alterações locais não commitadas das alterações da branch.
3. Separe objetivos principais, ajustes incidentais e pendências. Use correções explícitas do usuário para decidir o escopo. Uma correção incidental pode constar como observação no PR sem se tornar objetivo ou critério de aceite do card.

## Redação

Consulte [modelos de card e PR](references/modelos.md) para a estrutura. Adapte ao conteúdo existente e ao pedido:

- Card: descreva o trabalho a realizar com Objetivo, Requisitos e Critérios de aceite. Use requisitos no nível do resultado esperado; não transforme cada seletor, campo reposicionado ou detalhe já implementado em requisito. Nomeie entidades específicas quando a diferença entre elas for relevante para o escopo.
- PR: descreva o comportamento entregue e a razão das mudanças, com validações reais e limitações. Identifique correções complementares sem confundi-las com os objetivos principais.
- Preserve identificadores do Jira, vínculos, hierarquia, metadados e histórico relevante. Não altere status, responsáveis ou relações apenas por reescrever título e descrição.
- Não atribua resultados antigos de testes à revisão atual. Se citar evidência de uma execução anterior, identifique sua origem e não a apresente como execução própria.

## Entrega e edição externa

Quando o pedido for gerar texto, entregue título e descrição na conversa. Edite ou publique no GitHub apenas quando solicitado; criar PR, fazer merge e atualizar o card são ações distintas.

Antes de uma edição autorizada, releia o conteúdo vigente e preserve alterações concorrentes. Depois, confira título, corpo e os vínculos afetados. Se faltar acesso, entregue os textos e diga o que não foi publicado. Evite repetir uma escrita sem conferir o estado após uma resposta incerta.
