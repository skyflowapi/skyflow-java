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
