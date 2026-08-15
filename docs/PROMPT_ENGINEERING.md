# Prompt Engineering Guidelines

How to phrase questions to get good results from `/ask`, `/agent`, and `/rag-agent` --
grounded in how this specific app's retrieval behaves, not generic LLM prompting advice.

## How this system decides what's "relevant"

`/ask` and `/rag-agent` don't hand your question straight to Claude. First,
`RetrievalAugmentationAdvisor` embeds your question via Ollama, runs a
cosine-similarity search against pgvector, and keeps only the chunks that clear two
bars (see [Configuration reference](../README.md#configuration-reference)):

- **`app.rag.similarity-threshold: 0.5`** -- a chunk below this score is dropped entirely
- **`app.rag.top-k: 4`** -- at most 4 chunks are kept, even if more clear the threshold

Only the surviving chunks get injected into the prompt. If nothing clears the bar,
`allowEmptyContext=false` means Claude is told to say it doesn't have enough
information -- not to fall back on its own training data. A decline is the system
working correctly, not a bug (see [Retrieval grounding behavior](../README.md#retrieval-grounding-behavior)).

This means **retrieval quality is entirely about whether your question's embedding
lands close to the right chunk's embedding** -- everything below follows from that.

## Rule 1: Match the document's vocabulary, not just its meaning

`nomic-embed-text` is a small, local, free embedding model -- good, but nowhere near
as strong as a large hosted model at bridging paraphrases. Verified against a real
ingested resume in this app:

| Query | Score | Result |
|---|---|---|
| `Spring AI` | 0.52 | matched |
| `fault tolerant architecture` | 0.61 | matched |
| `99.9 percent uptime` | 0.55 | matched |
| `what is my tech stack` | -- | **no match** (below 0.5) |
| `professional experience` | -- | **no match** (below 0.5) |

Same underlying document, same person asking -- the only difference is that the first
three queries reuse words that actually appear in the source text, and the last two
paraphrase instead. Prefer the document's own terms (product names, section headings,
field names) over a natural-language rewording of the same idea.

## Rule 2: Ask one focused thing at a time

Retrieval keeps at most 4 chunks. A broad, multi-part question ("summarize this
person's whole career and also tell me about their education and awards") spreads
that budget thin across unrelated chunks instead of concentrating it on the one thing
you actually need. Split compound questions into separate calls.

## Rule 3: Pick the right endpoint for the job

| Endpoint | Sees | Use when |
|---|---|---|
| `/ask` | Only what's been through `/ingest` | The answer lives in a document you've already embedded |
| `/agent` | Live files on disk, via MCP -- nothing pre-embedded | You need the *current* state of a file, or it was never ingested |
| `/rag-agent` | Both | The question could need either, or both together |

A question like "what does `application.yml` currently configure" needs `/agent` --
`/ask` would only ever see whatever was true at ingestion time, which goes stale the
moment the file changes.

## Rule 4: Preview retrieval before trusting an answer

`GET /ask/preview` runs the identical similarity search `/ask` uses and returns the
matched chunks directly -- scores, text, metadata -- with no Claude call at all. If
`/ask` gives a disappointing or declined answer, check `/ask/preview` first:

- **Empty result** -- your wording didn't clear the threshold (see Rule 1), or the
  content genuinely wasn't ingested. Rephrase or check `/ingest`.
- **Wrong chunk matched** -- the right content exists but scored lower than an
  unrelated chunk. Try more specific, source-worded phrasing.
- **Right chunk, low score** -- borderline retrieval; consider whether
  `app.rag.similarity-threshold` is set appropriately for your corpus.

This also works without an Anthropic API key, so it's the fastest way to debug
ingestion and retrieval in isolation.

The same idea applies to `/agent`: `GET /agent/tools` lists what the MCP filesystem
server exposes, and `POST /agent/tools/{name}` invokes one directly with raw JSON
arguments -- bypassing Claude's reasoning entirely. If `/agent` isn't behaving as
expected, check whether the underlying tool call even works before assuming Claude's
tool selection is at fault.

## Rule 5: A decline is information, not failure

If `/ask` responds that it doesn't have enough information, that's
`allowEmptyContext=false` doing its job -- refusing to guess from Claude's general
training data when your documents don't cover it. Treat it as a signal to either
ingest the missing content or rephrase closer to what's already there, not as the
system malfunctioning.

## Quick checklist

- [ ] Reuse the document's own words where you can, not a paraphrase
- [ ] One question per call, not several bundled together
- [ ] `/ask` for embedded content, `/agent` for live files, `/rag-agent` for either
- [ ] Got a weak or declined answer? Check `/ask/preview` before assuming something's broken
- [ ] A decline means "not in the corpus (yet)," not "the app is broken"
