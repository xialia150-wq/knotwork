# Knotwork 简体中文汉化工具

这个文件夹保存了本 fork（`xialia150-wq/knotwork`，分支 `zh-build`）的汉化工具和全部译文，用于在原项目更新后补翻新增内容、重新打包。

## 文件

| 文件 | 作用 |
|---|---|
| `zh.py` | 汉化工具，所有操作都从这里运行 |
| `l10n_lib.py` | 解析安卓语言文件、转义、校验占位符 |
| `prompt_zh.md` | 给翻译模型的要求和术语表（改术语就改这里） |
| `store_zh.json` | 全部界面译文，2629 条。`values-zh` 文件夹由它生成，不要手改 `values-zh` |
| `store_en_snapshot.json` | 每条译文翻译时依据的英文原文，用来发现原文改动 |
| `out/` | 运行时生成的批次文件，不需要提交 |

内置模板译文在 `app/src/main/assets/presets/l10n/zh.json`，帮助文档译文在 `app/src/main/assets/docs/zh/`。

## 汉化范围

- 界面文字：全部
- 内置流程与提示词模板的名称、说明、节点标题、建议卡片、标签：全部
- 帮助文档 `faq.md`、`troubleshooting.md`：全部
- 不翻译：给模型看的系统提示词、工具描述的模型版本、工具返回值（原项目设计如此，模型读英文更稳）

## 原项目更新后怎么跟进

最省事的做法：开一个 Claude 会话，说"Knotwork 原项目更新了，把上游合并到 zh-build 并补翻"，并把这个仓库交给它。它会按下面的步骤做。

### 1. 合并上游

在 `zh-build` 分支上合并原项目 `alexeyw/knotwork` 的 `main`。可能冲突的只有 `app/build.gradle.kts`、`catalog/build.gradle.kts` 里本 fork 加的几行（`"zh"` 语言和 `MissingTranslation` 开关），保留这几行即可。

### 2. 看看要补什么

```bash
python3 tools/l10n_zh/zh.py status
```

会列出新增和原文有改动的界面文字、需要更新的模板条目，以及过期的帮助文档。

### 3. 生成批次、翻译、合并

```bash
python3 tools/l10n_zh/zh.py batch                    # 生成 out/batch-日期-N.txt
# 把批次文件交给 Gemini："按文件开头的要求完成翻译，只输出一个 JSON 代码块。"
python3 tools/l10n_zh/zh.py apply batch-日期-1 回复.txt  # 校验并合并，自动重新生成 values-zh
```

校验不通过的条目会列出来，让模型只重翻这几条，再 `apply` 一次。

### 4. 复审（推荐）

```bash
python3 tools/l10n_zh/zh.py review batch-日期-1      # 生成复审包，交给 DeepSeek
python3 tools/l10n_zh/zh.py apply-review 复审回复.txt
```

复审建议要对照英文原文逐条看，不要照单全收：复审模型也会理解错。

### 5. 模板和帮助文档

```bash
python3 tools/l10n_zh/zh.py presets-batch            # 模板有改动时
python3 tools/l10n_zh/zh.py presets-apply 回复.txt
python3 tools/l10n_zh/zh.py docs-batch faq           # 文档过期时（faq 或 troubleshooting）
python3 tools/l10n_zh/zh.py docs-apply faq 回复.txt  # 会检查锚点、标题、链接，不通过不写入
```

`presets-batch` 和 `docs-batch` 以原项目维护的西班牙语译文为参照，确定有哪些条目和锚点。

### 6. 提交并打包

提交到 `zh-build` 分支后，GitHub Actions（`.github/workflows/build-zh.yml`）会自动：生成帮助文档索引 → 打包 `foss` 版 → 用固定签名签名 → 发布到 Releases 的 `zh-latest`。手机上下载后直接覆盖安装。

只改动 `tools/` 文件夹不会触发打包。

## 签名

打包使用仓库 Secrets 里的 4 个值（`RELEASE_KEYSTORE_BASE64`、`RELEASE_KEYSTORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`）。签名证书 SHA-256：

```
28de336e6ee02a60707275695ea6a8cc7fc522041dbbb30f1592b599ede862a2
```

每次发布的说明里都会写上证书指纹，和上面一致才说明签名正确。密钥文件本身由你自己保管，**不在仓库里，也不要放进仓库**。丢失密钥后新版本就无法覆盖安装旧版本。

## 已有的翻译约定

- 术语以 `prompt_zh.md` 里的术语表为准，例如 Pipeline = 流程、Provider = 服务商、Activate = 启用、Clarification = 追问。
- 不用英文破折号" — "，按语义改成逗号、冒号或括号。
- 中文与英文、数字之间加半角空格；称呼用"你"。
- 菜单路径写成“设置 → 模型”这样带引号的形式。
- 程序用的值不翻：`interface_language_tag` 固定为 `zh`；需要手动输入的确认词（如 `REPLACE`）保持英文。
