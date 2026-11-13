---
name: salgados-padronizar-telas
description: "Criar ou ajustar telas do Sistema Salgados da Lúcia, mantendo consistência de formulários, edição, cards, paginação e notificações. Usar em mudanças de interface deste projeto."
---

# Padronizar telas do Salgados

## Fontes de contexto

Não usar o Notion durante este procedimento: não consultar páginas, memórias ou skills, nem criar ou atualizar registros. Obter contexto no repositório, na conversa e, quando pertinente, no GitHub. Entregar os resultados na conversa ou no destino solicitado pelo usuário. Se faltar informação, indicar a lacuna ou perguntar ao usuário.

## Procedimento

1. Leia o `AGENTS.md` na raiz do projeto e os arquivos do módulo afetado. Identifique uma tela atual de referência para o comportamento ou aparência pedidos. Use Pedidos e Compras como referência do fluxo de edição, verificando o código vigente; não copie regras específicas desses domínios para outras entidades.
2. Confira `frontend/app.js`, os estilos compartilhados e os auxiliares de `frontend/js` pertinentes. Reutilize API, listagens, notificações e modais existentes antes de introduzir uma implementação paralela. Respeite a inicialização por `pagina:carregada` e evite acumular handlers ao navegar entre módulos.
3. Na edição pelo formulário de cadastro, preencha os dados e identifique o modo de edição. Mantenha salvar/cancelar, os bloqueios temporários de navegação e listagem previstos no fluxo e a restauração do cadastro ao concluir ou cancelar. Preserve os campos quando a gravação falhar.
4. Conforme o impacto da mudança, preserve filtros e página, trate páginas que ficarem vazias, impeça envios duplicados e respostas antigas substituindo estado recente. Reutilize o controle de carregamento da listagem. Recalcule alturas após atualizações e redimensionamento quando necessário; não fixe dimensões com base apenas em um exemplo curto.
5. Preserve mensagens específicas da API com os auxiliares de erro e notificações. Evite alertas nativos e mensagens duplicadas nos fluxos cobertos pelo sistema compartilhado. Respeite foco, teclado, rótulos e ações de somente leitura.
6. Limite a mudança ao alcance pedido. Se alterar um componente compartilhado, inspecione seus consumidores para conferir regressões; isso não autoriza redesenhar todos os módulos. Preserve diferenças intencionais entre as telas.

## Verificação e entrega

- Selecione os testes existentes pertinentes. Acrescente testes de comportamento quando houver lógica nova ou regressão concreta, sem criar testes que apenas conferem a escrita do CSS.
- Confira no navegador a apresentação em desktop e celular. Conforme o fluxo alterado, examine cadastro, edição, cancelamento, falha com dados preservados, carregamento, vazio, conteúdo comprido e paginação após atualizar registros.
- Informe o que mudou, a referência adotada e as telas verificadas. Separe testes automatizados de validação visual. Se não houver navegador ou ambiente disponível, declare a conferência visual pendente; prévias com dados fictícios não comprovam integração real.
