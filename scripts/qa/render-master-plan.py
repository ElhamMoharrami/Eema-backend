#!/usr/bin/python3
"""Render the Markdown master plan using the existing system MarkdownIt/Chrome tools."""
from pathlib import Path
from markdown_it import MarkdownIt
import subprocess
import tempfile
import re

root = Path(__file__).resolve().parents[2]
source = root / 'docs/EEMA_COMPLETE_TEST_MASTER_PLAN.md'
output = source.with_suffix('.pdf')
# Master-plan attack examples are text, never executable HTML. Restore only safe anchors.
body = MarkdownIt('commonmark', {'html': False}).enable('table').render(source.read_text())
body = re.sub(r'<p>&lt;a id=&quot;([a-z0-9_-]+)&quot;&gt;&lt;/a&gt;</p>', r'<a id="\1"></a>', body)
css = '''@page { size: A4 landscape; margin: 12mm; }
body { font: 9pt/1.4 Arial,sans-serif; color: #18212b; }
h1,h2,h3,h4 { color: #173b4a; break-after: avoid; }
h1 { font-size: 22pt; } h2 { font-size: 17pt; } h3 { font-size: 14pt; } h4 { font-size: 11pt; }
table { border-collapse: collapse; width:100%; font-size: 8pt; table-layout: fixed; }
th,td { border: 1px solid #bbc5ca; padding: 4px; vertical-align: top; overflow-wrap: anywhere; }
th { background: #eaf0f2; } tr { break-inside: avoid; } thead { display: table-header-group; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #f2f4f5; padding: 8px; }
code { font-size: 8pt; overflow-wrap: anywhere; } a { color: #175772; text-decoration: none; }
'''
with tempfile.TemporaryDirectory(prefix='eema-master-plan-') as directory:
    html = Path(directory) / 'master-plan.html'
    html.write_text('<!doctype html><meta charset="utf-8"><title>Eema Complete Test Master Plan</title><style>'+css+'</style>'+body)
    subprocess.run(['google-chrome', '--headless', '--no-sandbox', '--disable-gpu',
                    '--disable-dev-shm-usage', '--disable-background-networking',
                    '--user-data-dir='+str(Path(directory)/'chrome'),
                    '--no-pdf-header-footer', '--print-to-pdf='+str(output), html.as_uri()], check=True, timeout=180)
print(output)
