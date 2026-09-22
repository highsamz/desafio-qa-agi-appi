# Decisões técnicas e trade-offs

Registro das escolhas de stack e arquitetura deste projeto, com os trade-offs
considerados em cada ponto. O objetivo é deixar explícito *por que* cada decisão
foi tomada, não apenas *o que* foi usado.

---

## 1. Transporte HTTP: Rest Assured (vs. Playwright)

**Decisão:** Rest Assured.

**Contexto:** a opção inicial era Playwright, ferramenta usada no dia a dia.

**Trade-offs:**

- **Playwright em Java** cobre chamadas de API via `APIRequestContext`, mas:
    - Não traz *runner* próprio em Java (diferente do TS) ainda dependeria de
      JUnit/TestNG por baixo.
    - Seus diferenciais reais (auto-wait, browser, tracing) não se aplicam a teste
      de API pura, paga-se a complexidade sem usar o benefício.
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