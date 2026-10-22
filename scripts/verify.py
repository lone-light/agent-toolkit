#!/usr/bin/env python3
"""Read-only snapshot and optional local installation comparison."""
from pathlib import Path
import argparse, hashlib, json, sys

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--installed', action='store_true', help='Also compare current home installation; never write')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    manifest = json.loads((root / 'snapshot.json').read_text())
    errors = []
    expected = manifest['files']
    actual = {str(p.relative_to(root)) for name in ['skills', 'instructions'] for p in (root / name).rglob('*') if p.is_file()}
    for extra in sorted(actual - set(expected)):
        errors.append('Unrecorded file: ' + extra)
    for name, sha in expected.items():
        rel = Path(name)
        if rel.is_absolute() or '..' in rel.parts or rel.parts[0] not in ['skills', 'instructions']:
            errors.append('Unsafe manifest path: ' + name)
            continue
        path = root / rel
        if path.is_symlink():
            errors.append('Unexpected symlink: ' + name)
        elif not path.is_file() or digest(path) != sha:
            errors.append('Missing or changed repository file: ' + name)
        if args.installed:
            if rel.parts[0] == 'skills':
                if name == 'skills/grilling/LICENSE':
                    continue  # Added only in repository for licence compliance.
                installed = Path.home() / '.agents' / rel
            elif rel.parts[1] == 'codex':
                installed = Path.home() / '.codex/AGENTS.md'
            elif rel.parts[1] == 'opencode':
                installed = Path.home() / '.config/opencode/AGENTS.md'
            else:
                errors.append('Unknown tool: ' + name)
                continue
            if not installed.is_file() or digest(installed) != sha:
                errors.append('Missing or changed installation: ' + str(installed))
    if errors:
        print('FAIL\n' + '\n'.join(errors))
        return 1
    print('PASS: %d snapshot files%s; no files written.' % (len(expected), ' and local installation' if args.installed else ''))
    return 0

if __name__ == '__main__':
    sys.exit(main())
