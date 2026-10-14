#!/usr/bin/env python3
"""Find translations that predate the last change to their English source.

Comparing a translation against the current English string only catches the case
where nobody translated it. It cannot catch a translation that faithfully renders
an English sentence which has since been reworded — that reads as a perfectly good
translation of the wrong text.

So this walks git history instead. For every key it records the commit in which
that key's value last changed, in the English file and in each locale file. A
translation whose value last changed before the English did is rendering a
superseded sentence.

Usage:
    python3 find_drifted_translations.py [--markdown OUT.md]
"""

import argparse
import io
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

BASE_DIR = Path(__file__).parent.parent.parent
RES_DIR = 'base/src/main/res'
ENGLISH = f'{RES_DIR}/values/strings.xml'


def git(*args):
    return subprocess.run(
        ['git'] + list(args), capture_output=True, text=True, cwd=BASE_DIR
    ).stdout


def commits(path):
    """Oldest-first commits touching path, as (sha, unix time)."""
    log = git('log', '--reverse', '--format=%H %ct', '--', path).strip()
    return [line.split() for line in log.splitlines() if line]


def strings_at(sha, path):
    """The key -> value map at a revision, or None when it will not parse."""
    blob = git('show', f'{sha}:{path}')
    if not blob.strip():
        return None
    try:
        root = ET.parse(io.StringIO(blob)).getroot()
    except ET.ParseError:
        return None
    return {
        s.get('name'): ''.join(s.itertext()).strip()
        for s in root.findall('string')
        if s.get('name') and s.get('translatable', 'true') != 'false'
    }


def value_history(path):
    """(key -> unix time its value last changed, key -> current value)."""
    changed_at, previous = {}, {}
    for sha, timestamp in commits(path):
        current = strings_at(sha, path)
        if current is None:
            continue
        for key, value in current.items():
            if previous.get(key) != value:
                changed_at[key] = int(timestamp)
        previous = current
    return changed_at, previous


def english_wordings(key, snapshots):
    """The English value before and after its last change."""
    seen = [(t, snap[key]) for t, snap in snapshots if key in snap]
    if not seen:
        return None, None
    distinct = [seen[0]]
    for timestamp, value in seen[1:]:
        if value != distinct[-1][1]:
            distinct.append((timestamp, value))
    if len(distinct) < 2:
        return None, distinct[-1][1]
    return distinct[-2][1], distinct[-1][1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--markdown', help='also write the report to this file')
    args = parser.parse_args()

    english_changed_at, english_now = value_history(ENGLISH)
    print(f'English: {len(english_now)} strings, {len(commits(ENGLISH))} commits of history')

    snapshots = []
    for sha, timestamp in commits(ENGLISH):
        snap = strings_at(sha, ENGLISH)
        if snap is not None:
            snapshots.append((int(timestamp), snap))

    drifted = {}
    locale_paths = sorted(
        p for p in git('ls-files', f'{RES_DIR}/values-*/strings.xml').split()
        if 'ldrtl' not in p
    )
    for path in locale_paths:
        locale = path.split('/')[-2].replace('values-', '')
        changed_at, current = value_history(path)
        for key, value in (current or {}).items():
            if key not in english_now or key not in english_changed_at or key not in changed_at:
                continue
            # Identical to English is a missing translation, which find_missing covers.
            if value == english_now[key]:
                continue
            if changed_at[key] < english_changed_at[key]:
                drifted.setdefault(key, []).append(locale)

    print(f'Locales: {len(locale_paths)}')
    print(f'\n{len(drifted)} keys have translations older than their English source\n')

    lines = []
    for key, locales in sorted(drifted.items(), key=lambda kv: -len(kv[1])):
        was, now = english_wordings(key, snapshots)
        lines.append(f'### {key}')
        lines.append(f'- affected: {len(locales)}/{len(locale_paths)} locales — {", ".join(locales)}')
        lines.append(f'- English was: {was!r}')
        lines.append(f'- English now: {now!r}')
        lines.append('')
        print(f'{len(locales):3}/{len(locale_paths)}  {key}')
        print(f'         was: {was!r}')
        print(f'         now: {now!r}')

    if args.markdown:
        out = Path(args.markdown)
        header = [
            '# Drifted translations',
            '',
            'Keys whose English text was reworded after the translations landed, so the',
            'translations still describe the previous wording. Regenerate with:',
            '',
            '    python3 tools/translate/find_drifted_translations.py --markdown ' + args.markdown,
            '',
            'A change that is only a typo or capitalisation fix leaves its translations',
            'correct; check the two wordings below before sending a key out for retranslation.',
            '',
        ]
        out.write_text('\n'.join(header + lines), encoding='utf-8')
        print(f'\nWritten to {out}')

    return 0 if not drifted else 0


if __name__ == '__main__':
    sys.exit(main())
