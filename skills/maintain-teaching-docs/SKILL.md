---
name: maintain-teaching-docs
description: Maintain or review LearnDocument teaching content across all supported technology stacks. Use for adding, editing, reorganizing, auditing, or aligning lessons, examples, projects, course routes, navigation, and cross-course consistency; dispatch the applicable common and technology-specific rules before acting.
---

# Maintain Teaching Docs

Apply repository rules without duplicating their policy here. Use this skill to select rules and execute a consistent inspect-change-verify workflow.

## Load context

1. Read the nearest `AGENTS.md`.
2. Read [rule-routing.md](references/rule-routing.md) completely, classify the task and technologies, then load every matching common and technology-specific rule from `.codex/rules/`.
3. Read [project-context.md](references/project-context.md) only for navigation, restructuring, cross-course ownership, or migration work.
4. Combine rules for cross-stack content; do not let a path-based match hide technologies present in code, configuration or prose. Report missing required files.

## Workflow

### Inspect

- Check the working tree and preserve unrelated changes.
- Inspect the target, adjacent lessons, course entry, direct links, and relevant `mkdocs.yml` entries.
- Compare the declared course route with filenames, navigation and actual content.
- When a project introduces a framework, library, component library, plugin, or key dependency, locate its prerequisite explanation and minimal examples.
- For a course or project route, identify the final learner outcome, acceptance criteria, stage outcomes, prerequisites, chapter completeness and final project before changing structure.
- Trace each core interface from its first explanation to a later exercise or project use; identify unused API lists and project-only unexplained interfaces.
- For moves, splits, merges or cross-course changes, search all references and define the old-to-new mapping first.

### Plan structural work

- Record what to preserve, split, merge, move, add or retire before editing.
- Map stage outcomes to prerequisites and verification; keep one stable mainline for zero-beginner routes.
- Define the continuous project state at each checkpoint: names, directories, data, interfaces, configuration and observable result.
- Separate mainline requirements from extensions, appendices and review material.

### Change

- Define the learner, prerequisite, outcome, difficulty, allowed files, and verification as internal editing constraints; do not copy this planning into lesson prose by default.
- Preserve correct content and patch locally unless the structure itself is broken.
- Put each concept in its canonical course; link instead of duplicating full explanations.
- Keep navigation, filenames, headings, numbering, links, code, explanation, and output synchronized.
- Keep a continuous project runnable between stages; state whether each example creates, appends, replaces or removes content.
- Write the result as a learner-facing technical tutorial, not a lesson plan, course-design explanation, editing log, or author commentary.
- Explain why a technology or practice matters; do not explain why the course teaches it earlier, later, or in a particular chapter.
- For frameworks and libraries, establish core concepts and interfaces, then focused minimal examples, then project integration. Keep one-off auxiliary dependencies inside the relevant project lesson when a separate chapter adds no learning value.
- Complete the mainline and final acceptance loop before expanding optional breadth. Do not leave project or delivery chapters as heading-only placeholders.

### Verify

Run the audit on the affected scope:

```powershell
python skills/maintain-teaching-docs/scripts/audit_teaching_docs.py --root . --scope <path>
```

Omit `--scope` for navigation, shared files, structural changes, or repository-wide review. Inspect every P0 and every `audience` finding manually.

Also verify:

- technical facts, examples, difficulty and relevant security guidance;
- route, navigation, chapter order and prerequisite agreement;
- runnable continuity between project stages and a complete final acceptance path;
- introduction or links for every core interface used by exercises and projects;
- exercises that include implementation, modification, verification or troubleshooting where the course is operational;
- absence of heading-only final projects, unexplained project dependencies and unused API catalogues;
- learner value for every added paragraph, removing author-facing planning prose from learner lessons.

Verify changing versions, products, commands, cloud behavior and security advice with primary official sources when practical.

### Report

For edits, report changed files, main changes, verification, and remaining risks. For reviews, list P0/P1/P2 findings with locations and fixes before the summary. Never present automated checks as proof of semantic correctness.
