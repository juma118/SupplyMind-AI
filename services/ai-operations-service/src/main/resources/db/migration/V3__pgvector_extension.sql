-- README §5 RAG design: supplier contracts, procurement policies, shipping agreements,
-- warehouse SOPs, product specs retrieved via pgvector. Spring AI's PgVectorStore would
-- also create this extension itself at startup (initialize-schema), but it's made explicit
-- here rather than left to that implicit behavior - same reasoning as every other schema
-- change in this codebase living in a tracked migration, not an app-managed side effect.
CREATE EXTENSION IF NOT EXISTS vector;
