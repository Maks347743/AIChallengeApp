"""
PR Review Script
----------------
1. Fetches PR diff + changed files via GitHub API
2. Runs inline RAG: chunks docs/review-rules.md, embeds via Jina, retrieves top-K relevant rules
3. Calls DeepSeek to generate a structured review
4. Posts the review as a PR comment (replaces previous AI review comment if any)
"""

import os
import re
import sys
import json
import time
import datetime

import httpx
import numpy as np

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

def _require_env(key: str) -> str:
    value = os.getenv(key)
    if not value:
        print(f"ERROR: Required environment variable '{key}' is not set.", file=sys.stderr)
        sys.exit(1)
    return value


GITHUB_TOKEN = _require_env("GITHUB_TOKEN")
DEEPSEEK_API_KEY = _require_env("DEEPSEEK_API_KEY")
JINA_API_KEY = _require_env("JINA_API_KEY")
REPO = _require_env("REPO")                  # e.g. "owner/repo-name"

_pr_number_raw = _require_env("PR_NUMBER")
try:
    PR_NUMBER = int(_pr_number_raw)
except ValueError:
    print(f"ERROR: PR_NUMBER must be an integer, got: '{_pr_number_raw}'", file=sys.stderr)
    sys.exit(1)

DOCS_DIR = "docs"
MAX_DIFF_CHARS = 50_000
TOP_K_CHUNKS = 3
AI_COMMENT_MARKER = "<!-- ai-pr-review -->"
OLD_REVIEWS_MARKER = "<!-- old-reviews -->"
VERSION_RE = re.compile(r"<!-- version:(\d+) -->")
TIMESTAMP_RE = re.compile(r"<!-- timestamp:([^>]+) -->")

GITHUB_API = "https://api.github.com"
DEEPSEEK_API = os.getenv("DEEPSEEK_API", "https://api.deepseek.com/v1/chat/completions")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
JINA_EMBEDDINGS_API = "https://api.jina.ai/v1/embeddings"
JINA_MODEL = os.getenv("JINA_MODEL", "jina-embeddings-v3")

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def github_headers() -> dict:
    return {
        "Authorization": f"Bearer {GITHUB_TOKEN}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }


def retry_request(func, retries: int = 3, backoff: float = 2.0):
    """Call func(), retrying up to `retries` times with exponential backoff."""
    last_exc = None
    for attempt in range(retries):
        try:
            return func()
        except Exception as exc:
            last_exc = exc
            if attempt < retries - 1:
                sleep_time = backoff ** attempt
                print(f"  Attempt {attempt + 1} failed: {exc}. Retrying in {sleep_time:.1f}s...")
                time.sleep(sleep_time)
    raise last_exc


# ---------------------------------------------------------------------------
# Step 1: Fetch PR data
# ---------------------------------------------------------------------------

def fetch_pr_data() -> dict:
    print(f"Fetching PR #{PR_NUMBER} metadata...")
    with httpx.Client(timeout=30) as client:
        resp = client.get(f"{GITHUB_API}/repos/{REPO}/pulls/{PR_NUMBER}", headers=github_headers())
        resp.raise_for_status()
        return resp.json()


def fetch_pr_files() -> list[dict]:
    print("Fetching changed files...")
    files = []
    page = 1
    with httpx.Client(timeout=30) as client:
        while True:
            resp = client.get(
                f"{GITHUB_API}/repos/{REPO}/pulls/{PR_NUMBER}/files",
                headers=github_headers(),
                params={"per_page": 100, "page": page},
            )
            resp.raise_for_status()
            batch = resp.json()
            if not batch:
                break
            files.extend(batch)
            page += 1
    return files


def fetch_pr_diff() -> str:
    print("Fetching PR diff...")
    headers = {**github_headers(), "Accept": "application/vnd.github.diff"}
    with httpx.Client(timeout=60) as client:
        resp = client.get(f"{GITHUB_API}/repos/{REPO}/pulls/{PR_NUMBER}", headers=headers)
        resp.raise_for_status()
        diff = resp.text
    if len(diff) > MAX_DIFF_CHARS:
        print(f"  Diff too large ({len(diff)} chars), truncating to {MAX_DIFF_CHARS}...")
        diff = diff[:MAX_DIFF_CHARS] + "\n\n[... diff truncated ...]"
    return diff


# ---------------------------------------------------------------------------
# Step 2: RAG — chunk docs, embed, retrieve top-K
# ---------------------------------------------------------------------------

def _chunk_markdown(content: str, source: str) -> list[dict]:
    """Split a markdown file into chunks by ## headings."""
    chunks = []
    parts = re.split(r"(?=^## )", content, flags=re.MULTILINE)
    for part in parts:
        part = part.strip()
        if not part or part.startswith("# "):
            continue
        heading_match = re.match(r"^## (.+)", part)
        heading = heading_match.group(1).strip() if heading_match else "General"
        chunks.append({"heading": heading, "source": source, "text": part})
    return chunks


def load_and_chunk_docs(docs_dir: str) -> list[dict]:
    """Load all .md files from docs_dir recursively and chunk by ## headings."""
    if not os.path.isdir(docs_dir):
        print(f"WARNING: Docs directory '{docs_dir}' not found. Proceeding without RAG context.", file=sys.stderr)
        return []

    all_chunks = []
    for root, _, files in os.walk(docs_dir):
        for filename in sorted(files):
            if not filename.endswith(".md"):
                continue
            filepath = os.path.join(root, filename)
            with open(filepath, encoding="utf-8") as f:
                content = f.read()
            rel_path = os.path.relpath(filepath, docs_dir)
            file_chunks = _chunk_markdown(content, source=rel_path)
            all_chunks.extend(file_chunks)
            print(f"  Loaded {len(file_chunks)} chunks from docs/{rel_path}")

    print(f"  Total: {len(all_chunks)} RAG chunks from {docs_dir}/")
    return all_chunks


def embed_texts(texts: list[str]) -> np.ndarray:
    """Embed a list of texts via Jina Embeddings API. Returns (N, D) float32 array."""
    headers = {
        "Authorization": f"Bearer {JINA_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {"model": JINA_MODEL, "input": texts}

    def do_request():
        with httpx.Client(timeout=60) as client:
            resp = client.post(JINA_EMBEDDINGS_API, headers=headers, json=payload)
            resp.raise_for_status()
            return resp.json()

    data = retry_request(do_request)
    vectors = [item["embedding"] for item in data["data"]]
    return np.array(vectors, dtype=np.float32)


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    """Compute cosine similarity between vector a (1, D) and matrix b (N, D)."""
    a_norm = a / (np.linalg.norm(a) + 1e-10)
    b_norms = b / (np.linalg.norm(b, axis=1, keepdims=True) + 1e-10)
    return (b_norms @ a_norm.T).flatten()


def retrieve_relevant_rules(chunks: list[dict], query: str, top_k: int = TOP_K_CHUNKS) -> list[dict]:
    print(f"  Embedding {len(chunks)} chunks + query via Jina...")
    all_texts = [c["text"] for c in chunks] + [query]
    all_embeddings = embed_texts(all_texts)

    chunk_embeddings = all_embeddings[:-1]
    query_embedding = all_embeddings[-1]

    scores = cosine_similarity(query_embedding, chunk_embeddings)
    top_indices = np.argsort(scores)[::-1][:top_k]

    results = []
    for idx in top_indices:
        results.append({**chunks[idx], "score": float(scores[idx])})
        print(f"    [{scores[idx]:.3f}] {chunks[idx]['heading']}")
    return results


def build_rag_context(relevant_chunks: list[dict]) -> str:
    parts = []
    for chunk in relevant_chunks:
        parts.append(f"### {chunk['heading']}\n{chunk['text']}")
    return "\n\n".join(parts)


# ---------------------------------------------------------------------------
# Step 3: Call DeepSeek for review
# ---------------------------------------------------------------------------

def call_deepseek(system_prompt: str, user_prompt: str) -> str:
    print("Calling DeepSeek API...")
    headers = {
        "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": DEEPSEEK_MODEL,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "max_tokens": 2000,
        "temperature": 0.1,
    }

    def do_request():
        with httpx.Client(timeout=120) as client:
            resp = client.post(DEEPSEEK_API, headers=headers, json=payload)
            resp.raise_for_status()
            return resp.json()

    data = retry_request(do_request)
    return data["choices"][0]["message"]["content"]


# ---------------------------------------------------------------------------
# Step 4: Post PR comment
# ---------------------------------------------------------------------------

def find_existing_ai_comment() -> dict | None:
    """Return existing AI review comment as {id, body}, or None."""
    page = 1
    with httpx.Client(timeout=30) as client:
        while True:
            resp = client.get(
                f"{GITHUB_API}/repos/{REPO}/issues/{PR_NUMBER}/comments",
                headers=github_headers(),
                params={"per_page": 100, "page": page},
            )
            resp.raise_for_status()
            comments = resp.json()
            if not comments:
                break
            for comment in comments:
                if AI_COMMENT_MARKER in comment.get("body", ""):
                    return {"id": comment["id"], "body": comment["body"]}
            page += 1
    return None


def _extract_version(body: str) -> int:
    m = VERSION_RE.search(body)
    return int(m.group(1)) if m else 1


def _extract_timestamp(body: str) -> str:
    m = TIMESTAMP_RE.search(body)
    return m.group(1) if m else "unknown"


def _extract_current_review(body: str) -> str:
    """Strip hidden markers and return the current review section (before old-reviews block)."""
    parts = body.split(AI_COMMENT_MARKER, 1)
    content = parts[1] if len(parts) > 1 else body
    content = VERSION_RE.sub("", content)
    content = TIMESTAMP_RE.sub("", content)
    if OLD_REVIEWS_MARKER in content:
        content = content[:content.index(OLD_REVIEWS_MARKER)]
    return content.strip()


def _extract_old_reviews(body: str) -> str:
    """Return the accumulated old-reviews section from the existing comment."""
    if OLD_REVIEWS_MARKER not in body:
        return ""
    return body[body.index(OLD_REVIEWS_MARKER) + len(OLD_REVIEWS_MARKER):].strip()


def post_or_update_comment(review_text: str, rag_footer: str) -> None:
    now = datetime.datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
    existing = find_existing_ai_comment()

    if existing:
        old_version = _extract_version(existing["body"])
        old_timestamp = _extract_timestamp(existing["body"])
        new_version = old_version + 1
        old_review_content = _extract_current_review(existing["body"])
        accumulated_old = _extract_old_reviews(existing["body"])

        old_block = (
            f"<details>\n"
            f"<summary>~~v{old_version} · {old_timestamp}~~</summary>\n\n"
            f"<del>\n\n"
            f"{old_review_content}\n\n"
            f"</del>\n\n"
            f"</details>"
        )
        all_old = f"{old_block}\n\n{accumulated_old}".strip()

        full_body = (
            f"{AI_COMMENT_MARKER}\n"
            f"<!-- version:{new_version} -->\n"
            f"<!-- timestamp:{now} -->\n\n"
            f"## AI Code Review — v{new_version}\n"
            f"_{now}_\n\n"
            f"{review_text}\n\n"
            f"---\n"
            f"_{rag_footer}_\n\n"
            f"{OLD_REVIEWS_MARKER}\n\n"
            f"{all_old}"
        )
        print(f"  Updating existing AI review comment #{existing['id']} → v{new_version}...")
        with httpx.Client(timeout=30) as client:
            resp = client.patch(
                f"{GITHUB_API}/repos/{REPO}/issues/comments/{existing['id']}",
                headers=github_headers(),
                json={"body": full_body},
            )
            resp.raise_for_status()
    else:
        full_body = (
            f"{AI_COMMENT_MARKER}\n"
            f"<!-- version:1 -->\n"
            f"<!-- timestamp:{now} -->\n\n"
            f"## AI Code Review — v1\n"
            f"_{now}_\n\n"
            f"{review_text}\n\n"
            f"---\n"
            f"_{rag_footer}_"
        )
        print("  Posting new AI review comment (v1)...")
        with httpx.Client(timeout=30) as client:
            resp = client.post(
                f"{GITHUB_API}/repos/{REPO}/issues/{PR_NUMBER}/comments",
                headers=github_headers(),
                json={"body": full_body},
            )
            resp.raise_for_status()

    print("  Done.")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    print(f"\n=== AI PR Review: {REPO}#{PR_NUMBER} ===\n")

    # Step 1: Fetch PR data
    pr_data = fetch_pr_data()
    pr_title = pr_data.get("title", "")
    pr_body = pr_data.get("body") or "(no description)"
    pr_files = fetch_pr_files()
    changed_files = [f["filename"] for f in pr_files]
    pr_diff = fetch_pr_diff()

    file_list_str = "\n".join(f"  - {f}" for f in changed_files)
    print(f"\nPR: {pr_title}")
    print(f"Changed files ({len(changed_files)}):\n{file_list_str}\n")

    # Step 2: RAG
    print("Running RAG pipeline...")
    chunks = load_and_chunk_docs(DOCS_DIR)
    query = (
        f"Code review for changes in: {', '.join(changed_files[:10])}. "
        f"Diff preview: {pr_diff[:500]}"
    )
    relevant_chunks = retrieve_relevant_rules(chunks, query)
    rag_context = build_rag_context(relevant_chunks)

    # Step 3: LLM call
    system_prompt = (
        "You are a senior Android Kotlin engineer performing a code review. "
        "Apply the following project-specific code review rules when analyzing the PR:\n\n"
        f"{rag_context}\n\n"
        "Be concise, actionable, and specific. Reference file names and line numbers when possible."
    )
    user_prompt = (
        f"Please review this pull request.\n\n"
        f"**Title:** {pr_title}\n\n"
        f"**Description:**\n{pr_body}\n\n"
        f"**Changed files:**\n{file_list_str}\n\n"
        f"**Diff:**\n```diff\n{pr_diff}\n```\n\n"
        "Provide a structured review with the following sections:\n"
        "## Potential Bugs\n"
        "## Architectural Issues\n"
        "## Recommendations\n\n"
        "If a section has no findings, write 'No issues found.' under it."
    )

    review_text = call_deepseek(system_prompt, user_prompt)

    # Step 4: Post comment
    print("\nPosting review comment to GitHub...")
    retrieved_sections = ", ".join(f"{c['source']}#{c['heading']}" for c in relevant_chunks)
    rag_footer = f"Generated by DeepSeek · RAG rules applied: {retrieved_sections}"
    post_or_update_comment(review_text, rag_footer)

    print("\n=== Review complete ===\n")


if __name__ == "__main__":
    main()
