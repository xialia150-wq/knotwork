"""Shared helpers: parse Android strings XML (raw inner text), de/re-escape, validate."""
import re, os, glob, json

TOKEN_RE = re.compile(r'<!--(?P<c>.*?)-->|<string\s+(?P<se>[^>]*?)/>|<string\s+(?P<sa>[^>]*?)>(?P<sb>.*?)</string>|<plurals\s+(?P<pa>[^>]*?)>(?P<pb>.*?)</plurals>', re.S)
ITEM_RE = re.compile(r'<item\s+quantity="(?P<q>\w+)"\s*>(?P<body>.*?)</item>', re.S)
NAME_RE = re.compile(r'name="([^"]+)"')
COMMENT_RE = re.compile(r'<!--(.*?)-->', re.S)
PH_RE = re.compile(r'%(?:\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[a-zA-Z]|%%')
TAG_RE = re.compile(r'</?[a-zA-Z][^>]*>')
ENT_RE = re.compile(r'&(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);')
VAR_RE = re.compile(r'\$[A-Z_]{3,}')

def unescape_android(s):
    s = re.sub(r'\s+', ' ', s).strip()      # Android collapses raw whitespace/newlines
    if len(s) >= 2 and s[0] == '"' and s[-1] == '"':
        s = s[1:-1]                          # quoted: inner whitespace is significant
    out, i = [], 0
    while i < len(s):
        c = s[i]
        if c == '\\' and i + 1 < len(s):
            n = s[i+1]
            if n == 'n': out.append('\n'); i += 2; continue
            if n == 't': out.append('\t'); i += 2; continue
            if n in '\'"@?\\': out.append(n); i += 2; continue
            if n == 'u' and i + 5 < len(s):
                out.append(chr(int(s[i+2:i+6], 16))); i += 6; continue
        out.append(c); i += 1
    return ''.join(out)

def escape_android(s):
    s = s.replace('\\', '\\\\').replace("'", "\\'").replace('"', '\\"').replace('\n', '\\n').replace('\t', '\\t')
    if s[:1] in '@?':
        s = '\\' + s
    if s != s.strip() or '  ' in s:
        s = '"' + s.replace('\\"', '\\"') + '"'   # keep leading/trailing spaces
    return s

def parse_file(path):
    """Ordered entries: dict(kind,name,attrs,en,note,one). Plurals: en is the 'other' form."""
    text = open(path, encoding='utf-8').read()
    entries, pending, last_end = [], [], 0
    for m in TOKEN_RE.finditer(text):
        gap = text[last_end:m.start()]
        if gap.count('\n') > 1:          # blank line: comment was a section header, not a note
            pending = []
        last_end = m.end()
        if m.group('c') is not None:
            pending.append(re.sub(r'\s+', ' ', m.group('c')).strip()); continue
        note = ' '.join(x for x in pending if x)[:400]; pending = []
        if m.group('se') is not None:
            attrs = m.group('se'); name = NAME_RE.search(attrs).group(1)
            entries.append(dict(kind='string', name=name, attrs=attrs, en='', note=note, one=None))
        elif m.group('sa') is not None:
            attrs = m.group('sa'); name = NAME_RE.search(attrs).group(1)
            entries.append(dict(kind='string', name=name, attrs=attrs, en=unescape_android(m.group('sb')), note=note, one=None))
        else:
            attrs = m.group('pa'); name = NAME_RE.search(attrs).group(1)
            items = {im.group('q'): im.group('body') for im in ITEM_RE.finditer(m.group('pb'))}
            src = items.get('other') or list(items.values())[-1]
            entries.append(dict(kind='plurals', name=name, attrs=attrs, en=unescape_android(src), note=note,
                                one=unescape_android(items['one']) if 'one' in items else None))
    return entries

def translatable(e):
    return 'translatable="false"' not in e['attrs']

def sig(s):
    return (sorted(PH_RE.findall(s)), sorted(TAG_RE.findall(s)), sorted(ENT_RE.findall(s)), sorted(VAR_RE.findall(s)))

def check_pair(en, zh):
    """Return list of problems for one translated string."""
    p = []
    if not isinstance(zh, str) or not zh.strip():
        return ['译文为空']
    a, b = sig(en), sig(zh)
    if a[0] != b[0]: p.append(f'占位符不一致: 原文{a[0]} 译文{b[0]}')
    if a[1] != b[1]: p.append(f'标签不一致: 原文{a[1]} 译文{b[1]}')
    if re.search(r'&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)', zh): p.append('出现了未转义的 & 符号')
    if '<' in zh and not a[1]: p.append('出现了 < 符号')
    if a[3] != b[3]: p.append(f'$变量不一致: 原文{a[3]} 译文{b[3]}')
    if en.count('\n') != zh.count('\n'): p.append('换行数量不一致')
    return p
