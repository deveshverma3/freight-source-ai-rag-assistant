// Generates spring-ai-claude-rag-mcp-overview.pptx (in the repo root) from
// scratch. Edit the content arrays/strings below and re-run to update the
// deck -- there's no separate template file, this script is the only source
// of truth for its content and layout.
//
// Usage:
//   npm install pptxgenjs   # only if `require("pptxgenjs")` fails below
//   node docs/deck/build-deck.js
//   mv spring-ai-claude-rag-mcp-overview.pptx ../../   # if run from docs/deck/

const fs = require("fs");
const path = require("path");
const pptxgen = require("pptxgenjs");

function loadImageDataUri(filename) {
  const buf = fs.readFileSync(path.join(__dirname, "screenshots", filename));
  return "image/png;base64," + buf.toString("base64");
}

const PROJECT_NAME = "FreightSource Knowledge Assistant";

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE"; // 13.33 x 7.5 in
pres.company = PROJECT_NAME;
pres.title = PROJECT_NAME;

// ---------- palette ----------
const DARK_BG = "15171F";
const LIGHT_BG = "FFFFFF";
const TEXT_DARK = "1B1A24";
const TEXT_LIGHT = "F4F3F7";
const MUTED = "6B6B7B";
const MUTED_ON_DARK = "A9ABC0";
const BORDER = "E6E3F0";
const CARD_BG = "FAF9FC";

const VIOLET = "7C5CFF";
const CORAL = "FF7A59";
const PINK = "EF5DA8";

// mode colors -- mirror the actual web UI, used as the deck's visual motif
const C = {
  chat: "4FA3FF",
  ask: "2BD4A3",
  agent: "F5A623",
  combo: "EF5DA8",
  ingest: "7C5CFF",
};

const FONT_HEAD = "Cambria";
const FONT_BODY = "Calibri";

const PAGE_W = 13.33;
const MARGIN_X = 0.6;
const CONTENT_W = PAGE_W - MARGIN_X * 2;

let slideCounter = 0;

function footer(slide, dark) {
  slideCounter++;
  slide.addText(PROJECT_NAME, {
    x: MARGIN_X, y: 7.14, w: 6, h: 0.3,
    fontFace: FONT_BODY, fontSize: 9, color: dark ? MUTED_ON_DARK : MUTED, margin: 0,
  });
  slide.addText(String(slideCounter), {
    x: PAGE_W - MARGIN_X - 0.6, y: 7.14, w: 0.6, h: 0.3,
    fontFace: FONT_BODY, fontSize: 9, color: dark ? MUTED_ON_DARK : MUTED, align: "right", margin: 0,
  });
}

function titleBlock(slide, title, subtitle, dark) {
  slide.addText(title, {
    x: MARGIN_X, y: 0.45, w: CONTENT_W, h: 0.75,
    fontFace: FONT_HEAD, fontSize: 30, bold: true,
    color: dark ? TEXT_LIGHT : TEXT_DARK, margin: 0,
  });
  if (subtitle) {
    slide.addText(subtitle, {
      x: MARGIN_X, y: 1.12, w: CONTENT_W, h: 0.4,
      fontFace: FONT_BODY, fontSize: 14, color: dark ? MUTED_ON_DARK : MUTED, margin: 0,
    });
  }
}

function iconCircle(slide, x, y, d, color, label, opts) {
  opts = opts || {};
  slide.addShape("ellipse", { x, y, w: d, h: d, fill: { color }, line: { type: "none" } });
  slide.addText(label, {
    x, y, w: d, h: d, align: "center", valign: "middle",
    fontFace: FONT_BODY, fontSize: opts.fontSize || 12, bold: true, color: "FFFFFF", margin: 0,
  });
}

function card(slide, x, y, w, h, opts) {
  opts = opts || {};
  slide.addShape("roundRect", {
    x, y, w, h, rectRadius: 0.08,
    fill: { color: opts.fill || CARD_BG },
    line: { color: opts.line || BORDER, width: 1 },
    shadow: opts.shadow === false ? undefined : {
      type: "outer", color: "000000", opacity: 0.10, blur: 6, offset: 2, angle: 90,
    },
  });
}

// =========================================================
// 1. TITLE
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: DARK_BG };

  iconCircle(s, PAGE_W / 2 - 0.5, 1.05, 1.0, VIOLET, "✦", { fontSize: 26 });

  s.addText(PROJECT_NAME, {
    x: 1.0, y: 2.55, w: PAGE_W - 2.0, h: 1.3,
    fontFace: FONT_HEAD, fontSize: 34, bold: true, color: TEXT_LIGHT,
    align: "center", valign: "middle", margin: 0,
  });
  s.addText("Retrieval-augmented question answering with live document and database access", {
    x: 1.0, y: 3.7, w: PAGE_W - 2.0, h: 0.5,
    fontFace: FONT_BODY, fontSize: 17, color: MUTED_ON_DARK,
    align: "center", margin: 0,
  });

  const dotY = 4.5, dotD = 0.14, gap = 0.3;
  const dots = [C.chat, C.ask, C.agent, C.combo];
  const totalW = dots.length * dotD + (dots.length - 1) * gap;
  let dx = PAGE_W / 2 - totalW / 2;
  dots.forEach((clr) => {
    s.addShape("ellipse", { x: dx, y: dotY, w: dotD, h: dotD, fill: { color: clr }, line: { type: "none" } });
    dx += dotD + gap;
  });

  s.addText("Built on Spring AI · Claude · pgvector (RAG) · MCP", {
    x: 1.0, y: 4.9, w: PAGE_W - 2.0, h: 0.35,
    fontFace: FONT_BODY, fontSize: 12.5, color: MUTED_ON_DARK, align: "center", margin: 0,
  });

  s.addText("Technical Overview", {
    x: 1.0, y: 6.55, w: PAGE_W - 2.0, h: 0.35,
    fontFace: FONT_BODY, fontSize: 13, bold: true, color: TEXT_LIGHT, align: "center", margin: 0,
  });
  s.addText("August 15, 2026", {
    x: 1.0, y: 6.87, w: PAGE_W - 2.0, h: 0.3,
    fontFace: FONT_BODY, fontSize: 11, color: MUTED_ON_DARK, align: "center", margin: 0,
  });
}

// =========================================================
// 2. GOAL
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Purpose", "A natural-language interface to internal documents and data", false);

  card(s, MARGIN_X, 1.7, 5.7, 4.9);
  s.addText("What this system does", {
    x: MARGIN_X + 0.35, y: 2.0, w: 5.0, h: 0.4,
    fontFace: FONT_BODY, fontSize: 13, bold: true, color: MUTED, margin: 0,
  });
  s.addText(
    "A Spring Boot application that answers natural-language questions about an organization's own documents — answered by Claude, grounded via retrieval-augmented generation against pgvector, and extended with MCP so it can also read files live and query the database directly.",
    {
      x: MARGIN_X + 0.35, y: 2.5, w: 5.0, h: 2.6,
      fontFace: FONT_BODY, fontSize: 14.5, color: TEXT_DARK, lineSpacingMultiple: 1.32, margin: 0,
    }
  );

  const rightX = MARGIN_X + 6.05;
  const items = [
    ["LLM integration", "Production Anthropic API integration via Spring AI.", VIOLET],
    ["Grounded retrieval", "Answers are backed by the organization's own documents.", C.ask],
    ["Agentic tool-calling", "Claude can read a live file or query the database, not only pre-indexed content.", C.agent],
    ["Documented decisions", "Every technology choice is made and explained, with tradeoffs recorded.", CORAL],
  ];
  let iy = 1.7;
  items.forEach(([h, d, clr]) => {
    // Icon is sized and centered to match the title line (y=iy-0.02, h=0.35)
    // exactly -- the previous 0.5in icon extended 0.17in below the title,
    // overlapping the description text starting at y=iy+0.33.
    iconCircle(s, rightX + 0.05, iy - 0.045, 0.4, clr, "✓", { fontSize: 13 });
    s.addText(h, {
      x: rightX + 0.68, y: iy - 0.02, w: 5.4, h: 0.35,
      fontFace: FONT_BODY, fontSize: 14.5, bold: true, color: TEXT_DARK, margin: 0,
    });
    s.addText(d, {
      x: rightX + 0.68, y: iy + 0.33, w: 5.4, h: 0.7,
      fontFace: FONT_BODY, fontSize: 11.5, color: MUTED, margin: 0, lineSpacingMultiple: 1.2,
    });
    iy += 1.18;
  });

  footer(s, false);
}

// =========================================================
// 3. ARCHITECTURE
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Architecture at a Glance", "Everything runs locally except the Claude API call itself", false);

  const MARGIN_X2 = 0.6;
  const CONTENT_W2 = 12.13;
  const old_boxW = 2.05, old_gapW = 0.55;
  const old_total = 5 * old_boxW + 4 * old_gapW;
  const factor = CONTENT_W2 / old_total;
  const boxW = old_boxW * factor, gapW = old_gapW * factor;

  const boxY = 2.4, boxH = 1.15;
  const boxes = [
    ["Your docs", "6 formats", MUTED],
    ["Ingest + embed", "local Ollama", VIOLET],
    ["pgvector", "Postgres", C.ask],
    ["Similarity search", "top-K chunks", C.agent],
    ["Claude", "Anthropic API", CORAL],
  ];
  let bx = MARGIN_X2;
  boxes.forEach(([h, sub, clr], i) => {
    card(s, bx, boxY, boxW, boxH, { fill: "FFFFFF", line: clr });
    s.addText(h, {
      x: bx, y: boxY + 0.18, w: boxW, h: 0.4, align: "center",
      fontFace: FONT_BODY, fontSize: 13, bold: true, color: TEXT_DARK, margin: 0,
    });
    s.addText(sub, {
      x: bx, y: boxY + 0.6, w: boxW, h: 0.4, align: "center",
      fontFace: FONT_BODY, fontSize: 10.5, color: clr, margin: 0,
    });
    if (i < boxes.length - 1) {
      s.addText("→", {
        x: bx + boxW, y: boxY, w: gapW, h: boxH, align: "center", valign: "middle",
        fontFace: FONT_BODY, fontSize: 20, color: MUTED, margin: 0,
      });
    }
    bx += boxW + gapW;
  });

  const claudeBox = boxes.length - 1;
  const claudeX = MARGIN_X2 + claudeBox * (boxW + gapW);
  const claudeRight = claudeX + boxW;

  // Two MCP server boxes sit side by side beneath the pipeline's right end,
  // both feeding up into Claude (the box that reasons about which tool to
  // call). The filesystem box's right edge aligns with Claude's; the
  // Postgres box sits immediately to its left with a matching gap.
  const mcpW = 2.6, mcpGap = 0.3;
  const mcpX = claudeRight - mcpW;
  const mcpPgX = mcpX - mcpGap - mcpW;

  card(s, mcpX, 4.35, mcpW, 1.0, { fill: "FFFFFF", line: C.combo });
  s.addText("MCP filesystem server", {
    x: mcpX, y: 4.53, w: mcpW, h: 0.35, align: "center",
    fontFace: FONT_BODY, fontSize: 12, bold: true, color: TEXT_DARK, margin: 0,
  });
  s.addText("live tool calls to read files", {
    x: mcpX, y: 4.87, w: mcpW, h: 0.35, align: "center",
    fontFace: FONT_BODY, fontSize: 10, color: C.combo, margin: 0,
  });
  s.addText("↑", {
    x: mcpX + mcpW / 2 - 0.15, y: 3.55, w: 0.3, h: 0.8, align: "center", valign: "middle",
    fontFace: FONT_BODY, fontSize: 18, color: MUTED, margin: 0,
  });

  card(s, mcpPgX, 4.35, mcpW, 1.0, { fill: "FFFFFF", line: C.ingest });
  s.addText("MCP Postgres server", {
    x: mcpPgX, y: 4.53, w: mcpW, h: 0.35, align: "center",
    fontFace: FONT_BODY, fontSize: 12, bold: true, color: TEXT_DARK, margin: 0,
  });
  s.addText("read-only SQL, launched via uvx", {
    x: mcpPgX, y: 4.87, w: mcpW, h: 0.35, align: "center",
    fontFace: FONT_BODY, fontSize: 10, color: C.ingest, margin: 0,
  });
  s.addText("↑", {
    x: mcpPgX + mcpW / 2 - 0.15, y: 3.55, w: 0.3, h: 0.8, align: "center", valign: "middle",
    fontFace: FONT_BODY, fontSize: 18, color: MUTED, margin: 0,
  });

  card(s, MARGIN_X2, 5.75, CONTENT_W2, 0.95, { fill: "F6F4FF", line: "E4DEFF", shadow: false });
  s.addText(
    "Embeddings are free and local (Ollama has no per-token cost) — you only pay Anthropic for chat/reasoning tokens.",
    {
      x: MARGIN_X2 + 0.3, y: 5.75, w: CONTENT_W2 - 0.6, h: 0.95, valign: "middle",
      fontFace: FONT_BODY, fontSize: 14, italic: true, color: TEXT_DARK, margin: 0,
    }
  );

  footer(s, false);
}

// =========================================================
// 4. TECH STACK
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Tech Stack", "Every piece verified against the real, current version -- not assumed", false);

  const stack = [
    ["J21", "Java 21", VIOLET],
    ["SB", "Spring Boot 3.5.15", VIOLET],
    ["AI", "Spring AI 1.1.8", VIOLET],
    ["SEC", "Spring Security 6.5", VIOLET],
    ["C", "Claude Sonnet 5", CORAL],
    ["OL", "Ollama + nomic-embed-text", C.ask],
    ["PG", "Postgres + pgvector", C.ask],
    ["MCP", "MCP: filesystem + Postgres servers", C.combo],
    ["SW", "springdoc / Swagger UI", C.agent],
    ["UI", "Hand-built web UI", C.agent],
  ];

  const cols = 5, cardW = 2.25, cardH = 1.7, gx = 0.22, gy = 0.3;
  const startX = MARGIN_X + (CONTENT_W - (cols * cardW + (cols - 1) * gx)) / 2;
  stack.forEach(([abbr, label, clr], i) => {
    const col = i % cols, row = Math.floor(i / cols);
    const x = startX + col * (cardW + gx);
    const y = 1.95 + row * (cardH + gy);
    card(s, x, y, cardW, cardH);
    iconCircle(s, x + (cardW - 0.6) / 2, y + 0.22, 0.6, clr, abbr, { fontSize: abbr.length > 2 ? 10 : 13 });
    s.addText(label, {
      x: x + 0.12, y: y + 0.95, w: cardW - 0.24, h: 0.65, align: "center",
      fontFace: FONT_BODY, fontSize: 11, bold: true, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.12,
    });
  });

  footer(s, false);
}

// =========================================================
// 5 & 6. KEY ENGINEERING DECISIONS (split across 2 slides)
// =========================================================
const decisions = [
  ["Spring AI 1.1.8, not 2.0.0", "Spring AI 2.0.0 requires Spring Boot 4.x, a newer combination not yet adopted here. The 1.1.8 / Spring Boot 3.5.15 pairing was verified against the published release.", VIOLET],
  ["RetrievalAugmentationAdvisor, not QuestionAnswerAdvisor", "Both are available in Spring AI 1.1.8. RetrievalAugmentationAdvisor (built on VectorStoreDocumentRetriever) is the current, more modular RAG pipeline, provided by the spring-ai-rag dependency.", C.ask],
  ["Local Ollama embeddings, not a hosted API", "Anthropic has no embeddings endpoint. Voyage AI is the recommended hosted partner; local Ollama keeps ingestion free and offline, at the cost of requiring Ollama to run locally.", C.agent],
  ["Postgres + pgvector, not a managed vector DB", "Cost, control, and reuse of infrastructure already in place, instead of Pinecone/Weaviate. pgvector scales in production -- Supabase's vector offering is built on it.", C.combo],
];
const decisions2 = [
  ["Explicit chat/embedding model selection", "spring.ai.model.chat=anthropic + spring.ai.model.embedding=ollama in application.yml -- without it, having both starters on the classpath creates two competing ChatModel beans.", VIOLET],
  ["Multi-format document ingestion", "Prose formats (.md, .txt, .pdf, .docx) are token-chunked; tabular formats (.csv, .xlsx) are chunked one row at a time so structured records stay intact.", CORAL],
  ["Port configuration", "A native Postgres installation already occupied port 5432 on the reference machine. The container is mapped to host port 5434 to avoid the conflict.", C.ask],
  ["Postgres MCP Pro, not the official server-postgres", "The official @modelcontextprotocol/server-postgres is archived with a known read-only bypass via COMMIT/ROLLBACK. Postgres MCP Pro's restricted mode parses SQL before execution and rejects transaction-control statements.", C.ingest],
];

function decisionsSlide(title, subtitle, items) {
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, title, subtitle, false);

  // cardH/gap sized so 4 items end by y=6.86, leaving a real gap above the
  // footer at y=7.14 -- the previous 1.25/0.2 pairing put the 4th card's
  // bottom at 7.45, overlapping the footer text (including PROJECT_NAME).
  const cardH = 1.14;
  const cardGap = 0.15;
  let y = 1.85;
  items.forEach(([h, d, clr]) => {
    card(s, MARGIN_X, y, CONTENT_W, cardH);
    // Icon is vertically centered on the title line specifically (not the
    // whole card) -- sized to fit the title-to-description gap so it never
    // overlaps the description text starting at y+0.5.
    iconCircle(s, MARGIN_X + 0.32, y + 0.115, 0.4, clr, "→", { fontSize: 13 });
    s.addText(h, {
      x: MARGIN_X + 1.1, y: y + 0.14, w: CONTENT_W - 1.4, h: 0.35,
      fontFace: FONT_BODY, fontSize: 14, bold: true, color: TEXT_DARK, margin: 0,
    });
    s.addText(d, {
      x: MARGIN_X + 1.1, y: y + 0.5, w: CONTENT_W - 1.4, h: cardH - 0.6,
      fontFace: FONT_BODY, fontSize: 11, color: MUTED, margin: 0, lineSpacingMultiple: 1.2,
    });
    y += cardH + cardGap;
  });

  footer(s, false);
}
decisionsSlide("Key Engineering Decisions (1 / 2)", "Each decision reflects the current, verified state of the underlying libraries", decisions);
decisionsSlide("Key Engineering Decisions (2 / 2)", "Configuration correctness, environment compatibility, and MCP server selection", decisions2);

// =========================================================
// 7 & 8. THE ENDPOINTS (split across 2 slides -- 9 endpoints now)
// =========================================================
const endpoints1 = [
  ["GET", "/chat", "Plain Claude call — sanity check for the API key and Spring AI wiring, no RAG/MCP.", C.chat],
  ["POST", "/ingest", "Chunks and embeds a file or folder already on the server into pgvector via local Ollama.", C.ingest],
  ["POST", "/ingest/upload", "Same pipeline, but for a file uploaded directly from the caller's machine (multipart, up to 20MB).", C.ingest],
  ["GET", "/ask", "RAG: retrieves the closest chunks from pgvector and answers grounded in them.", C.ask],
  ["GET", "/ask/preview", "Runs the same retrieval as /ask and returns the matched chunks directly -- no Claude call.", C.ask],
];
const endpoints2 = [
  ["GET", "/agent", "MCP tool-calling: Claude can read a live file or run a read-only SQL query, not just embedded content.", C.agent],
  ["GET", "/agent/tools", "Lists every tool both connected MCP servers expose -- no Claude call.", C.agent],
  ["POST", "/agent/tools/{name}", "Invokes a named MCP tool directly with JSON arguments, bypassing Claude entirely.", C.agent],
  ["GET", "/rag-agent", "Combined: grounded retrieval and live tool access (filesystem + database) on one ChatClient.", C.combo],
];

function endpointsSlide(title, subtitle, eps) {
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, title, subtitle, false);

  let y = 1.85;
  // rowH/gap sized so 5 rows (the max, on the first Endpoints slide) end by
  // y=6.86, leaving a real gap above the footer at y=7.14 -- the previous
  // 0.98/0.16 pairing put the 5th row's bottom at 7.39, overlapping the
  // footer text (including PROJECT_NAME). Description height shrinks to
  // match, with badge/path positions unchanged since they sit near the top.
  const rowH = 0.89;
  const rowGap = 0.14;
  eps.forEach(([method, path, desc, clr]) => {
    card(s, MARGIN_X, y, CONTENT_W, rowH);
    // Badge is vertically centered on the path text (y+0.1, h=0.4) specifically,
    // not the full row -- keeps it clear of the description starting at y+0.5.
    const badgeY = y + 0.11;
    s.addShape("roundRect", {
      x: MARGIN_X + 0.25, y: badgeY, w: 0.72, h: 0.38, rectRadius: 0.06,
      fill: { color: clr }, line: { type: "none" },
    });
    s.addText(method, {
      x: MARGIN_X + 0.25, y: badgeY, w: 0.72, h: 0.38, align: "center", valign: "middle",
      fontFace: FONT_BODY, fontSize: 10, bold: true, color: "FFFFFF", margin: 0,
    });
    s.addText(path, {
      x: MARGIN_X + 1.15, y: y + 0.1, w: 3.1, h: 0.4,
      fontFace: "Courier New", fontSize: 14, bold: true, color: TEXT_DARK, margin: 0,
    });
    s.addText(desc, {
      x: MARGIN_X + 1.15, y: y + 0.5, w: CONTENT_W - 1.5, h: 0.35,
      fontFace: FONT_BODY, fontSize: 10.5, color: MUTED, margin: 0, lineSpacingMultiple: 1.15,
    });
    y += rowH + rowGap;
  });

  footer(s, false);
}
endpointsSlide("The Endpoints (1 / 2)", "Nine endpoints total -- ingest and retrieval", endpoints1);
endpointsSlide("The Endpoints (2 / 2)", "Tool-calling via MCP -- filesystem and Postgres", endpoints2);

// =========================================================
// 9. GROUNDING BEHAVIOR
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "How Grounding Works", "Behavior when retrieval finds nothing relevant", false);

  s.addText("Question comes in → embedded → similarity search against pgvector", {
    x: MARGIN_X, y: 1.85, w: CONTENT_W, h: 0.4,
    fontFace: FONT_BODY, fontSize: 13.5, color: TEXT_DARK, margin: 0,
  });

  const colW = (CONTENT_W - 0.5) / 2;
  card(s, MARGIN_X, 2.5, colW, 3.9, { fill: "F2FBF8", line: C.ask });
  iconCircle(s, MARGIN_X + 0.35, 2.85, 0.55, C.ask, "✓", { fontSize: 18 });
  s.addText("Relevant chunks found", {
    x: MARGIN_X + 1.1, y: 2.9, w: colW - 1.4, h: 0.45,
    fontFace: FONT_BODY, fontSize: 15, bold: true, color: TEXT_DARK, margin: 0,
  });
  s.addText(
    "Matched chunks are injected into the prompt as context. Claude answers using that context, producing a grounded response.",
    {
      x: MARGIN_X + 0.35, y: 3.6, w: colW - 0.7, h: 1.5,
      fontFace: FONT_BODY, fontSize: 12.5, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.3,
    }
  );

  const rightX = MARGIN_X + colW + 0.5;
  card(s, rightX, 2.5, colW, 3.9, { fill: "FFF6F6", line: "FF9B9B" });
  iconCircle(s, rightX + 0.35, 2.85, 0.55, "E85C5C", "×", { fontSize: 20 });
  s.addText("Nothing relevant retrieved", {
    x: rightX + 1.1, y: 2.9, w: colW - 1.4, h: 0.45,
    fontFace: FONT_BODY, fontSize: 15, bold: true, color: TEXT_DARK, margin: 0,
  });
  s.addText(
    "RetrievalAugmentationAdvisor's default ContextualQueryAugmenter has allowEmptyContext=false — Claude is told to say it doesn't have enough information, instead of answering from its own training data.",
    {
      x: rightX + 0.35, y: 3.6, w: colW - 0.7, h: 1.5,
      fontFace: FONT_BODY, fontSize: 12.5, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.3,
    }
  );

  footer(s, false);
}

// =========================================================
// 10. MCP TOOL-CALL WALKTHROUGH
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "How an MCP Tool Call Is Invoked", "The mechanics of a single tool-calling request, across either connected server", false);

  const steps = [
    ["Claude decides a tool is needed", "Reasoning over the question, not a hardcoded rule"],
    ["Returns a tool_use block", "Instead of a plain text answer"],
    ["SyncMcpToolCallbackProvider executes it", "Filesystem server (npx) or Postgres server (uvx), whichever tool was chosen"],
    ["Tool result returns to the conversation", "Live file content or SQL query result, as a message"],
    ["Claude produces the final answer", "Using that live content, not a cached embedding"],
  ];

  const n = steps.length;
  const boxW = (CONTENT_W - (n - 1) * 0.42) / n;
  let x = MARGIN_X;
  const y = 2.6, h = 2.7;
  steps.forEach(([h1, h2], i) => {
    card(s, x, y, boxW, h);
    iconCircle(s, x + boxW / 2 - 0.28, y - 0.35, 0.56, VIOLET, String(i + 1), { fontSize: 18 });
    s.addText(h1, {
      x: x + 0.15, y: y + 0.3, w: boxW - 0.3, h: 1.2, align: "center",
      fontFace: FONT_BODY, fontSize: 11.5, bold: true, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.12,
    });
    s.addText(h2, {
      x: x + 0.15, y: y + 1.55, w: boxW - 0.3, h: 1.05, align: "center",
      fontFace: FONT_BODY, fontSize: 9, color: MUTED, margin: 0, lineSpacingMultiple: 1.15,
    });
    if (i < n - 1) {
      s.addText("→", {
        x: x + boxW, y: y + h / 2 - 0.3, w: 0.42, h: 0.6, align: "center", valign: "middle",
        fontFace: FONT_BODY, fontSize: 18, color: MUTED, margin: 0,
      });
    }
    x += boxW + 0.42;
  });

  footer(s, false);
}

// =========================================================
// 11. USER EXPERIENCE
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Two Ways to Interact", "A hand-built chat UI, and a full API surface for integration", false);

  const colW = (CONTENT_W - 0.5) / 2;

  card(s, MARGIN_X, 1.85, colW, 4.6);
  s.addText("Web UI  —  /", {
    x: MARGIN_X + 0.35, y: 2.1, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 16, bold: true, color: TEXT_DARK, margin: 0,
  });
  s.addText("Single self-contained index.html — no framework, no build step.", {
    x: MARGIN_X + 0.35, y: 2.5, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 11.5, italic: true, color: MUTED, margin: 0,
  });
  const dots = [["Chat", C.chat], ["Ask (RAG)", C.ask], ["Agent (MCP)", C.agent], ["RAG + Agent", C.combo]];
  let dy = 3.05;
  dots.forEach(([label, clr]) => {
    s.addShape("ellipse", { x: MARGIN_X + 0.35, y: dy + 0.05, w: 0.16, h: 0.16, fill: { color: clr }, line: { type: "none" } });
    s.addText(label, {
      x: MARGIN_X + 0.65, y: dy - 0.06, w: colW - 1.0, h: 0.35,
      fontFace: FONT_BODY, fontSize: 12.5, bold: true, color: TEXT_DARK, margin: 0,
    });
    dy += 0.42;
  });
  s.addText(
    "Each mode carries its own color through the header, the send button, and every message it answers — plus a dark/light theme toggle and an ingest modal.",
    {
      x: MARGIN_X + 0.35, y: dy + 0.15, w: colW - 0.7, h: 1.5,
      fontFace: FONT_BODY, fontSize: 12, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.3,
    }
  );

  const rightX = MARGIN_X + colW + 0.5;
  card(s, rightX, 1.85, colW, 4.6);
  s.addText("Swagger UI  —  /swagger-ui/index.html", {
    x: rightX + 0.35, y: 2.1, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 16, bold: true, color: TEXT_DARK, margin: 0,
  });
  s.addText("Powered by springdoc-openapi.", {
    x: rightX + 0.35, y: 2.5, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 11.5, italic: true, color: MUTED, margin: 0,
  });
  const swaggerPoints = [
    "Every endpoint documented with a real description",
    "Authorize button -- enter credentials once, every “Try it out” call carries them",
    "A real file-picker widget for POST /ingest/upload, not a path field",
    "Raw OpenAPI JSON for anyone integrating against it",
  ];
  let sy = 3.15;
  swaggerPoints.forEach((t) => {
    iconCircle(s, rightX + 0.35, sy, 0.3, VIOLET, "✓", { fontSize: 11 });
    s.addText(t, {
      x: rightX + 0.8, y: sy - 0.04, w: colW - 1.2, h: 0.55,
      fontFace: FONT_BODY, fontSize: 12, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.2,
    });
    sy += 0.68;
  });

  footer(s, false);
}

// =========================================================
// 12. LIVE UI SCREENSHOTS
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "The Web UI, Live", "Real screenshots of the running application -- not mockups", false);

  const imgW = (CONTENT_W - 0.4) / 2;
  const imgH = imgW / (1280 / 800);
  const imgY = 1.85;

  const shots = [
    ["ask-rag-dark.png", "Ask (RAG) mode", C.ask],
    ["rag-agent-dark.png", "RAG + Agent mode", C.combo],
  ];
  shots.forEach(([file, caption, clr], i) => {
    const x = MARGIN_X + i * (imgW + 0.4);
    card(s, x - 0.08, imgY - 0.08, imgW + 0.16, imgH + 0.16, { fill: "FFFFFF", line: BORDER });
    s.addImage({ data: loadImageDataUri(file), x, y: imgY, w: imgW, h: imgH });
    s.addText(caption, {
      x, y: imgY + imgH + 0.15, w: imgW, h: 0.35, align: "center",
      fontFace: FONT_BODY, fontSize: 12.5, bold: true, color: clr, margin: 0,
    });
  });

  s.addText(
    "Each mode's accent color carries through the header, the active sidebar item, and the send button -- captured directly from a locally running instance.",
    {
      x: MARGIN_X, y: imgY + imgH + 0.6, w: CONTENT_W, h: 0.4, align: "center",
      fontFace: FONT_BODY, fontSize: 11, italic: true, color: MUTED, margin: 0,
    }
  );

  footer(s, false);
}

// =========================================================
// 13. SECURITY
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Security", "Every endpoint requires authentication -- no anonymous access anywhere", false);

  const colW = (CONTENT_W - 0.5) / 2;

  card(s, MARGIN_X, 1.85, colW, 4.6);
  s.addText("Enforcement", {
    x: MARGIN_X + 0.35, y: 2.1, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 16, bold: true, color: TEXT_DARK, margin: 0,
  });
  const enforcement = [
    "HTTP Basic auth via a Spring Security filter chain -- Swagger UI, the web UI, and every REST endpoint alike",
    "Passwords hashed with BCryptPasswordEncoder, never stored or compared in plaintext",
    "Credentials bound from app.security.* via a validated @ConfigurationProperties record",
    "Startup warning logged if the local-development default password is still in use",
    "CSRF disabled deliberately -- a stateless, credential-per-request API with no session cookies has a different threat model",
  ];
  let ey = 2.75;
  enforcement.forEach((t) => {
    iconCircle(s, MARGIN_X + 0.35, ey, 0.3, VIOLET, "✓", { fontSize: 11 });
    s.addText(t, {
      x: MARGIN_X + 0.8, y: ey - 0.06, w: colW - 1.2, h: 0.7,
      fontFace: FONT_BODY, fontSize: 11.5, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.25,
    });
    ey += 0.72;
  });

  const rightX = MARGIN_X + colW + 0.5;
  card(s, rightX, 1.85, colW, 4.6, { fill: "F6F4FF", line: "E4DEFF", shadow: false });
  s.addText("Configuration", {
    x: rightX + 0.35, y: 2.1, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 16, bold: true, color: TEXT_DARK, margin: 0,
  });
  s.addText("Two environment variables, no secrets committed to the repo:", {
    x: rightX + 0.35, y: 2.5, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 11.5, italic: true, color: MUTED, margin: 0,
  });
  const envRows = [
    ["APP_SECURITY_USERNAME", "admin"],
    ["APP_SECURITY_PASSWORD", "changeme"],
  ];
  let evy = 3.05;
  envRows.forEach(([k, v]) => {
    s.addText(k, {
      x: rightX + 0.35, y: evy, w: colW - 0.7, h: 0.35,
      fontFace: "Courier New", fontSize: 13, bold: true, color: TEXT_DARK, margin: 0,
    });
    s.addText("local default: " + v, {
      x: rightX + 0.35, y: evy + 0.34, w: colW - 0.7, h: 0.3,
      fontFace: FONT_BODY, fontSize: 10.5, color: MUTED, margin: 0,
    });
    evy += 0.85;
  });
  card(s, rightX + 0.35, evy + 0.1, colW - 1.0, 0.85, { fill: "1B1A24", shadow: false });
  s.addText('curl -u admin:changeme "http://localhost:8080/chat?q=hi"', {
    x: rightX + 0.55, y: evy + 0.1, w: colW - 1.4, h: 0.85, valign: "middle",
    fontFace: "Courier New", fontSize: 10.5, color: "9FF7D6", margin: 0,
  });

  footer(s, false);
}

// =========================================================
// 14 & 15. TESTING (split across 2 slides -- 42 tests now)
// =========================================================
const testing1 = [
  ["Document format readers", "Plain JUnit 5 against real temp files -- CSV, XLSX, Markdown parsing and row/metadata extraction, no mocking", VIOLET],
  ["DocumentIngestor", "Mockito -- verifies the Strategy pattern routes each file to the reader that supports it, and rejects unsupported formats", C.ingest],
  ["RagProperties", "Validates the compact-constructor bounds checking on app.rag.* configuration", C.ask],
  ["GlobalExceptionHandler", "Confirms IllegalArgumentException maps to 400 with the real message, and unexpected errors map to 500 without leaking internals", C.agent],
];
const testing2 = [
  ["ChatController + SecurityConfig", "@WebMvcTest slice with spring-security-test -- no credentials or wrong password get 401, a valid request reaches the controller", C.combo],
  ["IngestController (/ingest/upload)", "Asserts an unauthenticated upload is rejected, a valid upload is written to a temp file preserving its extension, and a filename-less upload is a bad request", C.ingest],
  ["RagController (/ask/preview)", "Asserts the mocked VectorStoreDocumentRetriever results are returned as JSON without going through ChatClient at all", C.ask],
  ["AgentController (/agent/tools)", "Asserts tool metadata is listed correctly, a named tool is invoked directly, and a failed tool call maps to 400 with the real reason", C.agent],
];

function testingSlide(title, subtitle, rows) {
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, title, subtitle, false);

  let y = 1.85;
  // rowH/gap sized so 4 rows end by y=6.86, matching the same footer-safe
  // margin as decisionsSlide -- the previous 1.15/0.2 pairing left only
  // 0.09in above the footer at y=7.14, uncomfortably tight.
  const rowH = 1.14;
  const rowGap = 0.15;
  rows.forEach(([area, desc, clr]) => {
    card(s, MARGIN_X, y, CONTENT_W, rowH);
    // Icon aligns with the title (area) text's own box exactly, both h=0.4 --
    // keeps it clear of the description starting at y+0.52.
    iconCircle(s, MARGIN_X + 0.32, y + 0.14, 0.4, clr, "✓", { fontSize: 14 });
    s.addText(area, {
      x: MARGIN_X + 1.0, y: y + 0.14, w: CONTENT_W - 1.35, h: 0.4,
      fontFace: FONT_BODY, fontSize: 13.5, bold: true, color: TEXT_DARK, margin: 0,
    });
    s.addText(desc, {
      x: MARGIN_X + 1.0, y: y + 0.52, w: CONTENT_W - 1.35, h: 0.55,
      fontFace: FONT_BODY, fontSize: 11, color: MUTED, margin: 0, lineSpacingMultiple: 1.22,
    });
    y += rowH + rowGap;
  });

  footer(s, false);
}
testingSlide("Testing (1 / 2)", "mvn test -- 42 unit tests, no Docker, Ollama, or API key required", testing1);
testingSlide("Testing (2 / 2)", "Every new endpoint added this build has matching test coverage", testing2);

// =========================================================
// 16. LOCAL vs PRODUCTION
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Local Build vs. Real Production Infra", "What actually changes shape when this ships", false);

  const rows = [
    ["Postgres + pgvector", "Docker container on a laptop", "Managed Postgres (RDS / Cloud SQL) with pgvector enabled — same tech, just hosted"],
    ["Embedding model", "Local Ollama (nomic-embed-text)", "Hosted API (Voyage/OpenAI) or self-hosted on GPU infra — code barely changes, it's behind EmbeddingModel"],
    ["Document ingestion", "Synchronous — call /ingest, wait", "Async, queue-triggered pipeline (SQS/Kafka) — only query-time embedding stays synchronous"],
    ["Claude API calls", "Already a real hosted call", "Unchanged — nothing to swap"],
  ];

  const colLabelW = 2.5, colLocalW = 4.35, colProdW = CONTENT_W - colLabelW - colLocalW - 0.6;
  let y = 1.85;
  s.addText("Local", { x: MARGIN_X + colLabelW + 0.3, y: 1.85, w: colLocalW, h: 0.35, fontFace: FONT_BODY, fontSize: 12, bold: true, color: MUTED, margin: 0 });
  s.addText("Production", { x: MARGIN_X + colLabelW + colLocalW + 0.6, y: 1.85, w: colProdW, h: 0.35, fontFace: FONT_BODY, fontSize: 12, bold: true, color: VIOLET, margin: 0 });
  y = 2.25;
  const rowH = 1.05;
  rows.forEach(([label, local, prod]) => {
    card(s, MARGIN_X, y, CONTENT_W, rowH, { shadow: false });
    s.addText(label, {
      x: MARGIN_X + 0.25, y, w: colLabelW - 0.25, h: rowH, valign: "middle",
      fontFace: FONT_BODY, fontSize: 13, bold: true, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.15,
    });
    s.addText(local, {
      x: MARGIN_X + colLabelW + 0.3, y, w: colLocalW, h: rowH, valign: "middle",
      fontFace: FONT_BODY, fontSize: 11.5, color: MUTED, margin: 0, lineSpacingMultiple: 1.2,
    });
    s.addText(prod, {
      x: MARGIN_X + colLabelW + colLocalW + 0.6, y, w: colProdW, h: rowH, valign: "middle",
      fontFace: FONT_BODY, fontSize: 11.5, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.2,
    });
    y += rowH + 0.16;
  });

  footer(s, false);
}

// =========================================================
// 17. FREQUENTLY ASKED QUESTIONS
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Frequently Asked Questions", "Common questions about this implementation", false);

  const qas = [
    ["Why pgvector over a managed vector store?", "Cost, control, and reuse of existing Postgres infrastructure — sufficient at this scale."],
    ["Why RetrievalAugmentationAdvisor rather than QuestionAnswerAdvisor?", "Both are available in Spring AI 1.1.8; RetrievalAugmentationAdvisor is the newer, more modular RAG pipeline."],
    ["What happens when retrieval returns nothing relevant?", "allowEmptyContext=false makes Claude decline gracefully instead of guessing an answer."],
    ["Why Postgres MCP Pro instead of the official server-postgres?", "The official package is archived with a known read-only bypass; restricted mode here actually parses and rejects unsafe SQL."],
  ];

  const colW = (CONTENT_W - 0.4) / 2, cardH = 2.15;
  qas.forEach(([q, a], i) => {
    const col = i % 2, row = Math.floor(i / 2);
    const x = MARGIN_X + col * (colW + 0.4);
    const y = 1.85 + row * (cardH + 0.3);
    card(s, x, y, colW, cardH);
    s.addText("Q", {
      x: x + 0.25, y: y + 0.2, w: 0.5, h: 0.5,
      fontFace: FONT_HEAD, fontSize: 20, bold: true, color: VIOLET, margin: 0,
    });
    s.addText(q, {
      x: x + 0.25, y: y + 0.68, w: colW - 0.5, h: 0.75,
      fontFace: FONT_BODY, fontSize: 13, bold: true, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.2,
    });
    s.addText(a, {
      x: x + 0.25, y: y + 1.4, w: colW - 0.5, h: 0.7,
      fontFace: FONT_BODY, fontSize: 11, color: MUTED, margin: 0, lineSpacingMultiple: 1.25,
    });
  });

  footer(s, false);
}

// =========================================================
// 18. GITHUB / HANDOVER
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: DARK_BG };
  titleBlock(s, "Source Control & Handover", "A clean, standalone repository -- ready to clone and run", true);

  card(s, MARGIN_X, 1.9, CONTENT_W, 1.1, { fill: "1F2230", line: "343850", shadow: false });
  s.addText("github.com/deveshverma3/freight-source-ai-rag-assistant", {
    x: MARGIN_X + 0.4, y: 1.9, w: CONTENT_W - 0.8, h: 1.1, valign: "middle",
    fontFace: "Courier New", fontSize: 16, bold: true, color: "9FF7D6", margin: 0,
  });

  const colW = (CONTENT_W - 0.5) / 2;
  const leftY = 3.35;
  card(s, MARGIN_X, leftY, colW, 3.1, { fill: "1F2230", line: "343850", shadow: false });
  s.addText("What's in the repo", {
    x: MARGIN_X + 0.35, y: leftY + 0.25, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 15, bold: true, color: TEXT_LIGHT, margin: 0,
  });
  const repoPoints = [
    "README with architecture + sequence diagrams, config reference, security and testing sections",
    ".env.example -- every required variable documented, zero secrets committed",
    "docker-compose.yml for the pgvector container, ready on first clone",
    "42 passing unit tests (mvn test) -- no external services needed to run them",
  ];
  let rpy = leftY + 0.75;
  repoPoints.forEach((t) => {
    iconCircle(s, MARGIN_X + 0.35, rpy, 0.28, VIOLET, "✓", { fontSize: 10 });
    s.addText(t, {
      x: MARGIN_X + 0.78, y: rpy - 0.07, w: colW - 1.15, h: 0.6,
      fontFace: FONT_BODY, fontSize: 11, color: MUTED_ON_DARK, margin: 0, lineSpacingMultiple: 1.22,
    });
    rpy += 0.58;
  });

  const rightX = MARGIN_X + colW + 0.5;
  card(s, rightX, leftY, colW, 3.1, { fill: "1F2230", line: "343850", shadow: false });
  s.addText("Get it running", {
    x: rightX + 0.35, y: leftY + 0.25, w: colW - 0.7, h: 0.4,
    fontFace: FONT_BODY, fontSize: 15, bold: true, color: TEXT_LIGHT, margin: 0,
  });
  const cmds = [
    "ollama pull nomic-embed-text",
    "docker compose up -d",
    "export ANTHROPIC_API_KEY=sk-ant-...",
    "mvn spring-boot:run",
  ];
  let cy = leftY + 0.72;
  cmds.forEach((cmd, i) => {
    s.addText(String(i + 1), {
      x: rightX + 0.35, y: cy, w: 0.3, h: 0.42, align: "center",
      fontFace: FONT_BODY, fontSize: 11, bold: true, color: MUTED_ON_DARK, margin: 0,
    });
    s.addText(cmd, {
      x: rightX + 0.72, y: cy, w: colW - 1.1, h: 0.42, valign: "middle",
      fontFace: "Courier New", fontSize: 12, color: "9FF7D6", margin: 0,
    });
    cy += 0.53;
  });
  s.addText("Four commands, start to running app.", {
    x: rightX + 0.35, y: cy + 0.05, w: colW - 0.7, h: 0.35,
    fontFace: FONT_BODY, fontSize: 10.5, italic: true, color: MUTED_ON_DARK, margin: 0,
  });

  footer(s, true);
}

// =========================================================
// 19. STATUS / NEXT STEPS
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: LIGHT_BG };
  titleBlock(s, "Project Status", "As of August 15, 2026 -- delivered and verified", false);

  card(s, MARGIN_X, 1.85, CONTENT_W, 4.6, { fill: "F2FBF8", line: C.ask });
  s.addText("Delivered", { x: MARGIN_X + 0.35, y: 2.1, w: CONTENT_W - 0.7, h: 0.4, fontFace: FONT_BODY, fontSize: 17, bold: true, color: TEXT_DARK, margin: 0 });

  const done = [
    "Application scaffolded — 9 endpoints across ingest, retrieval, and MCP tool-calling",
    "Two MCP servers live: filesystem (npx) and read-only Postgres (uvx)",
    "Document ingestion: 6 formats, server-path and browser upload",
    "Web UI (color-coded modes) + Swagger UI with a working Authorize flow",
    "HTTP Basic authentication enforced on every endpoint",
    "42 unit tests, including coverage for every endpoint added this build",
    "Debug endpoints for retrieval and MCP calls -- no Anthropic key required",
    "README, prompt-engineering guide, and repository published",
  ];

  const gridColW = (CONTENT_W - 0.7 - 0.5) / 2;
  const startY = 2.75;
  const rowStep = 0.85;
  done.forEach((t, i) => {
    const col = i % 2, row = Math.floor(i / 2);
    const x = MARGIN_X + 0.35 + col * (gridColW + 0.5);
    const y = startY + row * rowStep;
    iconCircle(s, x, y, 0.26, C.ask, "✓", { fontSize: 10 });
    s.addText(t, {
      x: x + 0.4, y: y - 0.06, w: gridColW - 0.4, h: 0.72,
      fontFace: FONT_BODY, fontSize: 11.5, color: TEXT_DARK, margin: 0, lineSpacingMultiple: 1.2,
    });
  });

  footer(s, false);
}

// =========================================================
// 20. CLOSING
// =========================================================
{
  const s = pres.addSlide();
  s.background = { color: DARK_BG };

  iconCircle(s, PAGE_W / 2 - 0.45, 2.2, 0.9, CORAL, "✦", { fontSize: 24 });

  s.addText("Thank you", {
    x: 1.0, y: 3.4, w: PAGE_W - 2.0, h: 0.9,
    fontFace: FONT_HEAD, fontSize: 34, bold: true, color: TEXT_LIGHT, align: "center", margin: 0,
  });
  s.addText("Questions welcome.", {
    x: 1.0, y: 4.25, w: PAGE_W - 2.0, h: 0.5,
    fontFace: FONT_BODY, fontSize: 15, color: MUTED_ON_DARK, align: "center", margin: 0,
  });
  s.addText(PROJECT_NAME, {
    x: 1.0, y: 6.7, w: PAGE_W - 2.0, h: 0.35,
    fontFace: FONT_BODY, fontSize: 11, color: MUTED_ON_DARK, align: "center", margin: 0,
  });
}

pres.writeFile({ fileName: "spring-ai-claude-rag-mcp-overview.pptx" }).then(() => {
  console.log("written");
});
