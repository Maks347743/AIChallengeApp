# Module: rag-server

Standalone Compose Desktop application providing semantic document retrieval via MCP.

## Package
`com.example.ragserver`

## Port
3002

## Startup Modes

```bash
# With desktop UI (document management + indexing):
./gradlew run

# Headless server only:
./gradlew run --args="--headless"

# Import folder of markdown files and index them:
./gradlew run --args="--import /path/to/docs/folder"
```

## Architecture

```
Main.kt
  ├── startWithUi()    — Compose Desktop window + Ktor server
  ├── startHeadless()  — Ktor server only
  └── startImport()    — Import .md/.txt files and index, then exit
        ├── DocumentStorage.create() per file (deduplicates by source path)
        └── IndexingService.runIndexing(StructuralChunkingStrategy)
```

## Key Components

### RetrievalPipeline
`mcp/RetrievalPipeline.kt`

Multi-stage retrieval:
1. QueryRewriter (optional) — reformulate query for better matching
2. OllamaEmbeddingService — embed query to FloatArray
3. VectorIndex.searchWithScores() — cosine similarity, top-initialK
4. Reranker (optional) — cross-encoder reranking, top-topK
5. Filter by similarityThreshold

### VectorIndex
`data/VectorIndex.kt`

- In-memory `ConcurrentHashMap<String, FloatArray>`
- `add(chunkId, vector)` — store embedding
- `searchWithScores(query, topK)` — cosine similarity search
- `save(path)` / `load(path)` — atomic JSON serialization to `~/.ragserver/index/index.bin`

### DocumentStorage
`data/DocumentStorage.kt`

- Stores documents as JSON files in `~/.ragserver/docs/`
- `create(title, source, content)` — generates UUID, saves file
- `loadAll()` — reads all `*.json` files, sorted by createdAt
- `delete(id)` — removes file (used for deduplication in `--import`)

### ChunkStorage
`data/ChunkStorage.kt`

- Stores chunks as JSON files in `~/.ragserver/chunks/`
- In-memory cache with lazy disk loading

### IndexingService
`service/IndexingService.kt`

```kotlin
suspend fun runIndexing(strategy: ChunkingStrategy)
```
1. `documentStorage.loadAll()` — get all documents
2. `chunkStorage.clear()` + `vectorIndex.reset()`
3. Apply chunking strategy to each document
4. For each chunk: `embeddingService.embed(text)` → `vectorIndex.add()` + `chunkStorage.save()`
5. `vectorIndex.save(indexPath)`

### Chunking Strategies

`chunking/StructuralChunkingStrategy.kt` — splits by markdown headers (`#`, `##`, etc.), max 300 words per chunk. Best for documentation.

`chunking/FixedSizeChunkingStrategy.kt` — fixed 500-word chunks with 50-word overlap. Best for prose.

### Embedding & Reranking

| Component | Cloud | Local |
|-----------|-------|-------|
| QueryRewriter | DeepSeekQueryRewriter | OllamaQueryRewriter |
| Reranker | JinaReranker | OllamaReranker |
| Embedding | — | OllamaEmbeddingService |

Switched via `RagConfig.useLocalModel`.

### RetrieveTool
`mcp/tools/RetrieveTool.kt`

MCP tool name: `retrieve`

Response includes two content blocks:
1. Formatted chunk text (for AI consumption)
2. `__RAG_META__:{json}` — chunk metadata for citation tracking (parsed by `ExecuteToolCallsUseCase` in Android app)

### Document Ingestion Methods

1. **UI file picker** — import `.md`, `.txt`, `.rst` files via Compose Desktop dialog
2. **Manual entry** — type/paste content in UI
3. **DeepWiki import** — fetch docs from GitHub wiki via DeepWiki MCP API
4. **CLI import** — `--import /path/to/folder` reads all `.md`/`.txt`/`.rst` recursively

### File Storage Layout

```
~/.ragserver/
  config.json        — RagConfig (API keys, model names, thresholds)
  docs/
    {uuid}.json      — Document objects
  chunks/
    {uuid}.json      — Chunk objects with metadata
  index/
    index.bin        — Vector index (JSON map: chunkId → List<Float>)
```

## Configuration (RagConfig)

Persisted to `~/.ragserver/config.json`. Editable via UI settings tab.

Key settings:
- `topK` — chunks returned to AI (default: 3)
- `initialK` — candidates before reranking (default: 20)
- `similarityThreshold` — minimum cosine similarity (default: 0.3)
- `useQueryRewrite` — whether to rewrite query before embedding
- `useRerank` — whether to apply cross-encoder reranking
- `useLocalModel` — switch between cloud and local models
