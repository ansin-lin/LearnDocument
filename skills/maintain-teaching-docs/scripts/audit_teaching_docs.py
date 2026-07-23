#!/usr/bin/env python3
"""Audit LearnDocument Markdown structure and selected teaching policies."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path


@dataclass(frozen=True)
class Finding:
    severity: str
    category: str
    file: str
    line: int
    message: str


FENCE_RE = re.compile(r"^\s*(```|~~~)")
HEADING_RE = re.compile(r"^(#{1,6})\s+(.+?)\s*$")
LINK_RE = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
NAV_MD_RE = re.compile(r":[ \t]*([^:#\r\n]+?\.md)[ \t]*$", re.MULTILINE)
REQUEST_RE = re.compile(r"\brequests\.(get|post|put|patch|delete|request)\s*\((.*)")
AUTHOR_GUIDE_PATH_RE = re.compile(r"(?:^|/)[^/]*(?:teacher|instructor|teaching_usage_guide|教师指南|讲师指南)[^/]*\.md$", re.IGNORECASE)
META_WRITING_PATTERNS = (
    re.compile(r"(?:写作|编写|教学|课程)(?:说明|安排|设计)"),
    re.compile(r"(?:这样|如此)安排"),
    re.compile(r"(?:为什么|为何).{0,10}(?:先讲|后讲|课程安排|章节安排)"),
    re.compile(r"为了.{0,16}(?:控制|降低|减轻).{0,8}(?:难度|学习负担|认知负担)"),
    re.compile(r"(?:本章|本节|章节).{0,4}(?:定位|学习顺序)"),
    re.compile(r"学习顺序(?:建议|说明)"),
    re.compile(r"(?:课程编写者|教师决定|授课时|本教案)"),
    re.compile(r"(?:后续章节|后面的章节).{0,8}(?:再讲|再介绍|展开)"),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--scope", type=Path, default=Path("docs"))
    parser.add_argument("--format", choices=("text", "json"), default="text")
    return parser.parse_args()


def rel(path: Path, root: Path) -> str:
    return path.resolve().relative_to(root.resolve()).as_posix()


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def markdown_files(root: Path, scope: Path) -> list[Path]:
    target = scope if scope.is_absolute() else root / scope
    if target.is_file():
        return [target] if target.suffix.lower() == ".md" else []
    return sorted(target.rglob("*.md")) if target.exists() else []


def audit_markdown(path: Path, root: Path) -> list[Finding]:
    findings: list[Finding] = []
    text = path.read_text(encoding="utf-8-sig")
    lines = text.splitlines()
    file_name = rel(path, root)

    fence_markers: list[tuple[int, str]] = []
    prose_lines: list[tuple[int, str]] = []
    active_fence = ""
    for number, line in enumerate(lines, 1):
        fence_match = FENCE_RE.match(line)
        if fence_match:
            marker = fence_match.group(1)
            fence_markers.append((number, marker))
            if not active_fence:
                active_fence = marker
            elif marker == active_fence:
                active_fence = ""
            continue
        if not active_fence:
            prose_lines.append((number, line))
    if active_fence:
        findings.append(Finding("P0", "structure", file_name, fence_markers[-1][0], "Markdown code fence is not closed."))

    h1_lines = [number for number, line in prose_lines if re.match(r"^#\s+", line)]
    if not h1_lines:
        findings.append(Finding("P1", "structure", file_name, 1, "Document has no H1 title."))
    elif len(h1_lines) > 1:
        findings.append(Finding("P1", "structure", file_name, h1_lines[1], "Document has multiple H1 titles."))

    previous_level = 0
    previous_title = ""
    for number, line in prose_lines:
        match = HEADING_RE.match(line)
        if not match:
            continue
        level = len(match.group(1))
        title = re.sub(r"\s+", " ", match.group(2).strip())
        if previous_level and level > previous_level + 1:
            findings.append(Finding("P2", "structure", file_name, number, f"Heading jumps from H{previous_level} to H{level}."))
        if title == previous_title:
            findings.append(Finding("P2", "duplication", file_name, number, f"Adjacent duplicate heading: {title}"))
        previous_level, previous_title = level, title

    if not AUTHOR_GUIDE_PATH_RE.search(file_name):
        for number, line in prose_lines:
            stripped = line.strip()
            if any(pattern.search(stripped) for pattern in META_WRITING_PATTERNS):
                findings.append(
                    Finding(
                        "P2",
                        "audience",
                        file_name,
                        number,
                        "Possible author-facing course-design prose; keep only learner-relevant technical or scope information.",
                    )
                )

    for match in LINK_RE.finditer(text):
        target = match.group(1).strip().split()[0].strip("<>")
        if not target or target.startswith(("#", "http://", "https://", "mailto:")):
            continue
        target_path = target.split("#", 1)[0]
        if not target_path:
            continue
        resolved = (path.parent / target_path).resolve()
        if not resolved.exists():
            findings.append(Finding("P0", "link", file_name, line_number(text, match.start()), f"Broken relative link: {target}"))

    if "/python/" in f"/{file_name.lower()}":
        for number, line in prose_lines:
            stripped = line.strip()
            request_match = REQUEST_RE.search(stripped)
            if request_match and "timeout=" not in request_match.group(2):
                findings.append(Finding("P1", "example", file_name, number, "requests call may be missing timeout= in the same call."))
            positive_writelines = re.search(r"(自动|会).{0,6}(换行|添加换行)", stripped)
            negative_writelines = re.search(r"(不要|不应|不能|不会|并不|误以为|错误)", stripped)
            if "writelines" in stripped and positive_writelines and not negative_writelines:
                findings.append(Finding("P0", "technical", file_name, number, "Text may incorrectly claim that writelines() adds newlines."))
            positive_encoding = re.search(r"(修复|解决).{0,6}乱码", stripped)
            negative_encoding = re.search(r"(不要|不应|不能|不会|不可以|无法|误以为|错误)", stripped)
            if "ensure_ascii=False" in stripped and positive_encoding and not negative_encoding:
                findings.append(Finding("P0", "technical", file_name, number, "ensure_ascii=False may be described as fixing corrupted text."))

    return findings


def is_in_scope(path: Path, scope: Path) -> bool:
    try:
        path.resolve().relative_to(scope.resolve())
        return True
    except ValueError:
        return False


def audit_navigation(root: Path, scope: Path) -> list[Finding]:
    config = root / "mkdocs.yml"
    if not config.exists():
        return []
    text = config.read_text(encoding="utf-8-sig")
    findings: list[Finding] = []
    scope_path = scope if scope.is_absolute() else root / scope
    for match in NAV_MD_RE.finditer(text):
        target = match.group(1).strip().strip("'\"")
        resolved = root / "docs" / target
        if not is_in_scope(resolved, scope_path):
            continue
        if not resolved.exists():
            findings.append(Finding("P0", "navigation", "mkdocs.yml", line_number(text, match.start()), f"Navigation target does not exist: docs/{target}"))
    return findings


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8")

    args = parse_args()
    root = args.root.resolve()
    files = markdown_files(root, args.scope)
    if not files:
        print(f"No Markdown files found in scope: {args.scope}", file=sys.stderr)
        return 2

    findings: list[Finding] = []
    for path in files:
        findings.extend(audit_markdown(path, root))
    findings.extend(audit_navigation(root, args.scope))
    order = {"P0": 0, "P1": 1, "P2": 2}
    findings.sort(key=lambda item: (order[item.severity], item.file.lower(), item.line, item.message))

    if args.format == "json":
        print(json.dumps({"files_scanned": len(files), "findings": [asdict(item) for item in findings]}, ensure_ascii=False, indent=2))
    else:
        counts = {severity: sum(item.severity == severity for item in findings) for severity in order}
        print(f"Scanned {len(files)} Markdown files. P0={counts['P0']} P1={counts['P1']} P2={counts['P2']}")
        for item in findings:
            print(f"{item.severity} [{item.category}] {item.file}:{item.line} - {item.message}")

    return 1 if any(item.severity == "P0" for item in findings) else 0


if __name__ == "__main__":
    raise SystemExit(main())
