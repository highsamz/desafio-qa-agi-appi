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

> Não é necessário instalar o Maven separadamente se preferir usar um wrapper próprio;
> o projeto roda com qualquer Maven 3.9+ em Linux, Windows ou macOS.

---

## ▶️ Como executar

Clone e rode a suíte completa:

```bash
git clone https://github.com/highsamz/desafio-qa-agi-appi.git
cd desafio-qa-agi-appi
mvn test
```

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

### `GET /breeds/list/all`
- Contrato: 200, JSON, `status: success`, aderência ao JSON Schema
- Conteúdo: lista íntegra e não vazia; raça conhecida com sub-raças esperadas; raça
  sem sub-raça retornando lista vazia
- Negativos: rota inválida (segmento extra) → 404 fora do envelope JSON

### `GET /breed/{breed}/images`
- Caminho feliz: raça válida retorna lista não vazia; aderência ao Schema; URLs contêm
  o nome da raça; raça sem sub-raça também retorna imagens; sub-raça
  (`/breed/{breed}/{subBreed}/images`); raça principal combinando várias sub-raças
- Negativos: raça inexistente → 404 com `status: error` e `message` contendo
  *"not found"*; corpo de erro aderente ao Schema de erro; sub-raça inexistente com
  mensagem distinta da raça principal; rota inválida (barra final) rompendo o envelope

### `GET /breeds/image/random`
- Contrato: 200, JSON, `status: success`, aderência ao Schema
- Conteúdo: `message` é uma URL de imagem válida
- Aleatoriedade: chamadas repetidas retornam URLs majoritariamente distintas
- Acessibilidade (`@external`): a imagem retornada responde a um HEAD com 200

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
para cada falha, a mensagem de erro e um trecho de stack trace filtrado.

A coleta é feita via `TestWatcher` (JUnit 5) e a geração dispara no encerramento da
execução — sem parsing de log.

---

## 🔄 Integração contínua (CI)

O workflow em `.github/workflows/ci.yml` executa em **push**, **pull request** e
**manualmente** (`workflow_dispatch`):

O pipeline é dividido em dois jobs:

| Job | Escopo | Bloqueante |
|---|---|---|
| `core` | `mvn -B test -DexcludedGroups=external,unstable` | ✅ Sim |
| `full` | `mvn -B test` (suíte completa) | ❌ Não (`continue-on-error`) |

O job **core** roda apenas os testes determinísticos, isolando as fontes de
instabilidade *internas* à suíte, variação de aleatoriedade (`unstable`) e dependência
do CDN de imagens (`external`). O job **full** executa tudo em caráter informativo:
falhas ali indicam instabilidade nesses pontos, não regressão de código.

**Limite conhecido:** essa separação não torna o pipeline tolerante a uma
indisponibilidade da Dog API. Todos os testes, inclusive os do `core`, consomem a API
pública, se ela estiver fora, o `core` falha. O que o desenho entrega é falha *rápida*
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

**1. Case do nome da raça.** A documentação sugere que o nome da raça seria
case-sensitive, mas a Dog API **normaliza o case na entrada**: `Hound` retorna 200 com
exatamente o mesmo conjunto de imagens de `hound`. Coberto por teste ativo que compara
os dois conjuntos.

**2. Contrato de erro não uniforme.** Em rotas não mapeadas (ex.: `/breed/hound/images/`
com barra final, ou `/breeds/list/all/extra`), a API responde **fora do próprio
envelope** — sem `Content-Type: application/json` e sem os campos `status`/`message`
presentes nos demais erros. Coberto por testes que documentam o comportamento.

Ambos estão detalhados em [COMMENTS.md](./COMMENTS.md).

---

## 👤 Autor

**Samuel Lima** — QA Engineer / SDET
[LinkedIn](https://www.linkedin.com/in/samuel-lima-843a5723b)