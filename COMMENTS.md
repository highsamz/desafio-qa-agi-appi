# Decisões técnicas e trade-offs

Aqui ficam registradas as escolhas de stack e de arquitetura do projeto, com o que foi
pesado em cada uma. A ideia é deixar claro *por que* cada coisa foi feita, não só *o que*
foi usado.

---

## 1. HTTP: Rest Assured (em vez de Playwright)

**Decisão:** Rest Assured.

**Contexto:** minha primeira ideia foi Playwright, que é o que uso no dia a dia.

**O que pesou:**

- **Playwright em Java** faz chamada de API com `APIRequestContext`, mas:
  - não tem runner próprio em Java (diferente do TS), então ainda dependeria de
    JUnit ou TestNG por baixo;
  - o que ele tem de bom (auto-wait, browser, tracing) não serve para teste de API,
    então eu pagaria a complexidade sem usar o benefício;
  - eu teria que montar na mão captura de response, parse de body e validação de schema.
- **Rest Assured** é o padrão de mercado para API em Java:
  - DSL `given/when/then` feita para esse caso;
  - JsonPath e `matchesJsonSchema` já vêm na biblioteca;
  - qualquer avaliador reconhece na hora, e sobra menos código de infra para escrever.

**Conclusão:** o que dá identidade para a automação é a arquitetura em camadas, não a
biblioteca de HTTP. Rest Assured entrega o mesmo resultado com menos código de apoio.

---

## 2. Framework de teste: JUnit 5

**Decisão:** JUnit 5 (Jupiter).

**O que pesou:**

- **TestNG** tem data provider nativo mais forte para parametrização.
- **JUnit 5** é mais atual, tem a Extension API que o relatório usa (`TestWatcher`) e é
  o mais conhecido hoje.

**Conclusão:** JUnit 5, porque a extension de relatório encaixa sem gambiarra e
`@ParameterizedTest` já dá conta do que a Dog API pede.

---

## 3. Build: Maven

**Decisão:** Maven.

**O que pesou:**

- Roda em qualquer sistema com um `mvn test`, que é o que o desafio pede.
- O Surefire com `reuseForks=true` mantém a JVM viva até o shutdown hook que gera o
  relatório.

---

## 4. Relatório: feito do zero (em vez de Allure)

**Decisão:** relatório próprio, com dois writers (Markdown e PDF).

**O que pesou:**

- **Allure** dá um relatório bonito de graça, mas é padronizado e pouco personalizável.
- **Relatório próprio** dá mais trabalho, mas:
  - controle total do que aparece e de como aparece;
  - writer plugável (`ExecutionReportWriter`), então adicionar outro formato é fácil;
  - a coleta vem da Extension API do JUnit, não de leitura de log.
- O **XML do Surefire** continua sendo gerado do mesmo jeito, que é o formato que o CI
  entende. O relatório próprio é só a camada de apresentação em cima disso.

---

## 5. PDF: iText 7 (em vez de OpenPDF)

**Decisão:** iText 7.

**O que pesou:**

- **iText 7** é AGPL. Para repositório público está tudo bem, e o writer de PDF fica do
  jeito que já está.
- **OpenPDF** é LGPL, mais tranquilo para uso comercial, e a troca é quase direta, mas
  exigiria mudar os imports (`com.itextpdf` → `com.lowagie`).

**Conclusão:** fico com iText 7 por reaproveitar o writer. Se a licença virar problema,
a troca é rápida.

---

## 6. Arquitetura em camadas (Service Object Pattern)

**Decisão:** **client → flow → validator → cenários**.

**O que cada camada faz:**

- **Client** — só transporte, um objeto por serviço. É o Service Object Pattern, que é o
  Page Object do mundo de API.
- **Flow** — orquestra e monta pré-condição, encadeando chamadas do client.
- **Validator** — asserts reaproveitáveis e separados dos testes.
- **Cenários** — as classes de teste, que devem ler como narrativa.

**O que pesou:**

- Numa API simples e sem estado como a Dog API, a camada de **flow** fica meio vazia.
  Ela se justifica em casos de encadeamento (pegar uma raça da lista e buscar as imagens
  dela) e mantém o padrão igual ao de projetos maiores.
- **Custo que eu assumo:** parte dessa infra foi escrita antes dos testes que iam usar
  ela, então alguns métodos ainda estão sem teste. A lista completa está no item 19 e no
  README, em vez de ficar escondida no código.

---

## 7. Java 21

**Decisão:** Java 21 (LTS).

**O que pesou:** records e switch expression, que o relatório usa, funcionam sem
contorno. Sendo LTS, é fácil de ter instalado na máquina de quem for rodar.

---

## 8. DTO: um por endpoint (em vez de um envelope genérico)

**Decisão:** um DTO por endpoint (`BreedListResponse`, `BreedImagesResponse`,
`RandomImageResponse`).

**O que pesou:**

- Um **envelope genérico** `DogApiResponse<T>` repetiria menos código, mas generics com
  Jackson pede `TypeReference` e deixa tudo mais cheio de detalhe sem necessidade.
- **Um por endpoint** é mais direto e mais seguro no tipo: cada validator combina com um
  DTO e o contrato fica óbvio de ler.

**Detalhe do erro:** no erro, `message` é sempre string. Isso só combina com
`RandomImageResponse`, onde `message` já é `String`. Em `BreedImagesResponse` (lista) e
`BreedListResponse` (mapa), o corpo de erro **não** cabe no DTO de sucesso. Por isso os
cenários negativos validam na `Response` crua (status HTTP mais JsonPath em
`status`/`message`/`code`), e não pelo DTO.

---

## 9. Flow devolve `Response` crua (em vez de DTO)

**Decisão:** os métodos de flow devolvem a `Response` do Rest Assured. Quem converte
para DTO é o cenário, com `response.as(...)`.

**O que pesou:**

- Se o flow devolvesse **DTO direto**, eu perderia status HTTP, content-type e corpo de
  erro, que é justamente o que os validators de contrato e de negativo precisam.
- Devolver **`Response`** mantém a verdade do que veio na rede e não faz chamada HTTP
  duplicada, porque o `.as()` trabalha em cima da resposta que já chegou. O valor do flow
  passa a ser a orquestração, não esconder o transporte.

**O que ainda não está usado:** o exemplo mais claro dessa orquestração,
`firstBreed()` e `imagesForFirstBreed()`, que encadeiam lista → escolha → imagens, está
implementado mas **nenhum cenário chama**. Os testes atuais usam raça fixa (`hound`,
`affenpinscher`), que é mais estável para os asserts de sub-raça. O encadeamento fica
pronto para cenários data-driven depois (item 19).

---

## 10. JSON Schema em draft-04

**Decisão:** schemas em draft-04.

**O que pesou:** o `json-schema-validator` do Rest Assured usa a lib da fge, que suporta
draft-04 inteiro. Draft-07 traria coisas como `format`, mas com risco de não ser
validado de verdade. Regra que o schema não cobre bem, como formato de URL, fica no
validator em código.

---

## 11. URL de imagem e testes instáveis

**Decisão:** validar formato sempre (https, host `images.dog.ceo`, extensão de imagem) e
deixar a acessibilidade de verdade (HEAD 200) em um grupo separado com a tag `@external`.

**O que pesou:** o HEAD testa o CDN, não a API. Isso adiciona dependência de rede e pode
falhar sem ter bug nenhum. Separando por tag, o core continua estável e ainda dá para
rodar o grupo externo quando eu quiser. Os testes que podem oscilar (aleatoriedade e
contagem por sub-raça) também recebem tag, para não atrapalhar o core.

---

## 12. Extension de relatório registrada sozinha (em vez de `@ExtendWith`)

**Decisão:** registrar por `META-INF/services` mais
`junit.jupiter.extensions.autodetection.enabled=true`.

**O que pesou:**

- **`@ExtendWith` na classe base** é mais explícito e mais fácil de explicar.
- **Auto-detecção** deixa os cenários e a base limpos, sem anotação de relatório em
  lugar nenhum. O custo é ficar menos óbvio, e foi por isso que documentei no README.

---

## 13. Relatório no mesmo projeto (em vez de biblioteca separada)

**Decisão:** manter a camada de relatório dentro do projeto.

**O que pesou:**

- **Separar em lib** daria reuso de verdade entre projetos, mas quem fosse rodar teria
  que buildar e instalar a lib primeiro, e isso quebra o `git clone` + `mvn test` que o
  desafio pede.
- **Manter junto** mantém a execução simples e deixa o código do relatório visível no
  repositório principal.

**Observação de design:** a camada de relatório não sabe nada de Dog API nem do
transporte. Ela já está pronta para ser extraída em lib depois, se o reuso justificar.

---

## 14. Organização: `@Nested` e `@Tag`

**Decisão:** agrupar por `@Nested` (Contrato, Caminho feliz, Negativos) e classificar por
`@Tag`.

**O que pesou:**

- `@Nested` dá leitura em árvore no output e no relatório, ao custo de mais aninhamento.
- `@Tag` (`api`, uma por endpoint, `external`, `unstable`) deixa separar o que é estável
  do que pode oscilar. O core roda sozinho com
  `mvn test -DexcludedGroups=external,unstable`, que são 14 dos 18 cenários, e eu não
  perco a cobertura extra quando quiser rodar tudo.

---

## 15. Achado: a API normaliza o case do nome da raça

**Achado:** a documentação dá a entender que o nome da raça é case-sensitive, mas na
prática a Dog API **aceita qualquer case**. Testei contra produção:

```
GET /api/breed/Hound/images  → 200, application/json, mesmas imagens de hound
```

Não existe 404 por causa de maiúscula.

**Decisão:** deixei o cenário `breedNameIsCaseSensitive` no código com **`@Disabled`** e
a explicação do achado escrita na própria anotação. A outra opção era apagar o teste, e
aí a divergência desapareceria do repositório.

**Como está hoje:** o teste desabilitado *registra* o achado, mas não *verifica* nada. O
certo é reescrever ele para asserir o comportamento real (200 e o mesmo conjunto de
imagens de `hound`), e assim virar uma regressão que a suíte pega. Isso está aberto
(item 19). Enquanto não fizer, a suíte reporta 18 cenários com 1 ignorado, e o relatório
mostra isso na cara.

**Por que isso importa:** documentação que não bate com o comportamento é achado de QA
legítimo. E a cobertura do caminho de erro não depende desse teste, porque raça
inexistente (`notabreed` → 404) já tem cenário ativo.

---

## 16. Achado: o contrato de erro não é o mesmo em toda rota

**Achado:** o envelope de erro (`status`/`message`/`code` em JSON) só vale nas rotas que
a API conhece. Testei contra produção:

| Rota | HTTP | `Content-Type` | Envelope |
|---|---|---|---|
| `/api/breed/hound/images/` (barra no fim) | 404 | *(não vem)* | ❌ |
| `/api/breeds/list/all/extra` | 404 | *(não vem)* | ❌ |
| `/api/breed/notabreed/images` | 404 | `application/json` | ✅ `main breed does not exist` |
| `/api/breed/hound/notasub/images` | 404 | `application/json` | ✅ `sub breed does not exist` |

Nas duas primeiras a resposta sai fora do próprio contrato: sem `Content-Type` e sem os
campos do envelope.

**Por que isso importa:** quem consome a API e trata todo erro esperando
`status`/`message` quebra nessas rotas. Apontar isso é papel do QA.

**Como está hoje:** a infra para cobrir esses casos existe e funciona
(`DogApiClient.getBreedImagesTrailingSlash`, `getAllBreedsWithExtraSegment`,
`BreedFlow.invalidBreedImagesRoute`, `listAllBreedsWithExtraSegment` e
`ResponseValidator.assertNotJsonEnvelope`), mas **os cenários que usam ela ainda não
foram escritos**. Ou seja: achado testado na mão, não automatizado. O mesmo vale para a
diferença entre a mensagem de raça e de sub-raça, e para o
`schemas/error-schema.json`, que existe mas nenhum teste usa (hoje o corpo de erro é
validado campo por campo no `ResponseValidator.assertError`).

---

## 17. Timeout e divisão do pipeline

**Decisão:** timeout explícito no `RequestSpecBuilder` (5s de conexão, 10s de resposta) e
pipeline dividido em dois jobs.

**O que pesou:**

- Sem timeout, uma API que não responde deixa o build pendurado até o limite do runner,
  com feedback lento e custo desnecessário.
- Rodar tudo de forma bloqueante deixaria o pipeline dependente do que pode oscilar
  (aleatoriedade do random e disponibilidade do CDN de imagens). Rodar só o core perderia
  cobertura.
- A divisão resolve os dois lados: o job `core` é bloqueante e exclui `external` e
  `unstable`; o job `full` roda tudo com `continue-on-error`, então eu mantenho a
  informação sem transformar oscilação em build vermelho.

**O que isso resolve e o que não resolve:** a divisão trata só a instabilidade dos grupos
marcados. Ela **não** deixa o pipeline tolerante a uma queda da Dog API: todo teste,
inclusive os do `core`, bate na API pública, então se ela cair o `core` fica vermelho. O
timeout também não muda isso, ele só faz a falha acontecer rápido e com erro claro.
Tolerar queda de verdade pediria retry com espera, ou trocar a API por um mock, e aí eu
não estaria mais testando a integração real, que é a proposta.

---

## 18. Correção: mensagem de erro cortada no relatório

**Problema que apareceu na revisão:** a coleta guardava só a primeira linha da mensagem
de falha. Para assert simples isso bastava, mas falha de JSON Schema tem a primeira linha
genérica e o detalhe (qual campo, qual tipo esperado) nas linhas seguintes. Resultado: o
relatório mostrava justo a parte inútil e jogava fora o diagnóstico.

**Correção:** o corte saiu da coleta e foi para a apresentação:

- a extension guarda a mensagem inteira (`fullMessageOf`);
- `TestExecutionRecord` ganhou `errorHeadline()` (primeira linha) e `hasMultilineError()`;
- as tabelas (MD e PDF) usam a headline, que é o que cabe numa célula;
- a seção de falhas imprime a mensagem inteira, em bloco de código no Markdown e em
  parágrafos no PDF.

**O que eu tiro disso:** cortar na hora de coletar apaga informação que não volta. O
corte tem que ser o mais tarde possível, onde já se sabe quanto espaço existe.

---

## 19. O que ainda falta (lista aberta)

Essa seção existe porque uma revisão do projeto achou **documentação descrevendo teste
que não existia no código**. O README e o COMMENTS diziam que as rotas não mapeadas
estavam cobertas e que o teste de case estava ativo, e nenhuma das duas coisas era
verdade. Corrigi nos itens 15 e 16, e agora essa lista é o lugar único para saber o que a
suíte realmente cobre.

| Item | Como está |
|---|---|
| Cenários de rota não mapeada (barra no fim, segmento extra) | Client, flow e validator prontos, **teste falta** |
| `schemas/error-schema.json` | Criado, **nenhum teste usa** |
| Sub-raça inexistente com mensagem diferente | Confirmado na mão, **teste falta** |
| `breedNameIsCaseSensitive` | `@Disabled`, falta reescrever para asserir o comportamento real (item 15) |
| `BreedFlow.firstBreed` e `imagesForFirstBreed` | Implementados, nenhum cenário usa (item 9) |
| `ImageValidator.validateAll` | Implementado, sem uso |
| `reportInfo(...)` em teste que passa | Vai para o `ExecutionRegistry`, mas os writers só mostram `extras` no bloco de falha, então o dado dos testes verdes não aparece |
| Tolerância a queda da API | Não fiz, por decisão de escopo (item 17) |

**Por que deixar isso escrito:** código sem uso e documentação otimista são duas dívidas,
e a segunda é pior, porque faz quem está revisando confiar em cobertura que não existe.
Escrever o que falta não custa nada. Esconder custa a confiança inteira.

**O que eu aprendi no processo:** eu commitei a infra dos cenários negativos antes dos
cenários (`3e4ce69` → `8292165`) e escrevi a documentação descrevendo a intenção como se
já fosse fato. A ordem certa é o contrário: escrever o teste, ver falhar, fazer passar e
só depois documentar o que existe.