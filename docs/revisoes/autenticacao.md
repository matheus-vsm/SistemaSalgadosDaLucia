# Revisão dos testes de autenticação

Data: 19/11/2026. Base de leitura: `f5047eb1f5f4adc15fb8f87916132943cc46bade`.
As correções e os testes descritos abaixo são alterações locais posteriores a essa base; não representam publicação ou implantação.

Separação de escopo em 19/11/2026: implementação, README e esta matriz pertencem à branch
`bugfix/melhorias-autenticacao`. Os arquivos dos novos testes permanecem na atividade
`test/ajustes-testes-unitarios`, após integração local do backend. A cobertura e os resultados abaixo
descrevem a validação conjunta da revisão anterior à separação, não a cobertura isolada da branch de backend.
A conclusão das melhorias depende também da entrega dos testes em sua atividade própria.

## Decisões e escopo

Revisão do fluxo de autenticação do backend, incluindo validação HTTP, serviço, JWT, provider de senha,
filtro e configuração de segurança. Frontend e módulos seguintes não foram alterados. O exemplo de
username duplicado motivou a revisão, mas o cadastro permanece na futura revisão de usuários.

O responsável aprovou nesta conversa, em 19/11/2026:

- Bloquear usuários inativos no login, renovação e uso de tokens já emitidos.
- Exigir finalidade explícita em cada JWT, impedindo troca de acesso por refresh e vice-versa.
- Manter refresh reutilizável até expirar, sem revogação/rotação com estado, preservando 60/120 minutos.

Não há decisão pendente dessas três regras. Logout, revogação após troca de senha, bloqueio por tentativas
e alterações dos prazos não foram incluídos nem decididos nesta revisão.

## Matriz de cenários

Na base havia apenas um teste de JWT neste módulo, combinando duração, fuso e assinatura, com uma
asserção genérica de RuntimeException. Não havia testes próprios de serviço, controller ou filtro.

Classificações: **C** = regra existente sem cobertura suficiente; **D** = defeito em relação ao contrato
estabelecido; **R** = regra ausente, discutida e aprovada. A cobertura indica a suíte após esta revisão.

| Situação | Esperado e fundamento | Base / classificação | Cobertura atual |
|---|---|---|---|
| Login correto | Provider valida BCrypt; tokens pertencem ao principal autenticado | Sem cobertura / C | Serviço e HTTP com provider real |
| Senha incorreta ou usuário inexistente | 401, mesma mensagem, nenhum token; contrato do login | Sem cobertura / C | Serviço e HTTP |
| Usuário inativo no login | 401, sem tokens; decisão aprovada | isEnabled ignorava ativo / R | HTTP com provider real e serviço |
| Username/senha ausente, nulo, vazio ou espaços | 400 antes de autenticar; @NotBlank dos DTOs | Sem cobertura / C | HTTP, cada campo isoladamente |
| Refresh ausente, nulo, vazio ou espaços | 400 antes de consultar usuário; @NotBlank | Sem cobertura / C | HTTP |
| Corpo ausente ou JSON malformado | 400, formato de erro da API | Sem cobertura / C | HTTP nos dois endpoints |
| Geração de acesso e refresh | Assinatura HMAC256, emissor configurado, subjects username/ID, duração 60/120 minutos | Parcial / C | JWT real e HTTP |
| Fuso UTC/São Paulo | Mesma duração e verificação; expiração usa Instant | Cobertura agregada / C | Teste preservado com restauração do fuso |
| Assinatura errada, payload adulterado, emissor errado/ausente | Rejeição específica como BusinessException | Parcial; exceção genérica / C | JWT real; assinatura/emissor também via HTTP |
| Token expirado ou malformado | Rejeitar sem emitir tokens ou buscar usuário | Sem cobertura / C | JWT, HTTP de refresh e filtro |
| JWT sem subject ou expiração | 400 no refresh; token precisa identificar usuário e ter validade | Verificador aceitava ausência de claims / D | JWT e HTTP |
| Tipo ausente, desconhecido ou trocado | Rejeitar antes de buscar usuário; finalidade aprovada | Verificador compartilhado / R | JWT e HTTP, incluindo username numérico |
| Refresh válido | Consultar por ID e emitir par verificável para o usuário encontrado | Sem cobertura / C | Serviço e HTTP |
| Refresh de usuário removido | 404 sem novos tokens; contrato documentado do endpoint | Sem cobertura / C | Serviço e HTTP |
| Subject de refresh não convertível, fora de Long, zero ou negativo | 400 sem consulta; entrada inválida não deve causar erro interno | Conversão podia lançar NumberFormatException / D | Serviço parametrizado; HTTP com subject não numérico |
| Usuário desativado após emissão | 401 tanto na renovação quanto no acesso | Sem verificação de ativo / R | Serviço e HTTP com tokens emitidos antes da desativação |
| Reutilização do refresh | Sucesso até expirar, usuário existente/ativo; sem exigir strings novas | Sem cobertura / R | HTTP, duas renovações verificadas |
| Falha na emissão | Interromper fluxo e não retornar resposta parcial | Sem cobertura / C | Serviço, falhas na geração de acesso e de refresh |
| Falha interna no provider | Propagar, sem tratar como senha incorreta ou emitir tokens | Sem cobertura / C | Serviço; não redefine status global de infraestrutura |
| Sem token em rota protegida | Negar acesso com 403, preservando configuração existente | Sem cobertura / C | HTTP |
| Sem token em login/refresh e rotas públicas | Permitir chegar ao endpoint conforme SecurityConfig | Sem cobertura / C | HTTP; sentinelas para saúde/Swagger/OpenAPI |
| Bearer válido | Usar usuário atual e permissões do repositório, sem guardar senha no Authentication | Sem cobertura / C | HTTP e filtro unitário |
| Authorization com esquema inválido, vazio ou token com espaços | 401 em JSON; contrato Bearer documentado | replace aceitava cabeçalhos incorretos / D | HTTP parametrizado; esquema aceita variação de maiúsculas |
| JWT inválido ou usuário removido no filtro | 401 no formato da API; não continuar requisição | Exceção fora do handler MVC / D | HTTP e filtro unitário |
| Falha no filtro com contexto preexistente | Limpar Authentication; não executar cadeia seguinte | Sem cobertura / D | Filtro unitário |
| Credencial inválida explícita em rota pública | 401; permitAll dispensa credencial, não ignora credencial enviada | Falha do filtro sem contrato / D | HTTP no login |
| Requisição seguinte sem token | Não herdar autenticação da requisição anterior; stateless | Sem cobertura / C | HTTP |
| Funcionário e administrador | Funcionário sem privilégio admin; admin herda funcionário | Sem cobertura / C | HTTP com permissões vindas do usuário |
| Erro de negócio após filtro | Não converter erro de outro domínio em falha de autenticação | Proteção da correção / D | Filtro unitário |
| Inicialização não web | Dispensar filtro/controller/serviço web e dependências MVC | Proteção do bootstrap existente / C | Contexto não web; não provisiona administrador real |

## Implementação e compatibilidade

- JWTs recebem claim `tipo=acesso` ou `tipo=refresh`. `verificaToken` verifica acesso e
  `verificaRefreshToken` verifica renovação. Ambos exigem subject não vazio e expiração.
- `Usuario.isEnabled()` reflete `ativo`. Renovação e filtro verificam a situação atual do usuário.
- Refresh com identificação inválida gera BusinessException (400); usuário removido mantém 404;
  usuário inativo retorna 401. Login inativo usa a mesma mensagem de credenciais inválidas.
- O filtro encaminha falhas de credencial ao handler MVC com InvalidCredentialsException (401),
  limpa o contexto e interrompe a cadeia. Erros posteriores da cadeia não entram nesse tratamento.
  Filtro é carregado somente em aplicação web Servlet para preservar o bootstrap não web.
- Tokens anteriores sem `tipo` deixam de ser aceitos: é necessário fazer login novamente.
  Clientes continuam recebendo `tokenAcesso`/`refreshToken`; nenhuma migração de banco é necessária.
- Não há identificador aleatório por emissão nem registro de revogação. Tokens gerados no mesmo segundo
  podem ser idênticos. Desativar usuário bloqueia seu uso, sem revogar criptograficamente o JWT.

## Validação e limites

Suítes: `AutenticacaoServiceTest`, `TokenServiceTest`, `AutenticacaoHttpTest`, `FiltroTokenAcessoTest`
e `AutenticacaoContextTest`, em `backend/src/test/java/br/com/salgadosdalucia/api/autenticacao`.
MockMvc executa MVC, Bean Validation, handler, provider BCrypt, JWT e cadeia de segurança reais;
somente repositórios são simulados. Sentinelas de URL/permissão testam SecurityConfig e segurança de
métodos, não implementações de Swagger/Actuator nem permissões de todos os controllers do sistema.

Não se comprova aqui persistência MySQL, busca case-insensitive real, migrações Flyway, concorrência,
containers, Nginx, navegador ou provisionamento real do administrador. O teste do provider usa uma
resposta simulada do repositório para username em maiúsculas; isso não prova collation do banco.
Não se força falha artificial do assinador JWT com usuário inconsistente só para cobrir catch.

Comando de verificação do backend: `.\backend\mvnw.cmd -f backend/pom.xml verify`.
A primeira execução de testes no sandbox ficou bloqueada na anexação do agente Java do Mockito.
Após ajustar as referências dos testes ao verificador separado de refresh, a verificação completa fora
do sandbox terminou em 19/11/2026 às 14:52 (America/Sao_Paulo), com BUILD SUCCESS: 272 testes,
nenhuma falha, erro ou teste ignorado; 100 cenários são das cinco suítes de autenticação. O pacote do
backend também foi gerado. `git diff --check` passou após remover uma linha em branco no fim do teste.

## Documentação externa

O README foi atualizado localmente. Foram consultadas as páginas 8 (API e Segurança) e 9 (Testes e
Rastreabilidade) da wiki, após fetch do clone em `.tools/wiki-endereco-review`. A wiki não foi publicada
nem alterada nesta tarefa. Na página 8, a descrição de isEnabled, ausência de finalidade e falhas do
filtro precisa incorporar as regras aprovadas acima quando houver publicação. A referência antiga a
segredo literal/fuso fixo também está desatualizada em relação ao código base. Na página 9, a seção de
autenticação deve referenciar esta matriz e os limites de MockMvc, sem declarar integração MySQL validada.
