# Modelos de card e PR

Use as estruturas abaixo quando forem pertinentes; preserve formatos ou seções existentes que contenham informação útil. Substitua as orientações pelo conteúdo apurado, sem inventar requisitos ou evidências.

Para cards/issues e PRs, configure **Assignees** incluindo `matheus-vsm` e **Labels** com classificações existentes pertinentes. Esses são campos do GitHub; mencioná-los no corpo não substitui seu preenchimento. Preserve valores existentes. Em propostas de texto, indique os metadados recomendados sem publicá-los.

## Card

**Título:** verbo no infinitivo e resultado pretendido, preservando prefixos de identificação existentes.

### Objetivo

Descrever o problema e o resultado desejado para o usuário.

### Requisitos

Listar as capacidades necessárias no nível do comportamento. Por exemplo: melhorar a disposição dos campos para facilitar leitura e preenchimento, sem catalogar cada reposicionamento feito.

### Critérios de aceite

Descrever condições observáveis que comprovem os requisitos, incluindo falhas ou cancelamento quando relevantes. Evitar critérios que apenas repitam detalhes internos da solução.

Preservar fora dessas seções os identificadores, vínculos e histórico relevante já presentes.

## PR

**Título:** verbo e comportamento entregue, refletindo o escopo final.

**Descrição:** explicar o problema resolvido e como o sistema passa a se comportar. Agrupar mudanças relacionadas e mencionar correções incidentais separadamente quando ajudarem a revisão.

### Validação

Indicar comandos e resultados efetivamente verificados; distinguir testes automatizados de conferência visual e registrar pendências. Identificar a origem de evidências anteriores que forem citadas.

**Vínculo com a tarefa:** usar `Closes #N` somente quando o PR realmente concluir a tarefa e o fechamento automático for pretendido; para relação sem fechamento, usar `Refs #N`.
