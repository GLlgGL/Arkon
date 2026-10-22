#!/usr/bin/env python3
"""Builds USER_GUIDE.html (self-contained) from USER_GUIDE.md.  Usage: python3 tools/guide_html.py [out.html]"""
import base64, html, os, re, sys
import markdown

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = open(os.path.join(ROOT, 'USER_GUIDE.md'), encoding='utf-8').read()
# Re-indent nested lists by their real depth (GitHub-style 2/3-space nesting -> 4 spaces per level,
# which Python-Markdown needs). Continuation lines follow the item they belong to.
def _normalise_lists(text):
    out, stack, depth, in_code = [], [], -1, False
    item = re.compile(r'^( *)([-*] |\d+\. )')
    for line in text.split('\n'):
        if line.lstrip().startswith('```'):
            in_code = not in_code
        if in_code:
            out.append(line); continue
        m = item.match(line)
        if m:
            n = len(m.group(1))
            while stack and stack[-1] > n:
                stack.pop()
            if not stack or n > stack[-1]:
                stack.append(n)
            new_depth = len(stack) - 1
            prev = out[-1] if out else ''
            if prev.strip() and (not item.match(prev) or new_depth < depth):
                out.append('')       # a list after a paragraph, or stepping back out a level, needs a blank line
            depth = new_depth
            out.append(' ' * (4 * depth) + line.lstrip())
        elif line.strip() == '':
            out.append(line)
        elif line.startswith(' ') and stack:
            # continuation: it belongs to the deepest item it is indented under
            c = len(line) - len(line.lstrip())
            d = max([i for i, ind in enumerate(stack) if ind < c], default=0)
            out.append(' ' * (4 * (d + 1)) + line.lstrip())
        else:
            stack, depth = [], -1
            out.append(line)
    return '\n'.join(out)
src = _normalise_lists(src)
out_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'USER_GUIDE.html')
md = markdown.Markdown(extensions=['tables', 'fenced_code', 'toc', 'sane_lists'], extension_configs={'toc': {'toc_depth': '2-3'}})
body = md.convert(src)
logo = base64.b64encode(open(os.path.join(ROOT, 'src/main/resources/static/img/arkon-logo.svg'), 'rb').read()).decode()

m = re.search(r'<h1[^>]*>(.*?)</h1>', body, re.S)
title = re.sub('<[^>]+>', '', m.group(1)) if m else 'Arkon – User Guide'
if m: body = body[:m.start()] + body[m.end():]
# the guide's own contents list: the sidebar replaces it
body = re.sub(r'<(p|h[23])[^>]*>\s*(<strong>)?Contents(</strong>)?\s*</\1>\s*<ol>.*?</ol>', '', body, count=1, flags=re.S)
body = re.sub(r'(<table>.*?</table>)', r'<div class="table-wrap">\1</div>', body, flags=re.S)
body = body.replace('<blockquote>', '<blockquote class="note">')

tops = []
for t in md.toc_tokens:
    tops.extend(t['children'] if t['level'] == 1 else [t])
toc = ['<ol>']
for t in tops:
    if t['name'].strip().lower() == 'contents': continue
    toc.append(f'<li><a href="#{t["id"]}">{html.escape(t["name"])}</a>')
    if t.get('children'):
        toc.append('<ol>' + ''.join(f'<li><a href="#{k["id"]}">{html.escape(k["name"])}</a></li>' for k in t['children']) + '</ol>')
    toc.append('</li>')
toc.append('</ol>')
toc = ''.join(toc)

CSS = """
:root { --navy: #0b4779; --navy-deep: #08355b; --amber: #ffa100; --amber-deep: #c77b00;
  --ink: #1b2430; --muted: #5b6878; --line: #dfe5ec; --paper: #ffffff; --tint: #f3f7fb; --code: #eef2f6; color-scheme: light dark; }
@media (prefers-color-scheme: dark) {
  :root { --ink: #e3e9f0; --muted: #9aa8b8; --line: #2d3947; --paper: #141b24; --tint: #1a2330; --code: #1f2a37; --navy: #8fbbe8; --navy-deep: #0b4779; } }
* { box-sizing: border-box; }
html { scroll-behavior: smooth; scroll-padding-top: 1.5rem; }
@media (prefers-reduced-motion: reduce) { html { scroll-behavior: auto; } }
body { margin: 0; background: var(--paper); color: var(--ink); font: 17px/1.65 "Lato", "Segoe UI", system-ui, -apple-system, "Helvetica Neue", Arial, sans-serif; }
a { color: var(--navy); text-underline-offset: .18em; }
a:hover { color: var(--amber-deep); }
a:focus-visible, button:focus-visible { outline: 3px solid var(--amber); outline-offset: 2px; border-radius: 3px; }
.masthead { background: var(--navy-deep); color: #fff; padding: 2.2rem 1.5rem 2rem; }
.masthead .in { max-width: 1180px; margin: 0 auto; display: flex; gap: 1.2rem; align-items: center; }
.masthead img { width: 72px; height: 72px; flex: none; filter: drop-shadow(0 3px 6px rgba(0,0,0,.35)); }
.masthead h1 { margin: 0; font-size: clamp(1.7rem, 3.5vw, 2.5rem); font-weight: 900; letter-spacing: -.01em; line-height: 1.1; }
.masthead p { margin: .35rem 0 0; color: #c9dcf0; font-size: 1.02rem; }
.wrap { max-width: 1180px; margin: 0 auto; display: grid; grid-template-columns: 290px minmax(0, 1fr); gap: 3rem; padding: 2rem 1.5rem 4rem; }
nav.toc { position: sticky; top: 1rem; align-self: start; max-height: calc(100vh - 2rem); overflow: auto; font-size: .92rem; line-height: 1.4;
  border-right: 1px solid var(--line); padding-right: 1.2rem; }
nav.toc h2 { font-size: .95rem; margin: 0 0 .6rem; color: var(--muted); font-weight: 700; }
nav.toc ol { list-style: none; margin: 0; padding: 0; }
nav.toc .list > ol > li { margin: 0 0 .55rem; }
nav.toc .list > ol > li > a { font-weight: 700; text-decoration: none; color: var(--ink); }
nav.toc ol ol { margin: .3rem 0 0 .8rem; border-left: 2px solid var(--line); padding-left: .7rem; }
nav.toc ol ol li { margin: .2rem 0; }
nav.toc ol ol a { color: var(--muted); text-decoration: none; }
nav.toc a:hover { color: var(--amber-deep); }
nav.toc a.on { color: var(--navy); font-weight: 700; }
.toc-toggle { display: none; }
main { max-width: 76ch; min-width: 0; }
main h2 { font-size: 1.65rem; font-weight: 900; color: var(--navy); margin: 3rem 0 .8rem; line-height: 1.2; display: flex; gap: .65rem; align-items: center; }
main h2::before { content: ""; flex: none; width: 15px; height: 14px; background: var(--amber); clip-path: polygon(0 0, 100% 0, 78% 100%, 22% 100%); }
main > h2:first-child { margin-top: 0; }
main h3 { font-size: 1.2rem; margin: 2rem 0 .5rem; }
main h4 { font-size: 1.02rem; margin: 1.5rem 0 .4rem; }
main hr { border: 0; border-top: 1px solid var(--line); margin: 2.6rem 0 0; }
main ul, main ol { padding-left: 1.4rem; }
main li { margin: .25rem 0; }
main li::marker { color: var(--amber-deep); }
code { font-family: ui-monospace, "Cascadia Code", Consolas, monospace; font-size: .88em; background: var(--code); padding: .1em .35em; border-radius: 4px; }
pre { background: var(--code); padding: 1rem 1.1rem; border-radius: 8px; overflow: auto; line-height: 1.5; border-left: 4px solid var(--navy); }
pre code { background: none; padding: 0; font-size: .86rem; }
blockquote.note { margin: 1.2rem 0; padding: .8rem 1.1rem; background: var(--tint); border-left: 4px solid var(--amber); border-radius: 0 8px 8px 0; }
blockquote.note p { margin: .2rem 0; }
.table-wrap { overflow-x: auto; margin: 1rem 0 1.4rem; border: 1px solid var(--line); border-radius: 8px; }
table { border-collapse: collapse; width: 100%; font-size: .93rem; line-height: 1.45; }
th { background: var(--navy-deep); color: #fff; text-align: left; font-weight: 700; padding: .55rem .75rem; }
td { padding: .5rem .75rem; border-top: 1px solid var(--line); vertical-align: top; }
tbody tr:nth-child(even) td { background: var(--tint); }
.back { position: fixed; right: 1.2rem; bottom: 1.2rem; background: var(--navy-deep); color: #fff; border: 0; border-radius: 999px;
  width: 2.8rem; height: 2.8rem; font-size: 1.2rem; cursor: pointer; box-shadow: 0 4px 12px rgba(0,0,0,.25); opacity: 0; pointer-events: none; transition: opacity .2s; }
.back.show { opacity: 1; pointer-events: auto; }
footer { border-top: 1px solid var(--line); color: var(--muted); font-size: .88rem; text-align: center; padding: 1.4rem; }
@media (max-width: 900px) {
  .wrap { grid-template-columns: 1fr; gap: 1rem; padding-top: 1rem; }
  nav.toc { position: static; max-height: none; border-right: 0; padding: 0; }
  .toc-toggle { display: inline-flex; gap: .5rem; align-items: center; font: inherit; font-weight: 700; background: var(--tint); color: var(--ink);
    border: 1px solid var(--line); border-radius: 8px; padding: .55rem .9rem; cursor: pointer; }
  nav.toc .list { display: none; margin-top: .8rem; }
  nav.toc.open .list { display: block; }
  nav.toc h2 { display: none; }
  body { font-size: 16px; } }
@media print {
  .masthead { background: none; color: #000; padding: 0 0 1rem; } .masthead p { color: #333; }
  nav.toc, .back, .toc-toggle { display: none !important; }
  .wrap { display: block; padding: 0; } main { max-width: none; }
  main h2 { break-before: page; color: #0b4779; } main > h2:first-child { break-before: avoid; }
  thead { display: table-header-group; } tr, pre, blockquote { break-inside: avoid; }
  a { color: inherit; text-decoration: none; } body { font-size: 11pt; } }
"""
JS = """
(function () {
  var nav = document.querySelector('nav.toc'), btn = document.querySelector('.toc-toggle'), back = document.querySelector('.back');
  btn.addEventListener('click', function () { var o = nav.classList.toggle('open'); btn.setAttribute('aria-expanded', String(o)); });
  nav.addEventListener('click', function (e) { if (e.target.tagName === 'A') { nav.classList.remove('open'); btn.setAttribute('aria-expanded', 'false'); } });
  var links = Array.prototype.slice.call(nav.querySelectorAll('a'));
  var heads = links.map(function (a) { return document.getElementById(decodeURIComponent(a.getAttribute('href').slice(1))); });
  var last = -1, ticking = false;
  function update() {
    ticking = false;
    back.classList.toggle('show', window.scrollY > 600);
    var current = 0;
    heads.forEach(function (h, i) { if (h && h.getBoundingClientRect().top < 120) current = i; });
    if (current === last) return;
    last = current;
    links.forEach(function (a, i) { a.classList.toggle('on', i === current); });
    // keep the current chapter visible in the sidebar
    var a = links[current];
    if (a && getComputedStyle(nav).position === 'sticky') {
      var top = a.getBoundingClientRect().top - nav.getBoundingClientRect().top + nav.scrollTop;
      if (top < nav.scrollTop + 40 || top > nav.scrollTop + nav.clientHeight - 60) nav.scrollTop = top - nav.clientHeight / 3;
    }
  }
  window.addEventListener('scroll', function () { if (!ticking) { ticking = true; requestAnimationFrame(update); } }, { passive: true });
  update();
})();
"""
page = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<link rel="icon" href="data:image/svg+xml;base64,{logo}">
<style>{CSS}</style>
</head>
<body>
<header class="masthead"><div class="in">
  <img src="data:image/svg+xml;base64,{logo}" alt="Arkon">
  <div><h1>{html.escape(title)}</h1><p>Sales, stock, finance and accounting in one place.</p></div>
</div></header>
<div class="wrap">
  <nav class="toc" aria-label="Contents">
    <button class="toc-toggle" type="button" aria-expanded="false">☰ Contents</button>
    <h2>Contents</h2>
    <div class="list">{toc}</div>
  </nav>
  <main>
{body}
  </main>
</div>
<footer>Arkon – Business Management · User guide</footer>
<button class="back" type="button" aria-label="Back to top" onclick="window.scrollTo({{top: 0}})">↑</button>
<script>{JS}</script>
</body>
</html>
"""
open(out_path, 'w', encoding='utf-8').write(page)
print('written', out_path, len(page) // 1024, 'KB')
