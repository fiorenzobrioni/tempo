"""The index of PLANNING's archive (2 Nov 2026).

The plan lives in `PLANNING.md` (the architecture and the rules the code keeps, the phases still
open, what is open, the log of the current period) and the closed periods in
`docs/planning/NN-<period>.md`, frozen. This script writes `docs/planning/README.md`: every `##`
and `###` heading of every archive file with its line, and every entry of a log written as a list
(`- **7 Oct 2026, Phase 0.** ...`, the form §15's log had until archive 01), so a reference by name
or by date ("PLANNING.md §11 Phase 3", "§15, 9 Oct 2026") resolves with a search of the index and
a read from the right line.

The live log is not in it: it is short and searched by itself, and so the index changes only when
archiving (the log of `PLANNING.md` moves to a new archive file). Then run it again:

    python3 tools/planning_index.py           # rewrites the index
    python3 tools/planning_index.py --check   # exits with 1 if the index is behind
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ARCHIVE = ROOT / "docs" / "planning"
INDEX = ARCHIVE / "README.md"

HEADER = """# PLANNING's archive

The closed periods of Tempo's plan, one per file, **frozen**: they are not edited. The living
plan (the architecture, the phases still open, what is open, the log of the current period) is
[`PLANNING.md`](../../PLANNING.md), which also says how to archive. A decision that changes one of
these is written there, naming the one it changes.

This index is written by `tools/planning_index.py`: it is not edited by hand. Every section
(`##`) and subsection (`###`) heading of the archive with its line, and every dated entry of a log
kept as a list, to look up a reference by name or by date and read from the right place. The log
of the current period is not here: it is in `PLANNING.md`, short, and searched there.
"""

HEADING = re.compile(r"^(#{2,3}) (.+?)\s*$")
# A log entry kept as a list: its date and where it belongs in bold, then its title in bold or
# its first sentence.
ENTRY = re.compile(r"^- \*\*(\d{1,2} [A-Z][a-z]{2} \d{4}[^*]*)\*\*\s*(.*)$")
# A bold phrase is the entry's title unless the sentence goes on after it ("**Free time** is ...").
TITLE = re.compile(r"^\*\*(.+?)\*\*(?![ ]+[a-z])")
ENTRY_TITLE_MAX = 100


def files() -> list[pathlib.Path]:
    return sorted(ARCHIVE.glob("[0-9][0-9]-*.md"))


def title(path: pathlib.Path, lines: list[str]) -> str:
    for line in lines:
        if line.startswith("# "):
            return line[2:].strip()
    return path.name


def first_sentence(text: str) -> str:
    """Up to the first full stop, colon or semicolon outside brackets, code and quotes."""
    depth, code, quote = 0, False, False
    for i, char in enumerate(text):
        if char == "`":
            code = not code
        elif code:
            continue
        elif char == '"':
            quote = not quote
        elif char in "(«":
            depth += 1
        elif char in ")»":
            depth = max(0, depth - 1)
        elif char in ".:;" and depth == 0 and not quote and text[i + 1 : i + 2] in ("", " "):
            return text[:i]
    return text


def entry_title(rest: str) -> str:
    bold = TITLE.match(rest)
    text = bold.group(1) if bold else first_sentence(rest.replace("**", ""))
    text = text.strip().rstrip(".:;")
    if len(text) <= ENTRY_TITLE_MAX:
        return text
    return text[:ENTRY_TITLE_MAX].rsplit(" ", 1)[0].rstrip(",;:") + "…"


def render() -> str:
    out = [HEADER]
    for path in files():
        lines = path.read_text(encoding="utf-8").splitlines()
        out.append(f"\n## [{path.name}]({path.name}): {title(path, lines)}\n")
        fence = False
        for number, line in enumerate(lines, start=1):
            if line.startswith("```"):
                fence = not fence
            if fence:
                continue
            match = HEADING.match(line)
            if match:
                indent = "" if len(match.group(1)) == 2 else "  "
                out.append(f"{indent}- L{number}: {match.group(2)}")
                continue
            entry = ENTRY.match(line)
            if entry:
                out.append(f"    - L{number}: {entry.group(1).strip()} {entry_title(entry.group(2))}")
    return "\n".join(out) + "\n"


def main() -> int:
    text = render()
    if "--check" in sys.argv:
        current = INDEX.read_text(encoding="utf-8") if INDEX.exists() else ""
        if current != text:
            print("docs/planning/README.md is behind: python3 tools/planning_index.py")
            return 1
        return 0
    INDEX.write_text(text, encoding="utf-8", newline="\n")
    count = sum(1 for line in text.splitlines() if re.match(r"^\s*- L\d+:", line))
    print(f"{INDEX.relative_to(ROOT).as_posix()}: {count} entries")
    return 0


if __name__ == "__main__":
    sys.exit(main())
