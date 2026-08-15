# FreightSource Knowledge Assistant

A local Spring Boot app that answers questions about your own documents (and code, PDFs, RFCs) in natural language. Built on Spring AI, Claude, pgvector, and MCP:

- **Claude** (Anthropic) does the reasoning/answering
- **RAG** grounds answers in your own docs, embedded into **pgvector** (Postgres) via a local **Ollama** embedding model
- **MCP** (Model Context Protocol) lets Claude also read files live, directly from disk, on top of anything embedded

Everything runs locally except the actual Claude API call. Embeddings are free (local Ollama model, no API key, no per-token cost) — you only pay Anthropic for chat/reasoning tokens.

---

## Table of contents

- [Architecture](#architecture)
- [RAG request flow](#rag-request-flow)
- [MCP tool-call flow](#mcp-tool-call-flow)
- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quickstart](#quickstart)
- [Security](#security)
- [Exploring the API](#exploring-the-api)
- [Supported document formats](#supported-document-formats)
- [Retrieval grounding behavior](#retrieval-grounding-behavior)
- [Configuration reference](#configuration-reference)
- [Web UI](#web-ui)
- [Testing](#testing)
- [Prompt engineering guidelines](#prompt-engineering-guidelines)
- [Project structure](#project-structure)
- [Local build vs. production infrastructure](#local-build-vs-production-infrastructure)
- [Troubleshooting](#troubleshooting)

---

## Architecture

```mermaid
flowchart LR
    subgraph Client["Client"]
        WebUI["Web UI<br/>/"]
        Swagger["Swagger UI<br/>/swagger-ui"]
    end

    subgraph App["Spring Boot app (Spring AI)"]
        Ingest["POST /ingest, /ingest/upload<br/>DocumentIngestor"]
        Chat["GET /chat<br/>ChatController"]
        Ask["GET /ask<br/>RagController"]
        Agent["GET /agent<br/>AgentController"]
        Combo["GET /rag-agent<br/>RagAgentController"]
    end

    Docs["Your docs<br/>.md / .txt / .pdf / .docx / .csv / .xlsx"] --> Ingest
    Ingest -->|embed| Ollama["Ollama<br/>nomic-embed-text"]
    Ollama --> PGVector[("pgvector<br/>Postgres")]

    Ask -->|similarity search| PGVector
    Combo -->|similarity search| PGVector
    Ask --> Claude
    Chat --> Claude
    Combo --> Claude
    Agent -->|tool_use| MCP["MCP filesystem server<br/>(launched via npx)"]
    Combo -->|tool_use| MCP
    MCP -->|live file read| FS[("Local filesystem")]

    Agent -->|tool_use| MCPPG["MCP Postgres server<br/>(launched via uvx, read-only)"]
    Combo -->|tool_use| MCPPG
    MCPPG -->|read-only SQL| PGVector

    Claude["Claude<br/>Anthropic API"]

    WebUI --> App
    Swagger --> App
```

Two independent capabilities meet in the last three endpoints:

1. **Retrieval (RAG)** — documents are chunked and embedded offline (`/ingest`), then similarity-searched at query time (`/ask`) so Claude answers grounded in *your* content instead of its training data.
2. **Tool-calling (MCP)** — Claude can additionally call out to two MCP servers: a filesystem server to read a file *live* whether or not it was ever ingested, and a read-only Postgres server to run direct SQL against the database — both via `/agent`.

`/rag-agent` runs both on one `ChatClient`, so a single query can draw on grounded retrieval and live file access together.

## RAG request flow

```mermaid
sequenceDiagram
    participant U as User
    participant R as RagController
    participant V as pgvector
    participant C as Claude

    U->>R: GET /ask?q=...
    R->>V: embed q via Ollama, similarity search top-4, threshold 0.5
    V-->>R: matching chunks, or none
    R->>C: prompt plus retrieved chunks as context
    C-->>R: grounded answer, or a decline if nothing relevant
    R-->>U: response
```

This is `RetrievalAugmentationAdvisor` (built on `VectorStoreDocumentRetriever`) performing the retrieval and prompt augmentation — see [Retrieval grounding behavior](#retrieval-grounding-behavior) for how an empty result set is handled.

## MCP tool-call flow

```mermaid
sequenceDiagram
    participant U as User
    participant A as AgentController
    participant C as Claude
    participant M as MCP filesystem server

    U->>A: GET /agent?q=...
    A->>C: prompt plus available tool definitions
    C-->>A: tool_use block, e.g. read application.yml
    A->>M: SyncMcpToolCallbackProvider executes the call
    M-->>A: file contents
    A->>C: tool result appended to the conversation
    C-->>A: final answer, grounded in the live file
    A-->>U: response
```

Claude never touches the filesystem (or the database) directly — it emits a structured `tool_use` request, and Spring AI's `SyncMcpToolCallbackProvider` is what actually executes it against the right MCP server and feeds the result back into the conversation. A second MCP server, **Postgres MCP Pro** (`postgres-mcp`, run with `--access-mode=restricted`), is wired in alongside the filesystem one — it gives Claude direct, read-only SQL access to the database (schema introspection, ad hoc queries against `vector_store`), distinct from the embedding-based similarity search `/ask` already does. Restricted mode parses SQL before execution and rejects `COMMIT`/`ROLLBACK`, closing the transaction-control bypass that broke the older, now-archived `@modelcontextprotocol/server-postgres` package's read-only guarantee.

Both servers register their tools into the same `ToolCallbackProvider`, so no controller code distinguishes between them — `/agent`, `/rag-agent`, and the endpoints below see one merged tool list.

To inspect the MCP layer on its own, without a valid Anthropic API key: `GET /agent/tools` lists what every connected MCP server exposes (filesystem and Postgres both), and `POST /agent/tools/{name}` invokes one directly with a JSON object of arguments, bypassing Claude's reasoning entirely.

## Tech stack

| Component | Version / detail | Role |
|---|---|---|
| Java | 21 | Language runtime |
| Spring Boot | 3.5.15 | Application framework |
| Spring AI | 1.1.8 | Claude/Ollama/pgvector/MCP integration layer |
| Claude | `claude-sonnet-5` (Anthropic API) | Reasoning / answering |
| Ollama | `nomic-embed-text`, 768 dimensions | Local, free embeddings |
| Postgres + pgvector | via Docker, `pgvector/pgvector:pg16` | Vector store |
| MCP filesystem server | `@modelcontextprotocol/server-filesystem`, via `npx` | Live file-read tool for Claude |
| MCP Postgres server | Postgres MCP Pro (`postgres-mcp`), via `uvx`, restricted mode | Read-only SQL access to the database for Claude |
| Apache Tika | via `spring-ai-tika-document-reader` | `.docx` ingestion |
| Apache Commons CSV | 1.14.1 | `.csv` ingestion (row-aware) |
| Apache POI | 5.5.1 | `.xlsx` ingestion (row-aware) |
| springdoc-openapi | 2.9.0 | Swagger UI / OpenAPI docs |
| Web UI | Single `index.html`, no framework | Browser-based chat interface |

Spring AI is pinned to `1.1.8`, paired with Spring Boot `3.5.15`. The newer Spring AI `2.0.0` line requires Spring Boot 4.x and is not yet used in this project.

## Prerequisites

- Java 21
- Maven
- Docker Desktop (for Postgres + pgvector)
- [Ollama](https://ollama.com) installed locally
- An Anthropic API key from [console.anthropic.com](https://console.anthropic.com) (**not** the same as a claude.ai subscription — API access is billed separately, pay-as-you-go)
- Node.js / `npx` (used to launch the MCP filesystem server on demand — no separate install needed)
- [uv](https://docs.astral.sh/uv/getting-started/installation/) / `uvx` (used to launch the MCP Postgres server on demand, the same way `npx` does for the filesystem server)

## Quickstart

```bash
# 1. Pull the local embedding model (one-time, ~270MB, no API key needed)
ollama pull nomic-embed-text

# 2. Start Postgres + pgvector
docker compose up -d

# 3. Set your Anthropic API key for this shell
export ANTHROPIC_API_KEY=sk-ant-...

# 4. Build and run
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

**Note on ports:** the Postgres container listens on host port **5434**, not the default 5432 — this avoids clashing with any Postgres already installed natively on your machine. If you're running this alongside other Dockerized Postgres instances, check `docker-compose.yml` before starting.

## Security

Every endpoint — including Swagger UI and the web UI — requires **HTTP Basic auth**. There's no anonymous access anywhere in the app.

| Property | Env var | Local default | Purpose |
|---|---|---|---|
| `app.security.username` | `APP_SECURITY_USERNAME` | `admin` | Basic auth username |
| `app.security.password` | `APP_SECURITY_PASSWORD` | `changeme` | Basic auth password |

The defaults are for local development only. `SecurityConfig` logs a startup warning if the password is still `changeme`, as a reminder to set `APP_SECURITY_PASSWORD` before running this anywhere beyond a laptop. Passwords are stored hashed with `BCryptPasswordEncoder`, never in plaintext.

To call any endpoint with `curl`, pass credentials directly:

```bash
curl -u admin:changeme "http://localhost:8080/chat?q=hello"
```

In Swagger UI, click **Authorize** and enter the same credentials once — it's then applied to every "Try it out" call for the rest of the session. Browsers prompt for credentials automatically when you open the web UI.

CSRF protection is disabled deliberately: this is a stateless, credential-per-request API with no session cookies and no browser form submissions, so the CSRF threat model doesn't apply here.

## Exploring the API

**Swagger UI is the source of truth for the API** — open **http://localhost:8080/swagger-ui/index.html**. Every endpoint is documented there with a real description, example values on every parameter, and a "Try it out" button to execute requests directly from the browser. Raw OpenAPI JSON is at `http://localhost:8080/v3/api-docs`, if you want to generate a client or import it into Postman/Insomnia.

Quick reference (full details, examples, and live testing are in Swagger UI):

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/chat` | Plain Claude call — sanity check for the API key and Spring AI wiring, no RAG/MCP |
| `POST` | `/ingest` | Chunks and embeds a file or folder that already exists on this machine into pgvector |
| `POST` | `/ingest/upload` | Same pipeline, but for a file uploaded directly from the caller's machine (multipart, up to 20MB) — this is the one Swagger UI's file picker uses |
| `GET` | `/ask` | RAG: retrieves relevant chunks and answers grounded in them |
| `GET` | `/ask/preview` | Runs the same retrieval as `/ask` and returns the matched chunks directly, with scores and metadata — no Claude call, so it works without a valid API key |
| `GET` | `/agent` | MCP tool-calling: Claude can read a live file from disk or run a read-only SQL query against the database |
| `GET` | `/agent/tools` | Lists the MCP tools available to Claude from both connected servers (name, description, JSON schema) — no Claude call |
| `POST` | `/agent/tools/{name}` | Invokes a named MCP tool directly with a JSON object of arguments, bypassing Claude entirely — proves the MCP servers themselves work without a valid API key |
| `GET` | `/rag-agent` | Combined: grounded retrieval and live tool access (filesystem + database) on one `ChatClient` |

## Supported document formats

`/ingest` handles two fundamentally different shapes of content, and treats them differently on purpose:

| Format | Reader | Chunking |
|---|---|---|
| `.md`, `.txt` | Spring AI `TextReader` | Split into ~800-token chunks via `TokenTextSplitter` |
| `.pdf` | Spring AI `PagePdfDocumentReader` | Split into ~800-token chunks via `TokenTextSplitter` |
| `.docx` | Spring AI `TikaDocumentReader` (Apache Tika) | Split into ~800-token chunks via `TokenTextSplitter` |
| `.csv` | Apache Commons CSV | **One row = one chunk**, not token-split |
| `.xlsx` | Apache POI | **One row = one chunk**, not token-split (every sheet is read) |

Prose documents (contracts, RFCs, notes) get **token-based chunking** — reasonable, since meaning in prose is roughly uniform across a document and an ~800-token window is a sensible retrieval unit.

Tabular documents (rate cards, PO logs) get **row-based chunking** instead — each row is formatted as `Column: value | Column: value | ...` and embedded as its own atomic chunk, tagged with `source`, `row`, and (for `.xlsx`) `sheet` metadata. Token-splitting a spreadsheet would cut rows in half at arbitrary token boundaries and destroy the very structure that makes a row meaningful (a freight rate detached from its lane and carrier is useless) — so tabular formats skip the splitter entirely.

## Retrieval grounding behavior

This project uses Spring AI's `RetrievalAugmentationAdvisor` (built on `VectorStoreDocumentRetriever`) rather than the older `QuestionAnswerAdvisor`. Both are available in Spring AI 1.1.8, but `RetrievalAugmentationAdvisor` is the current, more modular RAG pipeline, provided by the `spring-ai-rag` dependency.

Its default `ContextualQueryAugmenter` has `allowEmptyContext=false`: when similarity search returns nothing above the threshold, Claude is instructed to say it doesn't have enough information, instead of answering from its own training data. That's what makes `/ask` correctly decline out-of-corpus questions rather than hallucinate.

To inspect retrieval on its own — e.g. while debugging ingestion, or without a valid Anthropic API key at hand — `GET /ask/preview` runs the identical similarity search and returns the matched chunks directly as JSON, skipping the Claude call entirely.

## Configuration reference

All of the following live in `src/main/resources/application.yml`:

| Property | Value | Why |
|---|---|---|
| `app.rag.similarity-threshold` | `0.5` | Minimum similarity score for a chunk to be considered relevant, bound via `RagProperties` |
| `app.rag.top-k` | `4` | Maximum number of chunks retrieved per question, bound via `RagProperties` |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | `20MB` | Caps uploads accepted by `POST /ingest/upload` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5434/genai` | Points at the Docker Postgres container (non-default port — see Quickstart) |
| `spring.ai.model.chat` | `anthropic` | Disambiguates which starter owns the `ChatModel` bean — both Anthropic and Ollama starters are on the classpath, and without this there are two competing beans |
| `spring.ai.model.embedding` | `ollama` | Same disambiguation, for `EmbeddingModel` |
| `spring.ai.anthropic.chat.model` | `claude-sonnet-5` | The Claude model used for all reasoning/answering |
| `spring.ai.ollama.embedding.model` | `nomic-embed-text` | The local embedding model |
| `spring.ai.vectorstore.pgvector.dimensions` | `768` | Must match `nomic-embed-text`'s output size |
| `spring.ai.vectorstore.pgvector.index-type` | `HNSW` | Approximate nearest-neighbor index type |
| `spring.ai.vectorstore.pgvector.distance-type` | `COSINE_DISTANCE` | Similarity metric |
| `spring.ai.vectorstore.pgvector.initialize-schema` | `true` | Auto-creates the `vector_store` table on startup |
| `spring.ai.mcp.client.stdio.connections.filesystem.args` | `${MCP_FILESYSTEM_ROOT:.}` | Directory the MCP filesystem server is allowed to read, defaulting to the working directory the app was started from. Override with the `MCP_FILESYSTEM_ROOT` environment variable to point `/agent` and `/rag-agent` elsewhere |
| `spring.ai.mcp.client.stdio.connections.postgres.args` | `--access-mode=restricted`, `${POSTGRES_MCP_URL:postgresql://genai:genai@localhost:5434/genai}` | Runs the Postgres MCP server in read-only mode against the same database as `spring.datasource.url` (note the different URI scheme — `postgresql://`, not `jdbc:postgresql://`). Override with `POSTGRES_MCP_URL` to point elsewhere |

## Web UI

Open **http://localhost:8080/** for a chat-style page — a single self-contained `index.html`, plain HTML/CSS/JS, no framework, no build step.

- Sidebar mode picker: **Chat / Ask (RAG) / Agent (MCP) / RAG + Agent** — each mode has its own accent color that carries through the header, the send button, and every message that mode answers, so scrolling back through a conversation shows at a glance which mode handled each turn
- Dark/light theme toggle
- An "Ingest documents" modal so you can embed a folder without leaving the browser

## Testing

```bash
mvn test
```

42 unit tests, no external services required (no Docker, Ollama, or Anthropic API key needed — everything is mocked or exercised in isolation).

| Area | Approach |
|---|---|
| Document format readers (`CsvFormatReader`, `XlsxFormatReader`, `MarkdownFormatReader`, `TabularRowFormatter`) | Plain JUnit 5 against real temp files (`@TempDir`) — no mocking, since these are pure parsing logic |
| `DocumentIngestor` | Mockito (`@ExtendWith(MockitoExtension.class)`) — verifies the Strategy pattern routes each file to the reader that supports it, and that an unsupported format throws |
| `RagProperties` | Validates the compact-constructor bounds checking on `app.rag.*` |
| `GlobalExceptionHandler` | Verifies `IllegalArgumentException` maps to 400 with the real message, and unexpected exceptions map to 500 without leaking internal details |
| `ChatController` + `SecurityConfig` | `@WebMvcTest` slice test with `spring-security-test` — asserts requests with no credentials or the wrong password get `401`, and a correctly authenticated request reaches the controller and returns Claude's (mocked) answer |
| `IngestController` (`/ingest/upload`) | `@WebMvcTest` — asserts an unauthenticated upload is rejected, a valid upload is written to a temp file preserving its extension and passed to `DocumentIngestor`, and a filename-less upload is rejected as a bad request |
| `RagController` (`/ask/preview`) | `@WebMvcTest` — asserts an unauthenticated request is rejected, and a valid request returns the mocked `VectorStoreDocumentRetriever` results as JSON (id, score, text, metadata) without going through `ChatClient` |
| `AgentController` (`/agent/tools`, `/agent/tools/{name}`) | `@WebMvcTest` — asserts an unauthenticated listing is rejected, tool metadata is returned correctly, a named tool is invoked directly with the given JSON arguments without going through `ChatClient`, and an unknown tool name is rejected as a bad request |

## Prompt engineering guidelines

`/ask`, `/agent`, and `/rag-agent` all reward specific phrasing habits, since retrieval
quality depends on how closely a question's embedding lands to the right chunk's. See
**[docs/PROMPT_ENGINEERING.md](docs/PROMPT_ENGINEERING.md)** for the practical rules
(with real, measured examples) on wording questions, picking the right endpoint, and
using `/ask/preview` to debug retrieval before trusting an answer.

## Project structure

```
src/main/java/com/freightsource/ragassistant/
  chat/           ChatController              -- /chat
  ingest/         DocumentIngestor,
                  IngestController             -- /ingest, /ingest/upload
                  reader/DocumentFormatReader  -- format strategy interface
                  reader/MarkdownFormatReader  -- .md, .txt
                  reader/PdfFormatReader       -- .pdf
                  reader/DocxFormatReader      -- .docx
                  reader/CsvFormatReader       -- .csv (row-based)
                  reader/XlsxFormatReader      -- .xlsx (row-based)
  rag/            RagController               -- /ask, /ask/preview
                  RetrievedChunk               -- /ask/preview response shape
  agent/          AgentController             -- /agent, /agent/tools, /agent/tools/{name}
                  McpToolInfo                 -- /agent/tools response shape
  combined/       RagAgentController          -- /rag-agent
  config/         AiCapabilitiesConfig        -- shared Advisor / ToolCallbackProvider / TokenTextSplitter beans
                  RagProperties               -- app.rag.* configuration
                  SecurityConfig              -- HTTP Basic auth filter chain
                  AppSecurityProperties       -- app.security.* configuration
                  OpenApiConfig               -- Swagger UI metadata
  error/          GlobalExceptionHandler      -- centralized ProblemDetail (RFC 7807) error responses

src/main/resources/
  application.yml                    -- Anthropic / Ollama / pgvector / MCP config
  static/index.html                  -- the web UI (served at /)

docs/
  PROMPT_ENGINEERING.md              -- see above
  deck/build-deck.js                 -- generates spring-ai-claude-rag-mcp-overview.pptx (repo root);
                                         edit this and re-run to update the deck, no separate template
  deck/screenshots/                  -- real UI screenshots embedded in the deck
```

`DocumentIngestor` depends only on the `DocumentFormatReader` interface, not on any specific parser — adding support for a new file format means adding one new `DocumentFormatReader` implementation, not modifying the ingestion orchestrator. `RagController`, `AgentController`, and `RagAgentController` all consume the same `Advisor` and `ToolCallbackProvider` beans from `AiCapabilitiesConfig` rather than each constructing their own.

## Local build vs. production infrastructure

What a production deployment would change relative to this local setup:

| Concern | This local build | Production |
|---|---|---|
| Postgres + pgvector | Docker container on a laptop | Managed Postgres (RDS / Cloud SQL) with the pgvector extension enabled — same tech, just hosted |
| Embedding model | Local Ollama (`nomic-embed-text`) | A hosted embedding API (Voyage AI, OpenAI) for simplicity, or a self-hosted embedding service on GPU infra for cost/privacy-sensitive teams — the code barely changes, since it's behind Spring AI's `EmbeddingModel` interface |
| Document ingestion | Synchronous — call `/ingest`, wait | An async, queue-triggered pipeline (SQS/Kafka), since embedding thousands of docs shouldn't block a request; only query-time embedding of the user's question stays synchronous |
| Claude API calls | Already a real hosted call | Unchanged — nothing to swap |

## Troubleshooting

- **`Connection refused` on startup / pgvector errors**: make sure `docker compose up -d` succeeded and the container is healthy: `docker ps --filter name=genai-pg`.
- **Port 5434 already in use**: another instance of this project's container is probably already running, or something else claimed that port — check with `lsof -nP -iTCP:5434`.
- **Application startup fails or hangs with an MCP client timeout**: both MCP servers launch during application startup (not on the first `/agent` request) — the filesystem server via `npx -y @modelcontextprotocol/server-filesystem ...`, the Postgres server via `uvx postgres-mcp ...` — and each downloads its package the first time it runs. A slow or cold install can exceed the MCP client's initialization timeout. Make sure both `npx` and `uvx` are on the `PATH`, and try starting the application again once the packages have been downloaded once.
- **`uvx: command not found`**: `uv` isn't installed — see [Prerequisites](#prerequisites). `uvx` ships with `uv`; there's nothing extra to install once `uv` itself is present.
- **`uvx` found, but the Postgres MCP connection still fails on startup**: two known first-run issues, already handled by this project's config, but worth knowing about if you're debugging further:
  - `postgres-mcp` depends on `pglast` and `cryptography`, both C extensions. If your machine has no prebuilt wheel for your Python/platform combination, `uv` compiles them from source on first run, which can take several minutes — well past `uvx`'s normal near-instant startup. This only happens once; after that, the build is cached.
  - `postgres-mcp==0.3.0` doesn't cap its `mcp` SDK dependency, and `mcp` 2.0.0 renamed/removed the module `postgres-mcp` imports (`mcp.server.fastmcp`), crashing it on import. `spring.ai.mcp.client.stdio.connections.postgres.args` already pins this via `uvx --with 'mcp<2.0.0' postgres-mcp ...` — if you see `ModuleNotFoundError: No module named 'mcp.server.fastmcp'`, confirm that pin is still in place.
  - Even with both of the above handled, `postgres-mcp`'s cold start (Python interpreter boot, module imports, DB pool init) can take longer than Spring AI's default 20s MCP request timeout. `spring.ai.mcp.client.request-timeout: 60s` is set for exactly this reason — don't remove it.
- **Ollama connection errors**: confirm Ollama is running (`ollama list` should show `nomic-embed-text`) and reachable at `http://localhost:11434`.
