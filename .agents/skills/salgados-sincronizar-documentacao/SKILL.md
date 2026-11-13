---
name: salgados-sincronizar-documentacao
description: "Conferir ou atualizar README, wiki, AGENTS.md e skills locais do Sistema Salgados da Lúcia conforme a implementação. Usar em revisões da documentação ou quando mudanças tornarem orientações e procedimentos do projeto desatualizados."
---

# Sincronizar documentação do Salgados

## Fontes de contexto

Não usar o Notion durante este procedimento: não consultar páginas, memórias ou skills, nem criar ou atualizar registros. Obter contexto no repositório, na conversa e, quando pertinente, no GitHub. Entregar os resultados na conversa ou no destino solicitado pelo usuário. Se faltar informação, indicar a lacuna ou perguntar ao usuário.

## Comparação e atualização

1. Leia o `AGENTS.md` na raiz, eventuais `AGENTS.md` dos diretórios afetados, o README, as skills locais e as páginas vigentes da wiki pertinentes ao pedido. Use os links do README para localizar a wiki. Delimite os assuntos; uma correção localizada não exige revisar a documentação inteira.
2. Registre o commit usado na revisão com `git rev-parse HEAD`. Identifique separadamente alterações locais que influenciem as conclusões; não as atribua ao commit. Inspecione código, migrações, configuração e testes relacionados, incluindo os dois lados de fluxos que cruzem API e interface.
3. Classifique o estado com evidências: **Implementado** quando a capacidade descrita existe no alcance documentado; **Parcial** quando só parte está disponível, especificando o que falta; **Planejado** quando representa intenção futura sem implementação correspondente. Falta de acesso ou evidência é incerteza, não prova de ausência ou cancelamento.
4. Distinga capacidade disponível somente na API daquela acessível pela interface. Separe o comportamento efetivo das intenções futuras, preservando requisitos, regras planejadas, identificadores RF/RNF/CSU, hierarquia e links existentes. Se precisar acrescentar identificadores, consulte a sequência vigente para evitar colisões.
5. Mantenha o README como ponto de entrada e a wiki para detalhes. Faça a menor atualização que resolva a divergência; não duplique manuais no README ou na skill. Vincule evidências ao commit revisado quando possível. Não altere código para adequá-lo à documentação durante esta tarefa.
6. Quando a mudança afetar arquitetura, comandos de execução ou validação, organização dos arquivos ou convenções permanentes, atualize o `AGENTS.md` correspondente ao alcance da orientação. Quando alterar um procedimento reutilizável, atualize a skill local afetada e suas referências; revise nome e descrição de descoberta apenas se sua finalidade mudar. Mantenha os metadados existentes coerentes e preserve a configuração de seleção automática. Não transforme uma correção isolada, estado temporário de tarefa ou resultado de teste em regra permanente.
7. Preserve a divisão de responsabilidades: README apresenta o projeto, wiki detalha o sistema, `AGENTS.md` reúne convenções permanentes e skills descrevem procedimentos. Altere somente os destinos que precisem de ajuste e mantenha links entre eles em vez de duplicar conteúdo. A manutenção das skills, inclusive desta, deve preservar a regra de não usar o Notion e não ampliar autorizações ou escopo.

## Conferência e publicação

- Confira destinos e âncoras dos links alterados, referências entre páginas e navegação afetada. Valide sintaxe e renderização dos diagramas alterados quando houver ferramenta disponível; informe quando a renderização não puder ser conferida.
- Não transforme testes existentes em resultados de execução. Execute apenas os checks pertinentes quando necessários para sustentar a revisão e relate seus limites.
- Se alterar uma skill, confira seu frontmatter, referências, condições de uso e coerência com o `AGENTS.md`. Execute `quick_validate.py` da skill-creator quando disponível; se não estiver disponível, revise a estrutura e informe essa limitação. A instrução principal deve continuar utilizável por ferramentas que leiam `SKILL.md`, sem depender de `agents/openai.yaml`.
- Quando o pedido for uma proposta, entregue-a sem publicar. Para edição ou publicação solicitada, releia os destinos antes de escrever, preserve edições concorrentes e verifique o conteúdo gravado depois. Atualizar a wiki não implica criar commit ou push na aplicação.
- Se a wiki estiver inacessível, entregue a proposta na conversa, indicando páginas e trechos de destino, e informe explicitamente que não foi publicada. Não use outro serviço como destino alternativo.
- Na entrega, identifique documentos e orientações revisados, commit de referência, diferenças de estado relevantes e verificações ou publicações pendentes. Explique quais convenções ou procedimentos mudaram se houver atualização de `AGENTS.md` ou skills.
