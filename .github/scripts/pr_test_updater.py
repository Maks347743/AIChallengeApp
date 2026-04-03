"""
PR Unit Test Updater
--------------------
1. Fetches changed Kotlin source files in feature/ modules via GitHub API
2. For each changed file, fetches source + existing test content
3. Calls DeepSeek to decide if tests need updating and generates updated test content
4. Writes test files to disk and runs Gradle tests
5. Commits and pushes test changes to the PR branch
6. Posts a PR comment summarising what changed and test results
"""

import os
import re
import sys
import json
import time
import base64
import datetime
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

import httpx

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
REPO = _require_env("REPO")
PR_HEAD_BRANCH = _require_env("PR_HEAD_BRANCH")

_pr_number_raw = _require_env("PR_NUMBER")
try:
    PR_NUMBER = int(_pr_number_raw)
except ValueError:
    print(f"ERROR: PR_NUMBER must be an integer, got: '{_pr_number_raw}'", file=sys.stderr)
    sys.exit(1)

GITHUB_API = "https://api.github.com"
DEEPSEEK_API = os.getenv("DEEPSEEK_API", "https://api.deepseek.com/v1/chat/completions")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
MAX_FILE_CHARS = 20_000

AI_COMMENT_MARKER = "<!-- ai-test-updater -->"
OLD_REVIEWS_MARKER = "<!-- old-test-reviews -->"
VERSION_RE = re.compile(r"<!-- version:(\d+) -->")
TIMESTAMP_RE = re.compile(r"<!-- timestamp:([^>]+) -->")

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


def fetch_file_content(path: str) -> str | None:
    """Fetch a file's content from the repo HEAD. Returns None if not found."""
    with httpx.Client(timeout=30) as client:
        resp = client.get(
            f"{GITHUB_API}/repos/{REPO}/contents/{path}",
            headers=github_headers(),
        )
        if resp.status_code == 404:
            return None
        resp.raise_for_status()
        data = resp.json()
        content = base64.b64decode(data["content"]).decode("utf-8")
        return content


# ---------------------------------------------------------------------------
# Step 2: Identify feature/ source files that need test analysis
# ---------------------------------------------------------------------------

def get_test_path(source_path: str) -> str:
    """Map a source file path to its expected test file path."""
    return source_path.replace("/src/main/", "/src/test/").replace(".kt", "Test.kt")


def get_gradle_module(file_path: str) -> str:
    """Derive Gradle module task prefix from a file path inside feature/."""
    # feature/chat/src/... → :feature:chat
    parts = file_path.split("/src/")[0].split("/")
    return ":" + ":".join(parts)


def identify_source_files(pr_files: list[dict]) -> list[dict]:
    """Return only feature/ Kotlin source files (not test files, not removed)."""
    result = []
    for f in pr_files:
        filename = f["filename"]
        if (
            filename.endswith(".kt")
            and filename.startswith("feature/")
            and "/src/main/" in filename
            and f.get("status") != "removed"
        ):
            result.append(f)
    return result


# ---------------------------------------------------------------------------
# Step 3: Call DeepSeek for test generation
# ---------------------------------------------------------------------------

KOTEST_EXAMPLE = """package com.example.aichallengeapp.feature.chat.domain.usecase

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class ExampleUseCaseTest : FunSpec({

    val useCase = ExampleUseCase()

    test("returns empty list when input is empty") {
        useCase(emptyList()).shouldBeEmpty()
    }

    test("returns correct result for valid input") {
        val result = useCase(listOf("a", "b"))
        result.size shouldBe 2
    }
})"""


def call_deepseek(system_prompt: str, user_prompt: str, max_tokens: int = 3000) -> str:
    print("  Calling DeepSeek API...")
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
        "max_tokens": max_tokens,
        "temperature": 0.1,
    }

    def do_request():
        with httpx.Client(timeout=120) as client:
            resp = client.post(DEEPSEEK_API, headers=headers, json=payload)
            resp.raise_for_status()
            return resp.json()

    data = retry_request(do_request)
    return data["choices"][0]["message"]["content"]


def analyze_and_generate_tests(
    source_path: str,
    source_content: str,
    test_path: str,
    existing_test_content: str | None,
    diff_patch: str,
) -> dict | None:
    """
    Returns dict with keys: needs_update, reason, test_file_path, test_file_content.
    Returns None on parse failure.
    """
    if len(source_content) > MAX_FILE_CHARS:
        source_content = source_content[:MAX_FILE_CHARS] + "\n\n[... truncated ...]"

    existing_test_section = (
        f"EXISTING TEST FILE CONTENT:\n```kotlin\n{existing_test_content}\n```"
        if existing_test_content
        else "EXISTING TEST FILE: does not exist yet"
    )

    system_prompt = (
        "You are a senior Android Kotlin engineer. Your task is to write or update unit tests "
        "for a changed Kotlin source file. Follow these rules strictly:\n"
        "- Use io.kotest.core.spec.style.FunSpec\n"
        "- Use io.kotest.matchers.* for assertions\n"
        "- Test observable behavior, not implementation details\n"
        "- Cover: success paths, edge cases, error/null scenarios\n"
        "- Do NOT use mocks; prefer fakes or direct instantiation\n"
        "- Each test must be independent — no shared mutable state between tests\n"
        "- Keep the existing package declaration and imports if updating a file\n\n"
        f"Example of expected test style:\n```kotlin\n{KOTEST_EXAMPLE}\n```\n\n"
        "Respond ONLY with valid JSON (no markdown fences) in this exact format:\n"
        "{\n"
        '  "needs_update": true,\n'
        '  "reason": "brief explanation of what changed and why tests need updating",\n'
        '  "test_file_path": "relative/path/FooTest.kt",\n'
        '  "test_file_content": "full Kotlin file content as a string"\n'
        "}\n"
        "OR if no test update is needed:\n"
        "{\n"
        '  "needs_update": false,\n'
        '  "reason": "brief explanation of why no test update is needed"\n'
        "}"
    )

    user_prompt = (
        f"SOURCE FILE: {source_path}\n\n"
        f"SOURCE CONTENT:\n```kotlin\n{source_content}\n```\n\n"
        f"{existing_test_section}\n\n"
        f"DIFF FOR THIS FILE:\n```diff\n{diff_patch}\n```\n\n"
        f"Expected test file path: {test_path}\n\n"
        "Analyze the diff and source file. Decide if tests need to be created or updated. "
        "Return JSON only."
    )

    raw = call_deepseek(system_prompt, user_prompt)

    # Strip markdown code fences if model wraps them anyway
    raw = raw.strip()
    if raw.startswith("```"):
        raw = re.sub(r"^```[a-z]*\n?", "", raw)
        raw = re.sub(r"\n?```$", "", raw)
    raw = raw.strip()

    try:
        return json.loads(raw)
    except json.JSONDecodeError as e:
        print(f"  WARNING: Failed to parse DeepSeek JSON response: {e}", file=sys.stderr)
        print(f"  Raw response (first 500 chars): {raw[:500]}", file=sys.stderr)
        return None


# ---------------------------------------------------------------------------
# Step 4: Write test files
# ---------------------------------------------------------------------------

def write_test_file(path: str, content: str) -> None:
    full_path = Path(path)
    full_path.parent.mkdir(parents=True, exist_ok=True)
    full_path.write_text(content, encoding="utf-8")
    print(f"  Written: {path}")


# ---------------------------------------------------------------------------
# Step 5: Run Gradle tests
# ---------------------------------------------------------------------------

def run_gradle_tests(modules: list[str]) -> tuple[bool, str]:
    """
    Run unit tests for the given Gradle modules.
    Returns (success, output_text).
    """
    # Strip Windows CRLF line endings — gradlew committed from Windows has \r\n
    gradlew = Path("gradlew")
    gradlew.write_bytes(gradlew.read_bytes().replace(b"\r\n", b"\n").replace(b"\r", b"\n"))
    subprocess.run(["chmod", "+x", "gradlew"], check=True)

    tasks = [f"{m}:test" for m in modules]
    # Run explicitly via bash to avoid /bin/sh (dash) which rejects some Gradle wrapper syntax
    cmd = ["bash", "gradlew"] + tasks + ["--continue", "--no-daemon"]
    print(f"  Running: {' '.join(cmd)}")

    result = subprocess.run(
        cmd,
        capture_output=True,
        text=True,
    )
    combined = result.stdout + "\n" + result.stderr
    return result.returncode == 0, combined


def parse_test_results(modules: list[str]) -> dict:
    """
    Parse JUnit XML results from all affected feature modules.
    Searches recursively under build/test-results/ for any TEST-*.xml files.
    Returns {total, passed, failed, skipped, failures: [{classname, testname, message}]}.
    """
    totals = {"total": 0, "passed": 0, "failed": 0, "skipped": 0, "failures": []}

    for module in modules:
        module_dir = module.lstrip(":").replace(":", "/")
        results_root = Path(f"{module_dir}/build/test-results")
        if not results_root.exists():
            print(f"  WARNING: {results_root} does not exist — no test results found for {module}")
            continue
        xml_files = list(results_root.rglob("TEST-*.xml"))
        print(f"  Found {len(xml_files)} XML result file(s) in {results_root}")
        for xml_file in xml_files:
            try:
                tree = ET.parse(xml_file)
                root = tree.getroot()
                totals["total"] += int(root.attrib.get("tests", 0))
                totals["failed"] += int(root.attrib.get("failures", 0)) + int(root.attrib.get("errors", 0))
                totals["skipped"] += int(root.attrib.get("skipped", 0))
                for tc in root.findall("testcase"):
                    failure = tc.find("failure") or tc.find("error")
                    if failure is not None:
                        totals["failures"].append({
                            "classname": tc.attrib.get("classname", ""),
                            "testname": tc.attrib.get("name", ""),
                            "message": (failure.attrib.get("message") or failure.text or "")[:500],
                        })
            except ET.ParseError:
                pass

    totals["passed"] = totals["total"] - totals["failed"] - totals["skipped"]
    return totals


# ---------------------------------------------------------------------------
# Step 6: Git commit and push
# ---------------------------------------------------------------------------

def git_commit_and_push(test_paths: list[str], pr_number: int) -> bool:
    """Stage test files, commit, and push. Returns True if commit was made."""
    subprocess.run(["git", "config", "user.name", "AI Test Bot"], check=True)
    subprocess.run(["git", "config", "user.email", "actions@github.com"], check=True)

    for path in test_paths:
        subprocess.run(["git", "add", path], check=True)

    # Check if there's anything staged
    status = subprocess.run(["git", "diff", "--cached", "--name-only"], capture_output=True, text=True)
    staged = status.stdout.strip()
    if not staged:
        print("  No changes to commit.")
        return False

    subprocess.run(
        ["git", "commit", "-m", f"ci: auto-update unit tests for PR #{pr_number}"],
        check=True,
    )
    subprocess.run(["git", "push", "origin", f"HEAD:{PR_HEAD_BRANCH}"], check=True)
    print("  Committed and pushed test changes.")
    return True


# ---------------------------------------------------------------------------
# Step 7: Post PR comment
# ---------------------------------------------------------------------------

def find_existing_comment() -> dict | None:
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


def _extract_current_section(body: str) -> str:
    parts = body.split(AI_COMMENT_MARKER, 1)
    content = parts[1] if len(parts) > 1 else body
    content = VERSION_RE.sub("", content)
    content = TIMESTAMP_RE.sub("", content)
    if OLD_REVIEWS_MARKER in content:
        content = content[:content.index(OLD_REVIEWS_MARKER)]
    return content.strip()


def _extract_old_sections(body: str) -> str:
    if OLD_REVIEWS_MARKER not in body:
        return ""
    return body[body.index(OLD_REVIEWS_MARKER) + len(OLD_REVIEWS_MARKER):].strip()


def build_comment_body(
    analyzed_count: int,
    updates: list[dict],  # [{path, reason, is_new}]
    test_results: dict | None,
    committed: bool,
    gradle_output: str = "",
) -> str:
    lines = []

    if not updates:
        lines.append("## AI Unit Test Update")
        lines.append("")
        lines.append(f"**Files analyzed:** {analyzed_count} source file(s)")
        lines.append("")
        lines.append("No test updates required for this PR.")
        return "\n".join(lines)

    lines.append("## AI Unit Test Update")
    lines.append("")
    lines.append(f"**Files analyzed:** {analyzed_count} source file(s)  ")
    lines.append(f"**Tests updated:** {len(updates)} file(s)")
    lines.append("")
    lines.append("### Changes")
    for u in updates:
        action = "created" if u["is_new"] else "updated"
        lines.append(f"- `{u['path']}` — {action} _(reason: {u['reason']})_")

    if committed:
        lines.append("")
        lines.append(
            "_Test files were committed to this branch. "
            "Please review the generated tests before merging._"
        )

    if test_results is not None:
        lines.append("")
        lines.append("### Test Results")
        total = test_results["total"]
        passed = test_results["passed"]
        failed = test_results["failed"]
        skipped = test_results["skipped"]

        if total == 0:
            lines.append("⚠️ No test results found — Gradle may have failed to compile or run tests.")
            if gradle_output.strip():
                trimmed = gradle_output.strip()[-3000:]
                lines.append("")
                lines.append("<details><summary>Gradle output</summary>")
                lines.append("")
                lines.append(f"```\n{trimmed}\n```")
                lines.append("</details>")
        else:
            status_icon = "✅" if failed == 0 else "❌"
            lines.append(f"{status_icon} **{passed} passed**, {failed} failed, {skipped} skipped (total: {total})")

            if test_results["failures"]:
                lines.append("")
                lines.append("<details><summary>Failures</summary>")
                lines.append("")
                for f in test_results["failures"]:
                    lines.append(f"**{f['classname']}#{f['testname']}**")
                    lines.append(f"```\n{f['message']}\n```")
                lines.append("</details>")

    return "\n".join(lines)


def post_or_update_comment(body_content: str) -> None:
    now = datetime.datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
    existing = find_existing_comment()

    if existing:
        old_version = _extract_version(existing["body"])
        old_timestamp = _extract_timestamp(existing["body"])
        new_version = old_version + 1
        old_section = _extract_current_section(existing["body"])
        accumulated_old = _extract_old_sections(existing["body"])

        old_block = (
            f"<details>\n"
            f"<summary>~~v{old_version} · {old_timestamp}~~</summary>\n\n"
            f"<del>\n\n{old_section}\n\n</del>\n\n"
            f"</details>"
        )
        all_old = f"{old_block}\n\n{accumulated_old}".strip()

        full_body = (
            f"{AI_COMMENT_MARKER}\n"
            f"<!-- version:{new_version} -->\n"
            f"<!-- timestamp:{now} -->\n\n"
            f"{body_content}\n\n"
            f"_v{new_version} · {now}_\n\n"
            f"{OLD_REVIEWS_MARKER}\n\n"
            f"{all_old}"
        )
        print(f"  Updating existing comment #{existing['id']} → v{new_version}...")
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
            f"{body_content}\n\n"
            f"_v1 · {now}_"
        )
        print("  Posting new comment (v1)...")
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
    print(f"\n=== AI Unit Test Updater: {REPO}#{PR_NUMBER} ===\n")

    # Step 1: Identify feature/ source files changed in the PR
    pr_files = fetch_pr_files()
    source_files = identify_source_files(pr_files)

    if not source_files:
        print("No feature/ Kotlin source files changed. Nothing to do.")
        post_or_update_comment(
            "## AI Unit Test Update\n\nNo Kotlin source files in `feature/` modules were changed in this PR."
        )
        return

    print(f"\nFeature source files to analyze ({len(source_files)}):")
    for f in source_files:
        print(f"  - {f['filename']}")
    print()

    # Step 2–4: Fetch content, call DeepSeek, write tests
    updates = []       # [{path, reason, is_new}]
    written_paths = []
    affected_modules = set()

    for pr_file in source_files:
        source_path = pr_file["filename"]
        test_path = get_test_path(source_path)
        diff_patch = pr_file.get("patch", "")

        print(f"Analyzing: {source_path}")

        source_content = fetch_file_content(source_path)
        if source_content is None:
            print(f"  WARNING: Could not fetch source file content. Skipping.")
            continue

        existing_test = fetch_file_content(test_path)
        is_new = existing_test is None
        print(f"  Test file: {'not found (will create)' if is_new else 'found (will update if needed)'}")

        result = analyze_and_generate_tests(
            source_path, source_content, test_path, existing_test, diff_patch
        )

        if result is None:
            print(f"  Skipping due to parse error.")
            continue

        if not result.get("needs_update", False):
            print(f"  No update needed: {result.get('reason', '')}")
            continue

        generated_path = result.get("test_file_path", test_path)
        generated_content = result.get("test_file_content", "")
        reason = result.get("reason", "")

        if not generated_content.strip():
            print(f"  WARNING: DeepSeek returned empty test content. Skipping.")
            continue

        write_test_file(generated_path, generated_content)
        written_paths.append(generated_path)
        updates.append({"path": generated_path, "reason": reason, "is_new": is_new})
        affected_modules.add(get_gradle_module(generated_path))

    # Step 5: Run tests (only if we wrote any test files)
    test_results = None
    committed = False
    gradle_output = ""

    if written_paths:
        print(f"\nRunning tests for modules: {', '.join(affected_modules)}")
        modules_list = list(affected_modules)
        success, gradle_output = run_gradle_tests(modules_list)
        test_results = parse_test_results(modules_list)
        print(
            f"  Results: {test_results['total']} total, "
            f"{test_results['passed']} passed, "
            f"{test_results['failed']} failed, "
            f"{test_results['skipped']} skipped"
        )
        if not success:
            print("  WARNING: Gradle reported failures.")

        # Step 6: Commit and push
        print("\nCommitting test changes...")
        committed = git_commit_and_push(written_paths, PR_NUMBER)

    # Step 7: Post comment
    print("\nPosting PR comment...")
    body = build_comment_body(
        analyzed_count=len(source_files),
        updates=updates,
        test_results=test_results,
        committed=committed,
        gradle_output=gradle_output,
    )
    post_or_update_comment(body)

    print("\n=== Test update complete ===\n")


if __name__ == "__main__":
    main()
