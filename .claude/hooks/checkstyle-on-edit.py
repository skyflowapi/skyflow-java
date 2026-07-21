# checkstyle-on-edit.py — PostToolUse hook for the Skyflow Java SDK.
#
# Registered in .claude/settings.json under hooks.PostToolUse with matcher "Edit|Write".
# Fires automatically after every Edit or Write tool call on any file.
#
# What it does:
#   - Ignores non-.java files immediately (no Maven overhead).
#   - For files under src/main/java/, runs checkstyle scoped to that single file
#     via -Dcheckstyle.includes=<relative-path> to keep it fast.
#   - For files outside src/main/java/ (e.g. tests, samples), runs full-module checkstyle.
#   - Prints the last 20 lines of any violations so Claude sees them in-turn
#     without needing a separate /quality run.
#
# Config: checkstyle.xml — generated/ is excluded by pom.xml config so Fern
# auto-generated code is never flagged.
import sys, json, subprocess, os

d = json.load(sys.stdin)
f = d.get('tool_input', {}).get('file_path', d.get('file_path', ''))
if not f or not f.endswith('.java'):
    sys.exit(0)

root = '/home/devb/SDK/skyflow-java'
marker = 'src/main/java/'
if marker in f:
    rel = f.split(marker, 1)[1]
    args = ['mvn', 'checkstyle:check', '-q', '-Dcheckstyle.includes=' + rel]
else:
    args = ['mvn', 'checkstyle:check', '-q']

r = subprocess.run(args, capture_output=True, text=True, cwd=root)
out = (r.stdout + r.stderr).strip()
if out:
    print('\n'.join(out.splitlines()[-20:]))
