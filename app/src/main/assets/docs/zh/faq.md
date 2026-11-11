<!-- translation of docs/faq.md, sha256 9014b36abf2b6d1e57c85e163f530c2b14450f44eaa327e0997a59b389ccb4c6 -->
<!-- en: frequently-asked-questions -->
# 常见问题

针对实际使用中常见疑问的简要解答。每一项都附有详尽说明页面的链接。

你想查看哪个页面？

- **本页面**解答*“能否支持、是否具备、位置在哪里、如果…会发生什么”*等问题。
- [`troubleshooting.md`](troubleshooting.md) 解答*“功能出现异常/报错”*的问题。
- [`user-guide.md`](user-guide.md) 详细介绍了各个界面和功能的运作机制，是行为规范的权威说明文档。

如果你想了解从全新安装到快速上手运行的最短路径，请参阅[下方的五步解答](#im-installed-now-what-do-i-actually-press)。

<!-- en: contents -->
## 目录

- [安装与选择模型](#installing-and-choosing-a-model)
- [哪些数据离开设备，哪些不会](#what-leaves-the-device-and-what-does-not)
- [流程与编辑器](#pipelines-and-the-editor)
- [触发器与后台运行](#triggers-and-background-work)
- [工具、MCP 与确认机制](#tools-mcp-and-confirmations)
- [外部自动化（Tasker、MacroDroid、adb）](#external-automation-tasker-macrodroid-adb)
- [运行限制与停止运行](#run-limits-and-stopping-a-run)
- [记忆](#memory)
- [已知局限](#known-limitations)

---

<!-- en: installing-and-choosing-a-model -->
## 安装与选择模型

<!-- en: what-do-i-need-to-run-this -->
### 运行该应用需要什么配置？

Android 14 或更高版本，以及为模型预留的大约 2 GB 可用 RAM。内存是主要的限制瓶颈；没有可用 GPU 的手机依然可以正常运行，只是速度较慢。详尽清单见 [README § 要求](../README.md#requirements)。

<!-- en: do-i-have-to-download-a-model-at-all -->
### 我必须下载模型吗？

要在设备端获得回答，必须下载：模型文件需要保存在你的手机上。另一种选择是使用你自备 API 密钥的云端服务商，这样无需下载任何文件。在获得回答前，二者必居其一；全新安装的应用两者均未预置。详见[新手入门](user-guide.md#getting-started)。

<!-- en: which-model-should-i-start-with -->
### 我应该从哪个模型开始？

新手引导会为你选定模型：每个使用场景都会指明所需模型并精确下载该模型。若手动选择，两个预设选项为 **Gemma 4 E2B** 和 **Gemma 4 E4B**；当存储空间或 RAM 紧张时，建议选择 E2B。详见[新手入门](user-guide.md#getting-started)。

<!-- en: which-cloud-providers-are-supported -->
### 支持哪些云端服务商？

托管服务商：OpenAI、Anthropic、Google Gemini、DeepSeek、OpenRouter 和 Groq，均需使用你自备的密钥。私有服务器：**Ollama** 或任何兼容 OpenAI API 的服务器。所有云端选项均需手动开启。它们位于“设置 → 模型 → 外部服务商”，每个服务商页面上的“测试连接”可以在不发送提示词的情况下检查密钥和地址。

<!-- en: can-i-point-it-at-my-own-openai-compatible-endpoint -->
### 我能否将其指向自定义的 OpenAI 兼容端点？

可以：添加一个“兼容 OpenAI 的服务器”，并按照服务器文档说明输入其地址，包括 `/v1`（vLLM、LM Studio 和 llama.cpp 均在该路径下提供 API）。密钥为选填项；若留空，则不会发送 `Authorization` 请求头。“测试连接”提取到服务器的模型列表后，即可选择模型，或直接输入模型 ID。

<!-- en: my-ollama-server-is-not-on-my-home-network-will-it-work -->
### 我的 Ollama 服务器不在家庭局域网内，能否正常使用？

如果使用 `https`，可以正常使用：位于“设置 → 模型 → 外部服务商”下的 Ollama 栏目支持配置其基础 URL。若使用明文 `http`，除非该地址属于专用局域网或你已明确批准该特定来源，否则将被直接拒绝，因为应用不会在公开互联网上发送未经加密的流量。

<!-- en: what-is-custom-model-url-and-can-it-use-authentication -->
### 什么是“自定义模型 URL”，它是否支持身份验证？

它用于通过直接链接向手机下载 `.litertlm` **文件**，而不是将应用指向留在远程服务器上的模型。它支持身份验证，但仅限使用 Bearer token 且仅适用于 `huggingface.co`：在“模型”页面填写 Hugging Face 访问 token 后，从该主机下载时将携带其作为 `Authorization` 请求头；指向任何其他主机的链接均在不带该凭证的情况下下载。应用不提供 OAuth 授权流程，也不支持其他认证方案。详见[下载模型](user-guide.md#2-download-a-model)。

<!-- en: does-the-app-check-a-downloaded-model -->
### 应用是否会校验下载的模型？

当 Hugging Face 在下载响应中提供了文件的 SHA-256 校验和时（针对其存放在 LFS 中的大文件通常会提供，模型文件通常属于此类），应用会进行校验：不匹配的文件将被删除而不会安装。指向任何其他主机的链接由于不包含哈希值，其文件将在未校验的情况下直接安装。“模型”页面会将每个模型标记为“已验证”或“未验证”。该校验只能证明文件与 Hugging Face 发布的文件一致，并不代表其发布者值得信任。详见[下载模型](user-guide.md#2-download-a-model)。

<!-- en: can-i-browse-and-install-models-from-hugging-face -->
### 我能否直接从 Hugging Face 浏览并安装模型？

可以，只要保存了访问 token，受权限管控（gated）的模型仓库也同样支持。详见[从 Hugging Face 发现模型](user-guide.md#discovering-models-from-hugging-face)。

<!-- en: what-is-the-foss-build-and-how-does-it-differ -->
### 什么是 FOSS 版本，它有什么不同？

这是 F-Droid 分发的分支版本：不含崩溃报告，不含分析统计，也没有相应的授权开关。其他一切功能，包括本地模型、云端服务商、MCP、触发器等，与常规版本完全一致。详见 [README § 安装](../README.md#install)。

---

<!-- en: what-leaves-the-device-and-what-does-not -->
## 哪些数据离开设备，哪些不会

<!-- en: does-a-normal-conversation-leave-my-phone -->
### 常规对话内容会离开我的手机吗？

不会。设备端推理意味着消息、回答以及二者之间的整个运行过程都完全保留在本地。没有用户账户，无需注册登录，维护者也没有搭建任何云端服务器。详见 [PRIVACY § 绝不离开设备的数据](../PRIVACY.md#4-what-never-leaves-your-device)。

<!-- en: so-what-can-go-out -->
### 那么哪些数据*可能*发送出去？

共有六条路径。其中五条完全由你主动开启：使用你自备密钥的**云端模型**（无论是在云端节点中、通过 `delegate_task` 工具调用，还是作为记忆“嵌入模型”）、你添加的 **MCP 服务器**、**模型下载**、针对你加入允许列表的主机发起的 **`http_request` 工具调用**，以及在你同意授权后的**崩溃报告**。

第六条路径自首次启动起默认开启，需要特别注意：内置的 **`search_tool`** 会在 Wikipedia 上查询主题，应用初始预置的流程会在遇到判定为事实性的问题时调用它。搜索词由模型生成，因此可能包含来自你对话的词句；由于该工具是只读的，它在调用前不会弹出确认。你可以在“工具”页面通过开关将其关闭，或者开启“禁止本地模型访问网络”，该开关会将其与所有云端路径一并切断。

除此之外，没有任何数据具有外发途径，除非你自己主动导出文件（如聊天记录、日志、运行追踪或备份文件），这些文件仅会发送至你指定的位置。详见 [PRIVACY § 可能离开设备的数据](../PRIVACY.md#3-what-can-leave-your-device)，以及 [SECURITY § 出站 HTTP 与信息渗漏链](../SECURITY.md#outbound-http-and-the-exfiltration-chain)中关于工具路径分层限制的说明。

<!-- en: where-are-my-api-keys-stored -->
### 我的 API 密钥保存在哪里？

保存在使用不可导出的 Android Keystore 密钥封装的加密存储中：绝不以明文保存在普通偏好设置中，绝不出现在导出的流程中，绝不输出到日志中，且仅在你勾选“包含 API 密钥与登录信息”时才会包含在备份中。MCP 凭据和 Hugging Face token 的保存方式完全相同。详见 [SECURITY § 云端服务商 API 密钥](../SECURITY.md#api-keys-for-cloud-providers)。

<!-- en: can-i-back-up-everything -->
### 我能否备份所有数据？

可以，全部打包为一个使用你自选密码锁定的单文件：前往“设置 → 隐私 → 备份与恢复 → 备份…”。它包含聊天、记忆、流程、运行历史、触发器、设置和文件，但不包含模型文件，且除非你勾选对应选项，否则不包含 API 密钥（写入服务器地址中的 token 会随地址一同保留）。该文件只保存在你指定的位置。通过“从文件恢复…”可以将数据导回本机或新手机，并替换该设备上的应用数据；已有的模型文件会保留，手机原有的密钥也会保留（备份中所包含的密钥除外）。详见[备份与恢复](user-guide.md#backup--restore)。

<!-- en: is-there-telemetry -->
### 应用是否存在遥测？

存在使用统计，但它们完全保存在设备本地，绝不向外传输：构建时的自动化检查若发现任何网络依赖接触到相关代码，就会直接中断构建。崩溃报告是独立的功能，默认处于关闭状态，仅存在于 `full` 构建版本中，并在收集任何内容前主动询问授权。记忆搜索底层的设备端库（MediaPipe）自带一个使用情况报告模块；发布版本已将其剔除，只有从源码编译的调试（debug）版本才会保留。详见 [SECURITY § 收集的内容](../SECURITY.md#what-is-collected-crash-reporting)。

<!-- en: is-my-chat-history-encrypted-on-the-phone -->
### 我的聊天记录在手机上是否加密？

是的：聊天记录、长期记忆和运行追踪均存储在经由 SQLCipher 加密的数据库中。详见 [SECURITY § 本地数据库](../SECURITY.md#local-database-at-rest-encryption)。

---

<!-- en: pipelines-and-the-editor -->
## 流程与编辑器

<!-- en: why-is-everything-a-pipeline-instead-of-just-a-chat-box -->
### 为什么所有功能都设计为流程，而不仅是一个聊天输入框？

因为这款应用的核心意义在于让你能够清晰查看并修改从发送消息到获取回答之间发生的完整过程：使用哪个模型、调用哪些工具、何时需要人工确认。首次启动即激活了开箱即用的就绪流程，因此在你产生定制需求之前，完全可以忽略背后的复杂逻辑。详见[流程](user-guide.md#pipeline-library)。

<!-- en: im-installed-now-what-do-i-actually-press -->
### 我已经安装完成，具体该按什么操作？

从头到尾只需五步：

1. **在新手引导中选择一个场景**：它会同时安装可运行的流程，并下载该场景所需的模型。
2. 模型下载完成后，进入“聊天 → 输入一条消息”。整个核心闭环就此完成；以下步骤均为可选项。（如果此前选择了“从头开始构建”，请先打开“模型”，下载一个预设并点按“激活”，详见[说明](user-guide.md#2-download-a-model)。）
3. 前往“流程 → 打开标记为 DEFAULT 的流程”，即可查看生成该回答的画布图谱（详见[说明](user-guide.md#visual-editor)）。
4. 前往“工具”开启某个工具，或添加 MCP 服务器（详见[说明](user-guide.md#adding-an-mcp-server)）。
5. 前往“更多 → 自动化 → 触发器”，让流程能够自主定时或按条件运行（详见[说明](user-guide.md#creating-or-editing-a-trigger)）。

<!-- en: settings-has-a-run-limits--structured-output-section--where-are-the-actual-pipelines -->
### “设置”中有个“运行限制与结构化输出”分区，实际的流程在哪里？

位于底部的“流程”标签页中，存放在流程库里。“设置”中的类别用于配置适用于运行的限制和安全防线，而不是流程本身；过去该项的命名曾引发过此类误解。详见[流程库](user-guide.md#pipeline-library)。

<!-- en: what-does-this-node-actually-do -->
### 这个节点具体是做什么的？

[流程手册](cookbook.md)中详细介绍了每种节点类型，包括它们如何处理输入、哪些配置字段会实质性改变运行、哪些字段仅供存储而被引擎忽略，并附带五个可导入的现成配方。

<!-- en: can-i-edit-a-pipeline-on-a-computer -->
### 我能否在电脑上编辑流程？

可以：应用提供了独立的网页端浏览器编辑器，读写完全相同的 JSON 文件，因此你可以在实体键盘上搭建流程并导出文件以供导入手机。详见[浏览器流程编辑器](user-guide.md#browser-pipeline-editor)。

<!-- en: can-i-share-a-pipeline-with-someone-else -->
### 我能否与他人分享流程？

你可以导出一个流程（或将整套组合打包导出为 bundle），其他人即可导入。请注意，**在 1.0 版本之前，该文件格式不构成向前兼容的契约保证**：导入时会按名称提示无法识别的内容，但这并不等同于不会丢失配置。请务必妥善保留原始文件。详见[分享流程文件](user-guide.md#sharing-pipeline-files-what-compatibility-you-can-count-on)。

---

<!-- en: triggers-and-background-work -->
## 触发器与后台运行

<!-- en: where-are-the-triggers -->
### 触发器在哪里设置？

进入“更多 → 自动化 → 触发器”，即第一分区的首行。详见[触发器](user-guide.md#triggers)和[“更多”标签页](user-guide.md#more-tab)。

<!-- en: does-the-agent-run-when-the-app-is-closed -->
### 应用关闭后，智能体还会继续运行吗？

是的：触发器、定时任务以及已在执行中的运行会在切出应用后继续推进，需要你批准的确认操作会以系统通知的形式送达。但这完全取决于下述电池优化设置，该设置在系统中默认并非无限制。详见[运行期间关闭应用会发生什么](user-guide.md#what-happens-when-you-close-the-app-during-a-run)。

<!-- en: why-does-nothing-happen-in-the-background-on-my-phone -->
### 为什么我的手机在后台什么也不执行？

几乎都是电池优化设置导致的，应用自身无法直接获取该白名单权限。[电池设置决定了后台任务能否执行](user-guide.md#battery-settings-decide-whether-any-of-this-happens)给出了权威原理说明；[离开应用瞬间后台运行立即终止](troubleshooting.md#a-background-run-dies-the-moment-i-leave-the-app)与[触发器没有触发](troubleshooting.md#a-trigger-didnt-fire)则对应两种具体的故障排查。

<!-- en: how-soon-after-the-condition-does-a-trigger-actually-fire -->
### 满足条件后触发器多久会真正触发？

并不是瞬时触发。除“充电中”以外的所有触发器都基于系统可延迟的后台调度机制运行，因此出现延迟属于正常现象，但如果延迟达到触发器自身执行周期的数倍，则属异常。详见[触发器多快能触发](user-guide.md#how-soon-a-trigger-fires)。

<!-- en: does-the-model-stay-in-memory-between-runs -->
### 运行间隙模型会一直驻留在内存中吗？

不会：空闲五分钟后模型会被自动卸载；当系统发出内存回收请求时也会卸载（正在进行的生成任务绝不会因此被强行中断）。因此，在没有任务运行时，它既不会霸占 RAM，也不会消耗电池电量。

<!-- en: how-do-i-get-the-background-journal-off-my-phone -->
### 如何导出手机中的后台执行日志？

在“触发器”顶部操作栏中有两项操作：将评估日志作为 JSON 文件**分享**，或**保存**到你选择的文件夹中。它涵盖所有触发器而不仅是你当前查看的那一个，因为排查问题时通常关注的是时间断层而非单条记录。在“设置 → 后台与触发器 → 外部自动化”下的请求日志中，也为外部传入请求提供了相同的两项操作。

在提交有关后台稳定性的问题反馈时，请附上上述任一日志文件：它能反映在你未观察应用时系统的具体行为。该文件仅记录日志行（例如失败运行的报错信息），绝不包含传递给运行的具体消息或生成的回答。这两项导出操作均不涉及任何网络传输。详见[查看触发器的运行历史](user-guide.md#checking-what-a-trigger-has-been-doing)。

---

<!-- en: tools-mcp-and-confirmations -->
## 工具、MCP 与确认机制

<!-- en: what-address-does-my-mcp-server-need -->
### 我的 MCP 服务器需要填写什么地址？

输入服务器启动时输出的端点地址，包含路径，表单自带的示例即为 `https://mcp.example.com/mcp`。界面中没有单独的端口输入框：非默认端口需直接写在 URL 中，且输入框下方的传输协议选择器必须与端点匹配：新建服务器默认选中的是 **Streamable HTTP**，若服务器仅支持较旧的协议则选择 **SSE**。详见[添加 MCP 服务器](user-guide.md#adding-an-mcp-server)。

<!-- en: which-authentication-methods-do-mcp-servers-support -->
### MCP 服务器支持哪些身份验证方式？

支持 Bearer token、HTTP Basic 或自定义 API 密钥请求头，无论你选择哪种方式，均按服务器维度独立加密存储。**不支持 OAuth。**详见[已知局限](#known-limitations)。

<!-- en: is-there-a-test-connection-button -->
### 是否有“测试连接”按钮？

有的。在服务器配置表单中点按“测试连接”，应用会使用你输入的参数发起连接、枚举服务器提供的工具列表并断开连接，此过程发生在保存配置之前。它会返回工具数量或具体的错误原因。详见[添加 MCP 服务器](user-guide.md#adding-an-mcp-server)。

<!-- en: my-server-publishes-16-tools-but-the-app-shows-13 -->
### 我的服务器发布了 16 个工具，但应用中只显示了 13 个？

这是由服务器决定的，而非应用本身限制，详见[已连接 MCP 服务器，但期望的某个工具未显示](troubleshooting.md#an-mcp-server-is-connected-but-a-tool-i-expect-isnt-there)，其中解释了原因以及对缺失工具的处理建议。

<!-- en: why-does-it-ask-me-before-every-tool-call -->
### 为什么每次工具调用前都会弹出询问？

因为该工具被归类为敏感或破坏性操作，这种安全闸门是在工具实际执行的位置由底层系统强制实施的，而非仅向模型发出提示建议。有两个设置项可以全局放宽或收紧此规则：“批准工具调用”可设置为对*每次*调用均进行询问，而“禁止破坏性工具”则会直接拒绝破坏性操作而不再提供批准选项，这两项设置均位于“设置 → 工具与工作区”。

若要停止针对**某个特定**工具的弹窗确认，可以在“工具”页面打开该工具，将其“风险级别”调整为“只读”。这适用于 MCP 工具以及在设备上发现的工具，后者默认被赋予“敏感”级别，正是因为应用无法预知其具体行为；而应用内置的工具则会明确标明其级别，因为系统底层代码可以直接解析该安全级别。详见[风险级别与人工介入](user-guide.md#risk-levels-and-human-in-the-loop)。

<!-- en: can-other-apps-on-my-phone-expose-tools-to-the-agent -->
### 手机上的其他应用能否向智能体公开工具？

Android 允许任何应用发布 AppFunctions，但调用其他应用的 AppFunctions 需要一项特殊权限（`EXECUTE_APP_FUNCTIONS`），Android 16 仅向具备特权的系统应用开放该权限，用户自行安装的常规应用并不具备。因此，你目前可以使用的是内置工具目录以及通过 MCP 连接的扩展工具。详见[内置工具](user-guide.md#built-in-tools)。

---

<!-- en: external-automation-tasker-macrodroid-adb -->
## 外部自动化（Tasker、MacroDroid、adb）

<!-- en: can-tasker-start-a-run -->
### Tasker 能否启动运行？

可以：这是官方支持的规范，并在文档中提供了针对 Tasker、MacroDroid 和 `adb` 的实操示例。详见 [external-automation.md](external-automation.md)。

<!-- en: does-it-work-out-of-the-box -->
### 该功能是否开箱即用？

不是，而且这是有意设计的。该入口**默认处于关闭状态**，即使打开开关，在明确绑定供外部应用调用的唯一流程之前，它仍然保持静默。指定其他任何流程名称的请求都将被直接拒绝，而不会被重定向。详见[开启功能](external-automation.md#switching-it-on)。

<!-- en: how-do-i-get-the-result-back -->
### 如何获取执行结果？

请求中可以指定一个回调广播，该广播会携带运行的状态，但绝不会携带其输出内容：你需要让流程将生成的结果输出保存到你能读取的位置。相关状态代码与拒绝原因均有完整枚举定义。详见[接收回调](external-automation.md#receiving-the-callback)。

<!-- en: does-calling-from-outside-skip-the-confirmations -->
### 从外部调用是否会跳过确认步骤？

不会。外部调用不是一种绕过安全机制的特权凭据：人工介入确认和破坏性工具拦截规则与应用自身的后台运行完全一致，确认通知会通过系统通知栏弹出。详见[安全模型概述](external-automation.md#the-safety-model-in-one-paragraph)。

---

<!-- en: run-limits-and-stopping-a-run -->
## 运行限制与停止运行

<!-- en: what-stops-a-runaway-pipeline -->
### 依靠什么机制防止流程失控死循环？

依靠四个数值指标和一个重复检测器：单次运行步数与单次运行 token 额度（针对非你手动启动的运行各设有一套独立的配额）；此外还单独配备了一个死循环监视机制，当检测到某个运行针对相同输入不断重复同一步骤时，会先发出提醒，若无改善则自动终止运行。详见[运行限制](user-guide.md#run-limits)与[当运行陷入死循环时](user-guide.md#when-a-run-goes-in-circles)。

<!-- en: a-run-hit-its-limit-is-the-work-lost -->
### 某次运行达到了限制，已完成的工作会丢失吗？

不会：它只是暂停并等待你的决定。达到限制后运行会处于暂停状态而非直接销毁，聊天界面会提供“继续（+N）”或“停止运行”两个按钮。选择继续将为其追加一份相同的配额，并从暂停位置继续推进，不会重复执行已完成的工作。暂停状态已写入本地数据库，因此即使应用关闭退出，当你返回时状态依然保留。详见[当运行达到限制时](user-guide.md#when-a-run-reaches-a-limit)。

<!-- en: does-continuing-past-a-limit-turn-the-limit-off -->
### 允许其突破限制继续运行，是否等于永久关闭该限制？

不是。这仅代表追加了**单次额外配额**，即按钮上标注的增量数值，当这段追加配额耗尽后，运行依然会再次暂停并询问。

<!-- en: can-i-cap-what-a-run-costs-me -->
### 我能否限制单次运行产生的花费金额？

不能。应用使用你自备的 API 密钥运行，无法得知你的云端账单，也不会凭空猜测并显示虚构的金额。Token 限制是目前最接近成本控制的手段。

<!-- en: how-do-i-stop-a-run-that-is-already-going -->
### 如何强行终止已经在执行中的运行？

点按聊天输入区域的**停止**按钮。这会从底层直接终止该运行，而不仅仅是关闭界面展示：后台工作立即停滞，对话中会新增一行提示，说明该运行在作出回答前已被手动停止。排队等待在其他运行之后的未决消息也会以同样方式取消。切换至其他聊天**不会**停止当前任务：后台运行依然会继续进行，这也是你可以稍后返回查看的原因。

此外还有两处可以终止运行：在达到限制的暂停卡片上点按“停止运行”；针对智能体为自身安排的链式计划任务，可前往“更多 → 任务 → 停止全部计划任务”，该操作也会一并取消正在执行中的任务。

<!-- en: my-run-stopped-by-itself-which-limit-was-it -->
### 我的运行自动停止了，究竟是触发了哪个限制？

系统消息中会明确指出停止的原因，而且并非所有中途停止都源自限制触发：中途停止共有三种不同的原因，对应着不同的排查方案。详见[智能体在中途停止运行](troubleshooting.md#the-agent-stopped-mid-run)。

---

<!-- en: memory -->
## 记忆

<!-- en: what-does-the-agent-remember-and-when -->
### 智能体会记住什么，何时会记住？

它会记住从对话中提取的长期事实（如偏好、事件、人际关系），并在相关节点被配置为读取记忆时按语义相似度检索出来。闲聊琐事以及它已经掌握的事实会被自动忽略；当某项事实发生变更时，条目会被更新，并在其历史版本中保留旧文本。详见[智能体能回忆起什么，以及何时回忆](user-guide.md#what-the-agent-recalls-and-when)。

<!-- en: can-i-add-or-delete-a-memory-myself -->
### 我能否自行手动添加或删除记忆？

可以，两者均支持手动操作，此外还提供“压缩”功能用于将重复冗余的条目合并，且在删除前会提供校验确认。详见[浏览长期记忆](user-guide.md#browsing-long-term-memory)。

<!-- en: does-memory-move-to-a-new-phone -->
### 记忆能否迁移至新手机？

通过“设置 → 隐私 → 备份与恢复”生成的完整备份恢复到新手机时，记忆会与其他数据一并迁移。单单记忆本身也支持独立的导出与导入，导出的文件由你自行保管。详见[备份与恢复](user-guide.md#backup--restore)与[将记忆迁移至其他设备（导出/导入）](user-guide.md#moving-memory-to-another-device-export--import)。

<!-- en: why-doesnt-a-memory-i-know-exists-come-back -->
### 为什么我确信存在的一条记忆没有被检索出来？

可能由多种不同原因引起，找准原因才能采取对应的解决方法。详见[记忆搜索未能找到明显存在的条目](troubleshooting.md#memory-search-isnt-finding-an-obvious-entry)。

---

<!-- en: known-limitations -->
## 已知局限

以下是应用目前尚不支持的功能。每项均说明了未来可能发生变更的前提条件。

- **全应用不提供 OAuth 支持。** 无论 MCP 服务器还是模型下载均不支持。目前仅提供 Bearer、Basic 和自定义 API 密钥请求头认证。若后续收到外部首例关于某真实服务器除 OAuth 外无法通过其他任何方式连接的反馈，将重新评估此项。
- **内置工具的风险级别不可更改。** MCP 工具以及在设备上发现的工具目前在其详情页上均配备了“风险级别”控制项；但写死在应用中的内置工具则没有，因为其安全级别是在读取你的任何偏好设置之前由底层代码直接解析确定的。如果实践中证明内置工具的分类在真实流程中存在实质性错误而非纯理论分歧，将重新评估此项。
- **单条消息仅支持附加一张图片。** 附加第二张图片会替换掉第一张，发生替换时应用会弹出提示。目前不支持拼图制作和多图横向对比。
- **部分 MCP 客户端高级特性暂时搁置：** `roots`、`sampling` 和 `elicitation` 尚未实现，这也是某些服务器在此应用中公开的工具数量可能少于其实际拥有数量的原因。这些功能中的每一项都属于拥有独立风险模型的新接口，而非简单的随手添加。当收到首例关于某个真实服务器因此缺失关键工具的外部反馈时会重新评估；其中 `elicitation` 会优先考虑，因为它是唯一能复用现有确认路径的特性。
- **无法直接调用其他应用的工具。** 发布 AppFunctions 向所有应用开放；但调用 AppFunctions 需要一项 Android 仅保留给特权系统应用的受限权限，目前的演进路线图对于常规安装形式尚无法改变这一现状。
- **记忆搜索在英文环境下表现最佳。** 设备端嵌入模型会将用俄语书写的无关联事实视作彼此高度相似的副本，导致记忆搜索在单次运行中仅保留其中一条，并将其余视作重复项丢弃（固定置顶的事实绝不会以此种方式被丢弃）。其他语言尚未经过详细基准测试。在“嵌入模型”中选用 OpenAI 按照其官方说明具备多语言能力（此处未做测试），但这会将你的记忆文本发送至 OpenAI。若要改进设备端模型，需要随应用内置分发多语言嵌入模型，目前该方案尚未敲定。
- **在 1.0 版本之前，流程文件不构成向前兼容契约。** 版本戳仅是一个标记，而非长期承诺，目前也不存在导入阶段的数据迁移兼容层。详见 [README § 预发布版本须知](../README.md#pre-release-notice)。
- **各项稳定性测试指标均基于单一设备和单一操作人员测得。** 曾有某个定时流程在未打开应用的情况下稳定运行了 7.14 天，共计执行了 55 次触发且无一例不明原因的遗漏。这是真实的实测数据，但并不等同于经过了广泛的外部多机型验证。
- **没有所谓的极简娱乐模式，未来也不会推出。** 本应用专门面向希望洞悉并修改底层运作机制的技术用户而构建。操作系统本身已经自带了能够回答日常问题的手机语音助手，本应用无意与其竞争。详见 [README § 适用人群](../README.md#who-its-for)。
- **仅有一名维护者，背后没有商业公司运作。** 相关问题的响应周期也取决于此。

---

<!-- en: see-also -->
## 参见

- [`user-guide.md`](user-guide.md)：各个界面与功能的详细运作机制。
- [`cookbook.md`](cookbook.md)：各节点类型功能详解及可导入的现成配方。
- [`troubleshooting.md`](troubleshooting.md)：出现故障时的排查解决步骤。
- [`external-automation.md`](external-automation.md)：供外部应用调用的交互契约。
- [`../PRIVACY.md`](../PRIVACY.md)：数据存储与外发规则说明。
- [`../SECURITY.md`](../SECURITY.md)：威胁模型与安全漏洞提报机制。
