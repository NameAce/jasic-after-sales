# -*- coding: utf-8 -*-
"""One-off: replace legacy \$surface-* SCSS tokens in contractor with user-end names (regex, longest-first)."""
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1] / "src"

SUBS_ORDERED = [
    (r"\$surface-voice-border\b", "$voice-panel-track"),
    (r"\$surface-voice\b", "$bg-muted-panel"),
    (r"\$surface-slate-300\b", "$icon-slate-light"),
    (r"\$surface-slate-200\b", "$border-slate"),
    (r"\$surface-slate-100\b", "$bg-hover"),
    (r"\$surface-slate-50\b", "$bg-light"),
    (r"\$surface-app\b", "$bg-page"),
    (r"\$surface-page\b", "$bg-page"),
]


def fix_surface_white(text: str) -> str:
    """须先处理 `background-color:`，避免 `background-color` 中的 `color:` 子串被误替换。"""
    text = re.sub(r"\$surface-white\b", "$__TMP_WHITE__", text)
    text = text.replace("background-color: $__TMP_WHITE__", "background-color: $bg-card")
    text = text.replace("background: $__TMP_WHITE__", "background: $bg-card")
    text = text.replace("solid $__TMP_WHITE__", "solid $bg-card")
    text = text.replace("transparent $__TMP_WHITE__", "transparent $bg-card")
    text = text.replace("$__TMP_WHITE__ 65%", "$bg-card 65%")
    text = text.replace("color: $__TMP_WHITE__", "color: $text-bg")
    text = text.replace(".text { color: $__TMP_WHITE__", ".text { color: $text-bg")
    text = text.replace("$__TMP_WHITE__", "$bg-card")
    return text


def main() -> None:
    for path in sorted(ROOT.rglob("*")):
        if path.suffix not in (".vue", ".scss"):
            continue
        raw = path.read_text(encoding="utf-8")
        out = raw
        for pat, rep in SUBS_ORDERED:
            out = re.sub(pat, rep, out)
        out = fix_surface_white(out)
        if out != raw:
            path.write_text(out, encoding="utf-8")
            print("updated", path.relative_to(ROOT.parent.parent))


if __name__ == "__main__":
    main()
