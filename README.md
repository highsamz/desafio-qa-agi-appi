# 🐶 Dog API — Automação de Testes

[![CI](https://github.com/highsamz/desafio-qa-agi-appi/actions/workflows/ci.yml/badge.svg)](https://github.com/highsamz/desafio-qa-agi-appi/actions/workflows/ci.yml)

Automação de testes de API para a [Dog API](https://dog.ceo/dog-api/documentation),
validando resposta, contrato (formato) e comportamento dos endpoints em diferentes
cenários — caminho feliz, negativos e casos de borda.

---

## 📑 Sumário

- [Objetivo](#-objetivo)
- [Stack](#-stack)
- [Arquitetura](#-arquitetura)
- [Estrutura de pastas](#-estrutura-de-pastas)
- [Pré-requisitos](#-pré-requisitos)
- [Como executar](#-como-executar)
- [Endpoints e cenários](#-endpoints-e-cenários)
- [Relatórios](#-relatórios)
- [Integração contínua (CI)](#-integração-contínua-ci)
- [Decisões técnicas](#-decisões-técnicas)
- [Achados de QA](#-achados-de-qa)
- [Débito técnico declarado](#-débito-técnico-declarado)
- [Autor](#-autor)

---

## 🎯 Objetivo

Garantir a qualidade da integração com a Dog API por meio de testes automatizados que
verificam:

- que a API responde corretamente (status HTTP e envelope `status`/`message`);
- que os dados retornados estão no **formato esperado** (validação por JSON Schema e
  por tipagem via DTOs);
- que a aplicação se comporta como esperado em **diferentes cenários** (raças válidas,
  sub-raças, raças inexistentes, aleatoriedade, acessibilidade de imagem).

---

## 🧰 Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| Build | Maven |
| Testes | JUnit 5 (Jupiter) |
| Cliente HTTP / asserts de API | Rest Assured |
| Validação de contrato | JSON Schema (`json-schema-validator`) |
| Desserialização | Jackson |
| Relatório | Implementação própria — Markdown + PDF (iText 7) |
| CI | GitHub Actions |

> O detalhamento das escolhas e trade-offs está em **[COMMENTS.md](./COMMENTS.md)**.

---

## 🏗 Arquitetura

Arquitetura em **camadas** baseada no *Service Object Pattern* (o equivalente do Page
Object Model para APIs), separando responsabilidades:

```
Cenário  →  Flow  →  Client  →  Dog API
   ↓
Validator  ←  DTO (Jackson)
```

- **Client** (`DogApiClient`) — camada de transporte. Encapsula as chamadas HTTP e
  devolve a `Response` crua, sem asserts.
- **Flow** (`BreedFlow`, `ImageFlow`) — orquestração e operações de negócio (ex.:
  encadear `listar raças → escolher uma → buscar imagens`).
- **Validator** (`ResponseValidator`, `BreedValidator`, `ImageValidator`) — asserts
  reutilizáveis e isolados (envelope, contrato, conteúdo, URL).
- **Model** (`BreedListResponse`, `BreedImagesResponse`, `RandomImageResponse`) — DTOs
  tipados (records) para o caminho de sucesso.
- **Cenários** (`*Tests`) — as classes de teste, que leem como narrativa: chamam o
  flow, passam o resultado ao validator.

A camada de **relatório** é agnóstica de domínio e coleta os resultados via *Extension
API* do JUnit (`TestWatcher`), registrada por auto-detecção (sem `@ExtendWith`).

---

## 📂 Estrutura de pastas

```
dog-api-test-automation/
├── pom.xml
├── COMMENTS.md                     # decisões técnicas e trade-offs
├── .github/workflows/ci.yml        # pipeline
└── src/test/
    ├── java/br/com/samuellima/qa/
    │   ├── client/                 # DogApiClient (Service Object)
    │   ├── flow/                   # BreedFlow, ImageFlow
    │   ├── validator/              # ResponseValidator, BreedValidator, ImageValidator
    │   ├── model/                  # DTOs (records)
    │   ├── support/                # BaseTest, RestAssuredConfig
    │   ├── report/                 # relatório (extension, publisher, writers, modelos)
    │   └── tests/                  # cenários: BreedsList, BreedImages, RandomImage
    └── resources/
        ├── junit-platform.properties
        ├── schemas/                # JSON Schemas dos endpoints
        └── META-INF/services/      # registro da extension de relatório
```

---

## ✅ Pré-requisitos

- **JDK 21+** (`java -version`)
- **Maven 3.9+** (`mvn -version`)
- Acesso à internet (os testes consomem a Dog API pública)

O projeto roda com qualquer Maven 3.9+ em Linux, Windows ou macOS.

---

## ▶️ Como executar

Clone e rode a suíte completa:

```bash
git clone https://github.com/highsamz/desafio-qa-agi-appi.git
cd desafio-qa-agi-appi
mvn test
```

Resultado esperado: **18 cenários** — 17 executados e 1 desabilitado
(ver [Achados de QA](#-achados-de-qa)).

### Execução seletiva por tag

Os cenários são classificados por `@Tag`, permitindo recortes:

```bash
# Apenas o núcleo determinístico (exclui instáveis e dependentes de rede externa)
mvn test -DexcludedGroups=external,unstable

# Apenas um endpoint
mvn test -Dgroups=breed-images

# Apenas uma classe
mvn test -Dtest=RandomImageTests
```

### Apontar para outra base URL

A base URL é sobrescrevível por propriedade de sistema (útil para mock/ambiente
alternativo), sem alterar código:

```bash
mvn test -Ddogapi.baseUri=https://minha-base-alternativa
```

### Tags disponíveis

| Tag | Significado |
|---|---|
| `api` | Todos os testes de API |
| `breeds-list` | Endpoint `/breeds/list/all` |
| `breed-images` | Endpoint `/breed/{breed}/images` |
| `random` | Endpoint `/breeds/image/random` |
| `external` | Depende de recurso externo (acessibilidade da imagem via HEAD) |
| `unstable` | Potencialmente instável (aleatoriedade, contagem por sub-raça) |

---

## 🔗 Endpoints e cenários

18 cenários no total. A tabela abaixo reflete exatamente o que existe em
`src/test/java/.../tests/`.

### `GET /breeds/list/all` — `BreedsListTests` (5)

| Grupo | Cenário |
|---|---|
| Contrato | Retorna 200, JSON e `status: success` |
| Contrato | Corpo adere ao JSON Schema |
| Conteúdo | Lista de raças íntegra e não vazia (todas as chaves e sub-raças validadas) |
| Conteúdo | Raça conhecida (`hound`) traz as sub-raças esperadas (`afghan`, `basset`) |
| Conteúdo | Raça sem sub-raça (`affenpinscher`) retorna lista vazia |

### `GET /breed/{breed}/images` — `BreedImagesTests` (8)

| Grupo | Cenário |
|---|---|
| Caminho feliz | Raça válida retorna lista não vazia de imagens |
| Caminho feliz | Corpo adere ao JSON Schema |
| Caminho feliz | Todas as URLs contêm o nome da raça |
| Caminho feliz | Raça sem sub-raça também retorna imagens |
| Caminho feliz | Sub-raça retorna imagens (`/breed/{breed}/{subBreed}/images`) |
| Caminho feliz | Raça principal combina imagens de múltiplas sub-raças — `@unstable` |
| Negativo | Raça inexistente (`notabreed`) → 404, `status: error`, `message` contendo *"not found"* |
| Negativo | Case do nome da raça — **`@Disabled`**, ver [Achados de QA](#-achados-de-qa) |

### `GET /breeds/image/random` — `RandomImageTests` (5)

| Grupo | Cenário |
|---|---|
| Contrato | Retorna 200, JSON e `status: success` |
| Contrato | Corpo adere ao JSON Schema |
| Conteúdo | `message` é uma URL de imagem válida (https + host + extensão) |
| Aleatoriedade | 5 chamadas retornam ao menos 2 URLs distintas — `@unstable` |
| Acessibilidade | A imagem retornada responde a um HEAD com 200 — `@external` |

O recorte determinístico (`-DexcludedGroups=external,unstable`) executa **14 cenários**.

---

## 📊 Relatórios

Ao final de cada execução, dois relatórios são gerados automaticamente em
**`target/dog-api-reports/`**:

| Arquivo | Descrição |
|---|---|
| `LAST_EXECUTION_REPORT.md` | Relatório em Markdown (resumo, falhas, execução por suíte) |
| `LAST_EXECUTION_REPORT.pdf` | Mesmo conteúdo em PDF, com formatação e cores |
| `DOG_API_EXECUTION_REPORT_<timestamp>.pdf` | Cópia versionada por data/hora |

O relatório traz total, aprovados/falhados/ignorados, taxa de sucesso, tempo total e,
para cada falha: suíte, classe, método, duração, a **mensagem de erro na íntegra**
(inclusive multi-linha, como nas falhas de JSON Schema) e um trecho de stack trace
filtrado, apontando a linha exata do cenário.

A coleta é feita via `TestWatcher` (JUnit 5) e a geração dispara no encerramento da
execução — sem parsing de log.

---

## 🔄 Integração contínua (CI)

O workflow em `.github/workflows/ci.yml` executa em **push**, **pull request** e
**manualmente** (`workflow_dispatch`), dividido em dois jobs:

| Job | Escopo | Bloqueante |
|---|---|---|
| `core` | `mvn -B test -DexcludedGroups=external,unstable` | ✅ Sim |
| `full` | `mvn -B test` (suíte completa) | ❌ Não (`continue-on-error`) |

O job **core** roda apenas os testes determinísticos, isolando as fontes de
instabilidade *internas* à suíte: variação de aleatoriedade (`unstable`) e dependência
do CDN de imagens (`external`). O job **full** executa tudo em caráter informativo:
falhas ali indicam instabilidade nesses pontos, não regressão de código.

**Limite conhecido:** essa separação não torna o pipeline tolerante a uma
indisponibilidade da Dog API. Todos os testes, inclusive os do `core`, consomem a API
pública; se ela estiver fora, o `core` falha. O que o desenho entrega é falha *rápida*
e diagnóstico claro (ver timeouts abaixo), não tolerância a outage. Tolerar isso exigiria
retry com backoff ou um mock/contract test, fora do escopo deste desafio.

Ambos publicam os relatórios (MD + PDF) como **artifact**, inclusive quando há falhas
(`if: always()`), disponíveis na aba **Actions** → execução → *Artifacts*
(`dog-api-reports-core` / `dog-api-reports-full`).

As requisições têm **timeout** configurado (5s de conexão, 10s de resposta): se a API
não responder, a execução falha rapidamente com erro explícito em vez de ficar pendurada
até o limite do runner.

---

## 🧠 Decisões técnicas

As escolhas de stack, arquitetura e os trade-offs considerados em cada ponto estão
documentados em **[COMMENTS.md](./COMMENTS.md)** — incluindo Rest Assured × Playwright,
relatório próprio × Allure, modelagem dos DTOs, estratégia de tags e organização dos
cenários.

---

## 🔎 Achados de QA

Três divergências entre documentação e comportamento observado, verificadas
manualmente contra a API em produção.

**1. Case do nome da raça — divergência confirmada.**
A documentação sugere que o nome da raça seria case-sensitive, mas a Dog API
**normaliza o case na entrada**: `GET /breed/Hound/images` retorna **200** com o mesmo
conjunto de imagens de `hound`, não 404.

*Status de cobertura:* o cenário `breedNameIsCaseSensitive` está **`@Disabled`**, com a
justificativa registrada na própria anotação. Ele foi mantido no código como
documentação executável do achado, em vez de ser removido silenciosamente. Reescrevê-lo
para asserir o comportamento *real* (200 + mesmo conjunto de `hound`) é item aberto —
ver [Débito técnico](#-débito-técnico-declarado).

**2. Contrato de erro não uniforme em rotas não mapeadas.**
O envelope de erro (`status`/`message`/`code` em JSON) só é respeitado nas rotas
mapeadas. Em rotas inválidas a API responde 404 **fora do próprio contrato** — sem
`Content-Type` e sem os campos do envelope:

| Rota | HTTP | `Content-Type` | Envelope |
|---|---|---|---|
| `/breed/hound/images/` (barra final) | 404 | *(ausente)* | ❌ |
| `/breeds/list/all/extra` | 404 | *(ausente)* | ❌ |
| `/breed/hound/notasub/images` | 404 | `application/json` | ✅ (`sub breed does not exist`) |

Por que importa: um cliente que trate todo erro esperando `status`/`message` quebra
nessas rotas.

*Status de cobertura:* **achado documentado, ainda não automatizado.** A infraestrutura
está pronta (`DogApiClient.getBreedImagesTrailingSlash`,
`getAllBreedsWithExtraSegment`, `ResponseValidator.assertNotJsonEnvelope`), os cenários
que a consomem são item aberto — ver [Débito técnico](#-débito-técnico-declarado).

**3. Mensagem de erro distingue raça de sub-raça.**
`/breed/notabreed/images` responde `Breed not found (main breed does not exist)`,
enquanto `/breed/hound/notasub/images` responde
`Breed not found (sub breed does not exist)`. Ponto positivo da API: o erro é
diagnosticável, não genérico.

*Status de cobertura:* o caso de raça principal é coberto por teste ativo; o de
sub-raça inexistente está documentado, não automatizado.

Os três estão detalhados em [COMMENTS.md](./COMMENTS.md) (itens 15, 16 e 19).

---

## 📌 Débito técnico declarado

Registro explícito do que **não** está coberto, para que o escopo real da suíte não
depender de leitura do código:

| Item | Situação |
|---|---|
| Cenários de rota não mapeada (barra final, segmento extra) | Client, flow e validator implementados; **testes pendentes** |
| `schemas/error-schema.json` | Criado, **nenhum cenário o exercita** — o corpo de erro é validado campo a campo em `ResponseValidator.assertError`, não por schema |
| Sub-raça inexistente com mensagem distinta | Achado confirmado manualmente, **teste pendente** |
| `breedNameIsCaseSensitive` | `@Disabled`; pendente reescrever para asserir o comportamento real |
| `BreedFlow.imagesForFirstBreed` / `firstBreed` | Helpers de orquestração implementados, ainda não usados por cenário |
| Resiliência a outage da API | Não implementada por decisão de escopo (ver CI, "Limite conhecido") |
| `reportInfo(...)` em testes que passam | Coletado, mas o writer só renderiza `extras` no bloco de falhas |

---

## 👤 Autor

**Samuel Lima** — QA Engineer / SDET
[LinkedIn](https://www.linkedin.com/in/samuel-lima-843a5723b)