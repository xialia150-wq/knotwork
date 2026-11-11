#!/usr/bin/env python3
"""Simplified Chinese (zh) localisation tool for this fork of Knotwork.

Run from the repository root:

    python3 tools/l10n_zh/zh.py status
    python3 tools/l10n_zh/zh.py batch               # write translation batches for new/changed strings
    python3 tools/l10n_zh/zh.py apply BATCH REPLY   # merge a model's JSON reply, regenerate values-zh
    python3 tools/l10n_zh/zh.py review BATCH...     # write a review pack for the given batches
    python3 tools/l10n_zh/zh.py apply-review REPLY  # merge a review reply ({"file": {"name": {"zh":..}}})
    python3 tools/l10n_zh/zh.py build               # regenerate values-zh from the store only
    python3 tools/l10n_zh/zh.py presets-batch       # batch for new/changed preset texts (zh.json)
    python3 tools/l10n_zh/zh.py presets-apply REPLY
    python3 tools/l10n_zh/zh.py docs-batch NAME     # NAME = faq | troubleshooting
    python3 tools/l10n_zh/zh.py docs-apply NAME REPLY

State lives next to this file:
    store_zh.json          translations, {"app/strings_x.xml": {"name": "译文"}}
    store_en_snapshot.json the English each translation was made from (detects changed source)
    out/                   generated batches and manifest (not needed in git)
"""
import glob
import hashlib
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, HERE)
from l10n_lib import parse_file, translatable, escape_android, check_pair  # noqa: E402

STORE = os.path.join(HERE, 'store_zh.json')
SNAP = os.path.join(HERE, 'store_en_snapshot.json')
OUT = os.path.join(HERE, 'out')
MANIFEST = os.path.join(OUT, 'manifest.json')
PROMPT = os.path.join(HERE, 'prompt_zh.md')
FIXED = {'interface_language_tag': 'zh'}   # program values, never sent to a model
BATCH_MAX = 240


def load(p, default):
    return json.load(open(p, encoding='utf-8')) if os.path.exists(p) else default


def save(p, data):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(data, open(p, 'w', encoding='utf-8'), ensure_ascii=False, indent=1, sort_keys=True)


def source_files():
    for mod in ('app', 'catalog'):
        for f in sorted(glob.glob(os.path.join(ROOT, mod, 'src/main/res/values/strings*.xml'))):
            yield f'{mod}/{os.path.basename(f)}', f


def src_path(key):
    mod, base = key.split('/', 1)
    return os.path.join(ROOT, mod, 'src/main/res/values', base)


def english(key):
    return {e['name']: e for e in parse_file(src_path(key)) if translatable(e) and e['en']}


def todo():
    """(key, name, kind) for every translatable string missing from the store or whose English changed."""
    st, snap = load(STORE, {}), load(SNAP, {})
    out = []
    for key, _ in source_files():
        for name, e in english(key).items():
            if name in FIXED:
                continue
            if name not in st.get(key, {}):
                out.append((key, name, 'new'))
            elif snap.get(key, {}).get(name) != e['en']:
                out.append((key, name, 'changed'))
    return out


def cmd_status(_):
    t = todo()
    st = load(STORE, {})
    print(f'strings translated: {sum(len(v) for v in st.values())}')
    print(f'strings to do: {len(t)} (new {sum(1 for x in t if x[2]=="new")}, changed {sum(1 for x in t if x[2]=="changed")})')
    for key, n in Counter(k for k, _, _ in t).most_common():
        print(f'  {key}: {n}')
    p = presets_todo()
    print(f'preset texts to do: {len(p)}')
    for name in ('faq', 'troubleshooting'):
        print(f'doc {name}: {"up to date" if doc_current(name) else "STALE or missing, run docs-batch " + name}')


def write_batches(items, prefix, note=''):
    prompt = open(PROMPT, encoding='utf-8').read()
    manifest = load(MANIFEST, {})
    os.makedirs(OUT, exist_ok=True)
    titles = []
    for i in range(0, len(items), BATCH_MAX):
        chunk = items[i:i + BATCH_MAX]
        title = f'{prefix}-{i // BATCH_MAX + 1}'
        payload, names = {}, {}
        for key, name, kind in chunk:
            e = english(key)[name]
            item = {'en': e['en']}
            if e['note']:
                item['note'] = e['note']
            if e['kind'] == 'plurals':
                item['plural'] = True
            payload.setdefault(key, {})[name] = item
            names.setdefault(key, []).append(name)
        extra = ''
        if any('_syn_' in n for v in names.values() for n in v):
            extra = ('====================\n附加规则：设置搜索的同义词\n====================\n'
                     '字符串名含 "_syn_" 的条目是搜索关键词：写出中文关键词，用半角逗号加空格分隔，再把原文英文关键词原样附在后面。不要加句号。\n\n')
        text = (prompt.replace('{EXTRA}', extra).replace('{BATCH}', title).replace('{COUNT}', str(len(chunk)))
                + '\n' + json.dumps(payload, ensure_ascii=False, indent=1) + '\n')
        open(os.path.join(OUT, title + '.txt'), 'w', encoding='utf-8').write(text)
        manifest[title] = names
        titles.append(title)
        print(f'{title}: {len(chunk)} 条 -> {os.path.relpath(os.path.join(OUT, title + ".txt"), ROOT)}')
    save(MANIFEST, manifest)
    return titles


def cmd_batch(_):
    t = todo()
    if not t:
        print('nothing to translate')
        return
    import datetime
    write_batches(t, 'batch-' + datetime.date.today().isoformat())


def extract_json(path):
    raw = open(path, encoding='utf-8').read()
    m = re.search(r'[\[{].*[\]}]', raw, re.S)
    return json.loads(m.group(0))


def merge(reply, expect=None):
    st, snap = load(STORE, {}), load(SNAP, {})
    ok, problems = 0, []
    for key, d in reply.items():
        if not os.path.exists(src_path(key)):
            problems.append((key, '', '未知文件'))
            continue
        en = english(key)
        for name, zh in d.items():
            if isinstance(zh, dict):
                zh = zh.get('zh')
            if name not in en:
                problems.append((key, name, '多出的条目名'))
                continue
            errs = check_pair(en[name]['en'], zh)
            if errs:
                problems.append((key, name, '; '.join(errs)))
                continue
            st.setdefault(key, {})[name] = zh
            snap.setdefault(key, {})[name] = en[name]['en']
            ok += 1
        if expect is not None:
            for name in expect.get(key, []):
                if name not in d:
                    problems.append((key, name, '缺少该条目'))
    save(STORE, st)
    save(SNAP, snap)
    print(f'accepted {ok}, problems {len(problems)}')
    for p in problems:
        print('  PROBLEM', p)
    build()


def cmd_apply(args):
    title, reply = args
    expect = load(MANIFEST, {}).get(title)
    if expect is None:
        sys.exit(f'unknown batch {title}; see {MANIFEST}')
    merge(extract_json(reply), expect)


def cmd_apply_review(args):
    merge(extract_json(args[0]))


def build():
    st = load(STORE, {})
    for key, name in [(k, n) for k, _ in source_files() for n in FIXED if n in english(k)]:
        st.setdefault(key, {})[name] = FIXED[name]
    n_files = 0
    for key, d in sorted(st.items()):
        if not os.path.exists(src_path(key)):
            continue
        mod, base = key.split('/', 1)
        lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources xmlns:tools="http://schemas.android.com/tools">']
        for e in parse_file(src_path(key)):
            if not translatable(e):
                continue
            if not e['en']:
                lines.append(f'    <string {e["attrs"].strip()} />')
                continue
            zh = d.get(e['name'])
            if zh is None:
                continue          # untranslated: Android falls back to English
            body = escape_android(zh)
            if e['kind'] == 'string':
                lines.append(f'    <string {e["attrs"].strip()}>{body}</string>')
            else:
                lines += [f'    <plurals {e["attrs"].strip()}>', f'        <item quantity="other">{body}</item>', '    </plurals>']
        lines.append('</resources>')
        dst = os.path.join(ROOT, mod, 'src/main/res/values-zh', base)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        open(dst, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
        ET.parse(dst)
        n_files += 1
    print(f'values-zh regenerated: {n_files} files')


def cmd_build(_):
    build()


def cmd_review(titles):
    manifest, st = load(MANIFEST, {}), load(STORE, {})
    only = {(k, n) for t in titles for k, v in manifest[t].items() for n in v}
    payload = {}
    for key, name in sorted(only):
        e = english(key)[name]
        item = {'en': e['en'], 'zh': st[key][name]}
        if e['note']:
            item['note'] = e['note']
        payload.setdefault(key, {})[name] = item
    p = open(PROMPT, encoding='utf-8').read()
    gloss = p[p.index('四、保留英文、不翻译'):p.index('{EXTRA}')].rstrip('= \n')
    head = f'''【复审任务，共 {len(only)} 条】

你是资深的中文软件本地化审校。下面是开源安卓应用 Knotwork（安卓上的 AI agent 应用）的界面文字：英文原文（en）和现有简体中文译文（zh）。请逐条审校。

只输出一个 JSON 代码块，并且只包含需要修改的条目：{{"来源文件": {{"字符串名": {{"zh": "修改后的译文", "why": "一句话原因"}}}}}}
没有问题的条目不要输出；只有确实更好时才改，不要只做同义词替换；不要联网搜索。
重点：误译漏译、术语不一致、生硬直译、按钮过长或多了句号、标点（中文全角，不用英文破折号" — "）、中英文间空格、用"你"不用"您"。
修改时占位符（%1$s、%2$d、%%）、换行数量、$ 变量必须与原文一致。

====================
{gloss}

====================
待审校内容
====================
'''
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, 'review-' + '+'.join(titles) + '.txt')
    open(out, 'w', encoding='utf-8').write(head + json.dumps(payload, ensure_ascii=False, indent=1) + '\n')
    print(os.path.relpath(out, ROOT))


# ---------------- presets ----------------
PRESETS_ES = os.path.join(ROOT, 'app/src/main/assets/presets/l10n/es.json')
PRESETS_ZH = os.path.join(ROOT, 'app/src/main/assets/presets/l10n/zh.json')


def flat_pairs(obj, lang):
    out = {}

    def walk(x, path):
        if isinstance(x, dict) and set(x) == {'en', lang}:
            out[path] = x
            return
        if isinstance(x, dict):
            for k, v in x.items():
                walk(v, f'{path}/{k}' if path else k)
        elif isinstance(x, list):
            for i, v in enumerate(x):
                walk(v, f'{path}/{i}')
    walk({'pipelines': obj.get('pipelines', {}), 'prompts': obj.get('prompts', {})}, '')
    return out


def presets_todo():
    """Uses the maintained Spanish file as the reference for which preset texts exist and their current English."""
    ref = flat_pairs(load(PRESETS_ES, {}), 'es')
    zh = flat_pairs(load(PRESETS_ZH, {}), 'zh')
    return {p: v['en'] for p, v in ref.items() if p not in zh or zh[p]['en'] != v['en']}


def cmd_presets_batch(_):
    t = presets_todo()
    if not t:
        print('presets up to date')
        return
    head = ('【任务：Knotwork 内置流程与提示词模板，共 %d 条】\n输入是 JSON，键是条目路径，值是英文原文。'
            '只输出一个 JSON 代码块 {"路径": "中文译文"}，路径不能增删改，不要联网搜索。\n'
            'name=名称，description=说明，nodes/*=画布节点标题（很短），samplePrompts/*=聊天页建议卡片（像真人会发的话），tags/*=标签。\n'
            '术语与风格见下：\n\n') % len(t)
    p = open(PROMPT, encoding='utf-8').read()
    head += p[p.index('三、风格'):p.index('{EXTRA}')]
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, 'presets-batch.txt')
    open(out, 'w', encoding='utf-8').write(head + '\n' + json.dumps(t, ensure_ascii=False, indent=1) + '\n')
    print(os.path.relpath(out, ROOT), len(t))


def cmd_presets_apply(args):
    reply = extract_json(args[0])
    es = load(PRESETS_ES, {})
    zh_now = flat_pairs(load(PRESETS_ZH, {}), 'zh')

    def conv(x, path):
        if isinstance(x, dict) and set(x) == {'en', 'es'}:
            if path in reply:
                return {'en': x['en'], 'zh': reply[path]}
            if path in zh_now and zh_now[path]['en'] == x['en']:
                return {'en': x['en'], 'zh': zh_now[path]['zh']}
            return {'en': x['en'], 'zh': x['en']}     # untranslated: shows English
        if isinstance(x, dict):
            return {k: conv(v, f'{path}/{k}' if path else k) for k, v in x.items()}
        if isinstance(x, list):
            return [conv(v, f'{path}/{i}') for i, v in enumerate(x)]
        return x
    out = {'formatVersion': es['formatVersion'], 'language': 'zh'}
    out.update(conv({'pipelines': es['pipelines'], 'prompts': es['prompts']}, ''))
    open(PRESETS_ZH, 'w', encoding='utf-8').write(json.dumps(out, ensure_ascii=False, indent=2) + '\n')
    print('zh.json written; still to do:', len(presets_todo()))


# ---------------- bundled docs ----------------
HEAD = re.compile(r'^#{1,6} ')
LINK = re.compile(r'\]\(([^)\s]+)\)')


def doc_source_line(name):
    en = open(os.path.join(ROOT, 'docs', name + '.md'), encoding='utf-8').read()
    return f'<!-- translation of docs/{name}.md, sha256 {hashlib.sha256(en.encode()).hexdigest()} -->', en


def doc_current(name):
    p = os.path.join(ROOT, 'app/src/main/assets/docs/zh', name + '.md')
    return os.path.exists(p) and open(p, encoding='utf-8').readline().rstrip('\n') == doc_source_line(name)[0]


def doc_anchors(name):
    """English anchors in order, taken from the maintained Spanish translation."""
    es = open(os.path.join(ROOT, 'app/src/main/assets/docs/es', name + '.md'), encoding='utf-8').read()
    return re.findall(r'^<!-- en: (\S+) -->$', es, re.M)


def cmd_docs_batch(args):
    name = args[0]
    _, en = doc_source_line(name)
    anchors, out_lines, k = doc_anchors(name), [], 0
    for line in en.split('\n'):
        if HEAD.match(line):
            out_lines.append(f'<!-- en: {anchors[k]} -->')
            k += 1
        out_lines.append(line)
    if k != len(anchors):
        sys.exit(f'{name}: {k} headings but the Spanish translation marks {len(anchors)} anchors; '
                 'the Spanish is out of date, translate from the English anchors by hand')
    p = open(PROMPT, encoding='utf-8').read()
    head = (f'【任务：Knotwork 帮助文档 {name}.md 翻译成简体中文】\n'
            '只输出一个 ```markdown 代码块，里面是完整中文文档，不要省略。不要联网搜索。\n'
            '必须遵守：<!-- en: xxx --> 行原样保留且紧挨在下一行标题上方；标题数量、层级、顺序与原文一致；'
            '链接 [文字](目标) 只翻译方括号里的文字，目标原样保留；反引号内容原样保留；不添加 HTML 和图片；'
            '界面按钮和菜单名按术语表译并用“”括起来；不要过度意译或添加原文没有的修饰。\n\n')
    head += p[p.index('三、风格'):p.index('{EXTRA}')]
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, f'docs-{name}.txt')
    open(out, 'w', encoding='utf-8').write(head + '\n\n待翻译的文档：\n\n' + '\n'.join(out_lines) + '\n')
    print(os.path.relpath(out, ROOT))


def cmd_docs_apply(args):
    name, reply = args
    src, en = doc_source_line(name)
    t = open(reply, encoding='utf-8').read().strip('\n')
    t = re.sub(r'^```(?:markdown)?\n|\n```$', '', t)
    t = re.sub(r'\n{3,}', '\n\n', t)
    lines = t.split('\n')
    errors = []
    if re.findall(r'^<!-- en: (\S+) -->$', t, re.M) != doc_anchors(name):
        errors.append('anchors differ from the English')
    if any(l.startswith('<!-- en:') and not HEAD.match(lines[i + 1]) for i, l in enumerate(lines[:-1])):
        errors.append('an anchor marker is not directly above a heading')
    if [len(l.split()[0]) for l in en.split('\n') if HEAD.match(l)] != [len(l.split()[0]) for l in lines if HEAD.match(l)]:
        errors.append('heading count or levels differ')
    if sorted(LINK.findall(en)) != sorted(LINK.findall(t)):
        errors.append('link targets differ')
    if re.search(r'<(?!!--)[a-zA-Z/][^>]*>', t):
        errors.append('contains HTML')
    if errors:
        sys.exit('NOT written: ' + '; '.join(errors))
    dst = os.path.join(ROOT, 'app/src/main/assets/docs/zh', name + '.md')
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    open(dst, 'w', encoding='utf-8').write(src + '\n' + t + '\n')
    print('written', os.path.relpath(dst, ROOT))


COMMANDS = {
    'status': cmd_status, 'batch': cmd_batch, 'apply': cmd_apply, 'apply-review': cmd_apply_review,
    'review': cmd_review, 'build': cmd_build, 'presets-batch': cmd_presets_batch,
    'presets-apply': cmd_presets_apply, 'docs-batch': cmd_docs_batch, 'docs-apply': cmd_docs_apply,
}

if __name__ == '__main__':
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        sys.exit(__doc__)
    COMMANDS[sys.argv[1]](sys.argv[2:])
