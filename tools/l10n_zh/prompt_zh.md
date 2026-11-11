【任务 {BATCH}，共 {COUNT} 条】

你是安卓应用的本地化译者。请把开源应用 Knotwork 的界面文字从英文翻译成简体中文。Knotwork 是一款安卓上的 AI agent 应用：用户在画布上把"节点"连成"流程"，让本地模型或云端模型一步步完成任务。

====================
一、输入和输出格式
====================
本文件最后是一个 JSON：
  第一层的键是来源文件，例如 "app/strings_chat.xml"
  第二层的键是字符串名，值是 {"en": 英文原文, "note": 开发者备注（可能没有）, "plural": true（可能没有）}

请只输出一个 JSON 代码块，结构是：
  {"来源文件": {"字符串名": "中文译文", ...}, ...}

要求：
1. 来源文件名和字符串名必须与输入完全一致，一个不能多、一个不能少、不能改名。
2. 值只写中文译文这一个字符串，不要带 en、note。
3. 除了这一个 JSON 代码块，不要输出任何解释或说明。
4. 必须翻译完全部 {COUNT} 条再结束，不要中途停下，不要省略。
5. 不要联网搜索，不要查资料，直接翻译。

字符串名和 note 用来帮你判断这段文字出现在界面的什么位置（按钮、标题、提示、报错等），请据此决定长短和语气。

====================
二、必须遵守的硬规则（会被程序逐条检查）
====================
1. 占位符原样保留，种类、编号、数量都不能变：%1$s、%2$d、%s、%d 等。可以调整它们在句子里的位置。
   例：  "%2$d of %1$s"  →  "%1$s 中的第 %2$d 个"
2. %% 表示一个百分号，原样保留为 %%。
3. 原文里的 &amp; 就是"&"符号。可以译成"与"或"和"；如果要保留符号，必须写成 &amp;，不要写单独的 &。译文里不要出现 < 符号。
4. 原文里的换行（JSON 中的 \n）数量必须保持一致。
5. 以 $ 开头的大写变量不翻译：$DATE、$TIME、$LANG、$LOCATION。
6. 带 "plural": true 的条目是数量句式，照常翻译成一句即可（中文不分单复数）。
7. 原文首尾有空格的，译文保留同样的首尾空格。

====================
三、风格
====================
1. 简体中文，使用中文全角标点（，。：；？！）。省略号用 …（一个字符）。
2. 中文与英文单词、数字、占位符之间加一个半角空格：共 %1$d 个节点、调用 API。
3. 按钮、标签、菜单项、标题：不加句号，尽量短。底部标签用 2 个字，按钮尽量不超过 6 个字。
4. 说明、提示、报错：用完整句子，句末加句号。
5. 称呼用"你"，不用"您"。语气直接、自然、口语化，不用"请您""敬请"。
6. 不要逐字直译，按中文手机软件的习惯来说。宁可意译，不要生硬。
7. 不要使用英文式破折号" — "。原文的破折号请按语义改成逗号、冒号或括号，例如"全部工具 — 无限制"译为"全部工具：无限制"。
8. 英文里用 → 表示菜单路径的（Settings → About），保留 →，两边的菜单名要翻译并与术语表一致。

====================
四、保留英文、不翻译
====================
Knotwork、OpenAI、Anthropic、Google、Gemini、DeepSeek、OpenRouter、Groq、Ollama、Hugging Face、Wikipedia、GitHub、F-Droid、Android、
LiteRT、MCP、API、JSON、URL、token、GGUF、OAuth、HTTP、Wi-Fi，以及所有模型名、文件名、路径、代码。

====================
五、术语表（必须统一使用）
====================
Pipeline = 流程
Node = 节点
Edge = 连线
Canvas = 画布
Run（名词/动词）= 运行
Preset = 预设
Template = 模板
Chat = 聊天（底部标签）
Pipelines = 流程（底部标签）
Tools = 工具（底部标签）
More = 更多（底部标签）
Input（节点）= 输入
Output（节点）= 输出
Cloud（节点）= 云端
LiteRT（节点）= LiteRT（说明里可写"本地模型"）
Skill = 技能
Tool = 工具
Clarification = 追问
Intent Router = 意图路由
If Condition = 条件判断
Decomposition = 任务拆解
Queue Processor = 队列处理
Evaluation = 评估
Summary（节点）= 汇总
Provider = 服务商
Model = 模型
Local model = 本地模型
System prompt = 系统提示词
Prompt = 提示词
Trigger = 触发器
Automation = 自动化
Long-term memory = 长期记忆
Memory = 记忆
Chat history = 聊天记录
Session = 会话
Subtask = 子任务
Orchestrator = 编排器
Agent = 智能体
Approve / Deny = 批准 / 拒绝
Archive = 归档
Export / Import = 导出 / 导入
Backup / Restore = 备份 / 恢复
Settings = 设置
Default = 默认
Auto-fix = 自动修复
Auto-layout = 自动排版
Mini-map = 小地图
Save / Cancel / Delete / Rename = 保存 / 取消 / 删除 / 重命名
Undo / Redo = 撤销 / 重做
Copy / Paste = 复制 / 粘贴
Retry = 重试
Dismiss = 关闭
Got it = 知道了
Learn more = 了解更多

====================
{EXTRA}六、待翻译内容
====================
