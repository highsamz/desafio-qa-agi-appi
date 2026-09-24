# Decisões técnicas e trade-offs

Registro das escolhas de stack e arquitetura deste projeto, com os trade-offs
considerados em cada ponto. O objetivo é deixar explícito *por que* cada decisão
foi tomada — não apenas *o que* foi usado.

---

## 1. Transporte HTTP: Rest Assured (vs. Playwright)

**Decisão:** Rest Assured.

**Contexto:** a opção inicial era Playwright, ferramenta usada no dia a dia.

**Trade-offs:**

- **Playwright em Java** cobre chamadas de API via `APIRequestContext`, mas:
  - Não traz *runner* próprio em Java (diferente do TS) — ainda dependeria de
    JUnit/TestNG por baixo.
  - Seus diferenciais reais (auto-wait, browser, tracing) não se aplicam a teste
    de API pura — paga-se a complexidade sem usar o benefício.
  - Exigiria reconstruir à mão captura de response, parsing de body e validação
    de schema.
- **Rest Assured** é o padrão de mercado para teste de API em Java:
  - DSL fluente (`given/when/then`) feita para o caso de uso.
  - Validação de JSON (JsonPath) e de contrato (`matchesJsonSchema`) nativas.
  - Reconhecimento imediato pelo avaliador; menos código de infraestrutura.

**Conclusão:** o que dá identidade à automação é a **arquitetura em camadas**, não
o transporte. Rest Assured entrega o mesmo shape com menos encanamento.

---

## 2. Framework de teste: JUnit 5

**Decisão:** JUnit 5 (Jupiter).

**Trade-offs:**

- **TestNG** tem *data providers* nativos fortes para parametrização.
- **JUnit 5** é mais moderno, integra nativamente com a *Extension API* (usada
  pelo relatório via `TestWatcher`) e é o mais reconhecido no ecossistema atual.

**Conclusão:** JUnit 5 pela integração limpa com a extension de relatório e pela
parametrização (`@ParameterizedTest`) ser suficiente para os cenários da Dog API.

---

## 3. Build: Maven

**Decisão:** Maven.

**Trade-offs:**

- Portabilidade e execução triviais na máquina do avaliador (`mvn test`), em
  qualquer SO — requisito explícito do desafio.
- Surefire com `reuseForks=true` mantém a JVM viva até o *shutdown hook* que
  dispara a geração do relatório.

---

## 4. Relatório: implementação própria (vs. Allure)

**Decisão:** relatório próprio, com dois *writers* (Markdown + PDF).

**Trade-offs:**

- **Allure** entrega relatório rico "de graça", porém padronizado e menos
  personalizável.
- **Relatório próprio** exige mais código, mas:
  - Controle total sobre visual e conteúdo.
  - Arquitetura de *writer* plugável (`ExecutionReportWriter`) — fácil adicionar
    novos formatos.
  - Coleta confiável via *Extension API* do JUnit (`TestWatcher`), não parsing
    de log.
- O **JUnit XML / Surefire** segue rodando junto: é o formato que o CI entende
  nativamente. O relatório próprio é a camada de apresentação por cima —
  redundância barata e segura.

---

## 5. Biblioteca de PDF: iText 7 (vs. OpenPDF)

**Decisão:** iText 7.

**Trade-offs:**

- **iText 7** é AGPL. Para repositório público é aceitável (copyleft aberto), e
  mantém o código do writer de PDF sem alterações.
- **OpenPDF** é LGPL (mais permissivo para uso comercial), *drop-in* quase igual,
  porém exigiria troca de imports (`com.itextpdf` → `com.lowagie`).

**Conclusão:** iText 7 por ora, por reaproveitar o writer existente. Migração para
OpenPDF é de baixo custo caso a licença se torne uma restrição.

---

## 6. Arquitetura: camadas sobre Service Object Pattern

**Decisão:** arquitetura em camadas — **client → flow → validator → cenários**.

**Nomenclatura:**

- **Client** — camada de transporte/acesso, um objeto por serviço/endpoint
  (*Service Object Pattern*, equivalente ao Page Object Model para APIs).
- **Flow** — orquestração e pré-condições (encadeia chamadas de client).
- **Validator** — asserts reutilizáveis e isolados (*Verification layer*).
- **Cenários** — as classes de teste, lendo como narrativa.

**Trade-off:**

- Numa API simples e *stateless* como a Dog API, a camada **flow** pode ficar
  "anêmica". Justifica-se com casos reais de encadeamento (ex.: obter uma raça da
  lista → buscar imagens dela), e a separação mantém o padrão consistente com
  projetos maiores.

---

## 7. Java 21

**Decisão:** Java 21 (LTS).

**Trade-off:** *records*, *switch expressions* e recursos modernos usados no
relatório rodam sem workaround; LTS garante disponibilidade na máquina do
avaliador.

---

## 8. Modelagem dos DTOs: um por endpoint (vs. envelope genérico)

**Decisão:** um DTO por endpoint (`BreedListResponse`, `BreedImagesResponse`,
`RandomImageResponse`).

**Trade-off:**

- **Envelope genérico** `DogApiResponse<T>` seria mais DRY, mas generics + Jackson
  exigem `TypeReference` e adicionam cerimônia que a API não pede.
- **Um por endpoint** é mais explícito e type-safe: cada validator casa com um DTO,
  e o avaliador entende o contrato na hora.

**Detalhe de erro:** no erro, `message` é sempre uma *string*. Isso só encaixa no
`RandomImageResponse` (cujo `message` já é `String`). Para `BreedImagesResponse`
(`List`) e `BreedListResponse` (`Map`), o corpo de erro **não** desserializa no DTO
de sucesso — por isso os cenários negativos validam no `Response` cru (status HTTP +
JsonPath `status`/`message`/`code`), não via DTO.

---

## 9. Camada flow retorna `Response` cru (vs. DTO)

**Decisão:** os métodos de flow devolvem a `Response` do Rest Assured; a
desserialização para DTO é feita via `response.as(...)` na camada de cenário.

**Trade-off:**

- Retornar **DTO direto** esconderia status HTTP, content-type e o corpo de erro —
  justamente o que os validators de contrato/negativo precisam.
- Retornar **`Response`** preserva a verdade de transporte e evita chamada HTTP
  duplicada (`.as()` opera sobre a resposta já obtida). O valor do flow passa a ser
  a **orquestração** (ex.: `imagesForFirstBreed` encadeia lista → escolha → imagens),
  não o encapsulamento do transporte.

---

## 10. JSON Schema em draft-04

**Decisão:** schemas em draft-04.

**Trade-off:** o validador padrão do Rest Assured (`json-schema-validator`) é baseado
na lib da fge, com suporte pleno a draft-04. Draft-07 traria recursos como `format`
mas com risco de não ser validado. Regras que o schema não cobre bem (formato de URL)
ficam nos validators em código, não no schema.

---

## 11. Validação de URL de imagem e cenários instáveis

**Decisão:** validar formato (https + host `images.dog.ceo` + extensão) sempre, e
acessibilidade real (HEAD 200) num grupo isolado por tag `@external`.

**Trade-off:** o HEAD testa o CDN, não a API — adiciona dependência de rede e
possível flakiness. Isolar por tag mantém a suíte core determinística e permite rodar
o grupo externo à parte. Cenários potencialmente instáveis (aleatoriedade, contagem
por sub-raça) também recebem tags para não contaminar o core.

---

## 12. Auto-registro da extension de relatório (vs. `@ExtendWith`)

**Decisão:** registrar a extension via `META-INF/services` +
`junit.jupiter.extensions.autodetection.enabled=true`.

**Trade-off:**

- **`@ExtendWith` na classe base** é mais explícito e fácil de justificar.
- **Auto-detecção** mantém os cenários e a base limpos (nenhuma anotação de report),
  ao custo de um comportamento menos óbvio, documentado no README para compensar.

---

## 13. Report no mesmo módulo (vs. biblioteca extraída)

**Decisão:** manter a camada de relatório dentro do próprio projeto, não como
biblioteca separada.

**Trade-off:**

- **Extrair para lib** (multi-repo ou multi-módulo) daria reuso real entre projetos,
  mas adiciona fricção ao avaliador: exigiria buildar/instalar a lib antes de rodar
  os testes, quebrando o fluxo `git clone` + `mvn test` que o desafio pede.
- **Manter no módulo** preserva a execução trivial e deixa o código do report visível
  no repositório principal.

**Nota de design:** a camada de report é agnóstica de domínio (não depende de Dog API
nem do transporte) e está projetada para extração futura em biblioteca compartilhada,
caso o reuso se justifique fora do contexto do desafio.

---

## 14. Organização dos cenários: @Nested + @Tag

**Decisão:** cenários agrupados por `@Nested` (Contrato / Caminho feliz / Negativos /
etc.) e classificados por `@Tag`.

**Trade-off:**

- `@Nested` dá leitura em árvore no output e no relatório (agrupado por classe
  top-level), ao custo de mais aninhamento.
- `@Tag` (`api`, por endpoint, `external`, `unstable`) permite isolar o núcleo
  determinístico dos testes potencialmente instáveis. O core roda sozinho com
  `mvn test -DexcludedGroups=external,unstable`, mantendo o pipeline confiável sem
  abrir mão da cobertura extra quando desejada.

---

## 15. Observação de comportamento: case do nome da raça

**Achado:** a documentação sugere que o nome da raça seria case-sensitive, mas na
prática a Dog API **normaliza o case na entrada** `Hound` resolve igual a `hound`
(retorna 200 com imagens), não 404.

**Decisão:** em vez de descartar o cenário, ele foi ajustado para **documentar o
comportamento real** via teste explícito (case-insensitive). O caso negativo de raça
inexistente continua coberto por um input de fato inválido (`notabreed` → 404).

**Racional:** divergência entre documentação e comportamento observado é um achado de
QA legítimo; registrá-la como teste é mais valioso do que ocultá-la removendo o caso.


---

## 16. Achado: contrato de erro não uniforme em rotas não mapeadas

**Achado:** o envelope de erro (`status`/`message`/`code` em JSON) só é respeitado nas
rotas mapeadas. Em rotas inválidas `/breed/{breed}/images/` (barra final) ou
`/breeds/list/all/{extra}` a API responde 404 **fora do próprio contrato**: sem
`Content-Type: application/json` e sem os campos do envelope.

**Decisão:** cobrir por testes explícitos (`assertNotJsonEnvelope`) que documentam a
divergência, em vez de assumir o envelope como universal.

**Racional:** consistência de contrato de erro é um atributo de qualidade relevante para
quem consome a API, um cliente que trate todo erro esperando `status`/`message` quebra
nessas rotas. Registrar isso é justamente o papel do QA.

---
17. Resiliência: timeouts e separação do pipeline
    Decisão: timeouts explícitos (5s de conexão, 10s de resposta) na RequestSpecBuilder e pipeline dividido em dois jobs.

Trade-off:

Sem timeout, uma API que não responde deixa o build pendurado até o limite do runner, feedback lento e custo desnecessário.
Rodar tudo de forma bloqueante deixaria o pipeline refém das fontes de instabilidade internas à suíte (aleatoriedade do endpoint random, disponibilidade do CDN de imagens). Rodar só o core perderia cobertura.
A divisão resolve os dois lados: o job core (bloqueante) exclui external e unstable; o job full roda a suíte completa com continue-on-error, mantendo a informação sem transformar flakiness em build vermelho.
Escopo do que isso resolve, e do que não resolve. A separação trata apenas a instabilidade dos grupos marcados. Ela não torna o pipeline tolerante a uma queda da Dog API: todo teste, inclusive os do core, consome a API pública, então um outage deixa o core vermelho. O timeout também não muda isso, ele faz a falha ser rápida e legível, não tolerada. Resiliência real a indisponibilidade exigiria retry com backoff, ou substituir a API por um mock/contract test, o que descaracterizaria a proposta de testar a integração real.

Atualização do achado nº 15: o teste de case foi reescrito para asserir o comportamento real (200 + mesmo conjunto de imagens de hound) e está ativo, não mais @Disabled.

18. Correção: truncamento de mensagens de erro no relatório
    Problema identificado em revisão: a coleta guardava apenas a primeira linha da mensagem de falha. Para asserts simples isso bastava, mas falhas de JSON Schema trazem uma primeira linha genérica e o detalhe (qual campo, qual tipo esperado) nas linhas seguintes, ou seja, o relatório exibia exatamente a parte inútil e descartava o diagnóstico.

Correção: a responsabilidade pelo truncamento passou da coleta para a apresentação:

a extension armazena a mensagem completa (fullMessageOf);
TestExecutionRecord ganhou errorHeadline() (primeira linha) e hasMultilineError();
as tabelas (MD e PDF) usam a headline, que é o que cabe numa célula;
a seção de falhas imprime a mensagem na íntegra — em bloco de código no Markdown e em parágrafos dedicados no PDF.
Lição: truncar no ponto de coleta destrói informação irrecuperável. O corte deve ocorrer o mais tarde possível, onde se conhece a restrição de espaço.