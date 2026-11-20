package app.knotwork.android.domain.services

import app.knotwork.android.domain.models.AgentTool
import app.knotwork.android.domain.models.ToolSource

/**
 * Renders the prompts the sentence-to-pipeline generator sends to the model.
 *
 * Pure text in, pure text out: no repository, no network, no settings. The prompt is
 * therefore unit-tested directly — what it names, what it withholds, and that it asks
 * for a plan before the JSON.
 *
 * **Why Chinese.** *Generate a pipeline from one sentence* is the Chinese-language
 * phrasing of the editor feature this prompt backs, and the node explanations are
 * Chinese by product decision. The JSON keys, the node type codes and the config field
 * names stay exactly as the app spells them, because those are the format — not prose
 * to translate.
 *
 * **What the prompt carries, and why.**
 *  - The user's demand, verbatim (bounded), plus an optional refinement sentence.
 *  - Every node type with a short Chinese description and the config fields that type
 *    actually reads, so the model can pick a type without guessing.
 *  - The *connected* MCP tools by name and description — the only tools a `TOOL` node
 *    may name, because anything else would be a tool the device does not have.
 *  - A compact, complete example, so "output valid JSON" is shown rather than only
 *    asserted.
 *  - The output rules: `schemaVersion` 1, `plan` first, no coordinates (the editor's
 *    `AutoLayout` owns those), one `INPUT`, one `OUTPUT`, acyclic.
 *
 * A tool name and description come from an MCP server — content the user did not write
 * — so they are trimmed to one bounded line each before they enter the prompt.
 */
object PipelineGenerationPromptBuilder {

    /**
     * Longest user demand (and refinement) the prompt carries. A demand is a sentence,
     * not a document; an unbounded one would crowd the node catalogue out of the
     * context window.
     */
    const val MAX_DEMAND_CHARS: Int = 4000

    /**
     * How many connected MCP tools the prompt lists. Past this many the catalogue adds
     * cost to every request without helping a one-sentence pipeline; the prompt says so
     * when tools were left out.
     */
    const val MAX_LISTED_TOOLS: Int = 40

    /** Longest single-line slice of an MCP tool description the prompt carries. */
    const val MAX_TOOL_DESCRIPTION_CHARS: Int = 240

    /**
     * Longest slice of the model's previous reply fed back verbatim on a validation
     * correction. Bounded because the reply is model-written text of unknown length.
     */
    const val MAX_PREVIOUS_OUTPUT_CHARS: Int = 8000

    /**
     * Renders the first prompt of a generation.
     *
     * @param demand What the user asked for, in their own words.
     * @param revision An additional change stacked on [demand] ("再改一句"), or `null`
     *   / blank when none was given.
     * @param mcpTools The connected MCP tools, in the user's server order.
     * @return The complete prompt.
     */
    fun buildInitial(demand: String, revision: String?, mcpTools: List<AgentTool>): String = buildString {
        append(commonPrompt(demand, revision, mcpTools))
        append("\n\n")
        append(OUTPUT_RULES)
        append("\n\n")
        append(EXAMPLE)
    }

    /**
     * Renders a corrective prompt: the same task and rules, plus the previous reply and
     * what the app's own checks found wrong with it.
     *
     * @param demand What the user asked for.
     * @param revision The additional change, if any.
     * @param mcpTools The connected MCP tools.
     * @param previousOutput The reply that failed. Kept verbatim (bounded) so the model
     *   corrects its own output instead of re-inventing the pipeline.
     * @param problems The validation findings, one sentence each.
     * @return The complete prompt.
     */
    fun buildCorrection(
        demand: String,
        revision: String?,
        mcpTools: List<AgentTool>,
        previousOutput: String,
        problems: List<String>,
    ): String = buildString {
        append(commonPrompt(demand, revision, mcpTools))
        append("\n\n")
        append(OUTPUT_RULES)
        append("\n\n")
        append(CORRECTION_HEADER)
        problems.forEach { problem ->
            append("\n- ")
            append(problem.toSingleLine())
        }
        append("\n\n")
        append(PREVIOUS_OUTPUT_HEADER)
        append('\n')
        append(previousOutput.take(MAX_PREVIOUS_OUTPUT_CHARS))
    }

    /** The task, the node catalogue and the connected-tool list — shared by both prompts. */
    private fun commonPrompt(demand: String, revision: String?, mcpTools: List<AgentTool>): String = buildString {
        append(PREAMBLE)
        append("\n\n用户需求：\n")
        append(demand.trim().take(MAX_DEMAND_CHARS))
        val refinement = revision?.trim().orEmpty()
        if (refinement.isNotEmpty()) {
            append("\n\n补充修改要求：\n")
            append(refinement.take(MAX_DEMAND_CHARS))
        }
        append(
            "\n\n先规划再输出：在 JSON 的 plan 字段里用 2-4 句中文说明你选哪些节点、为什么这样连线，" +
                "然后再给出 nodes 和 connections。\n\n",
        )
        append(NODE_CATALOG)
        append("\n\n")
        append(renderTools(mcpTools))
    }

    /** Renders the connected MCP tools, or states plainly that there are none. */
    private fun renderTools(mcpTools: List<AgentTool>): String {
        // Only MCP tools can be wired to a TOOL node: a built-in tool that the
        // user never connected must not appear in the catalogue the model reads.
        val connected = mcpTools.filter { it.source == ToolSource.MCP && it.name.isNotBlank() }
        if (connected.isEmpty()) return NO_TOOLS
        return buildString {
            append(TOOLS_HEADER)
            connected.take(MAX_LISTED_TOOLS).forEach { tool ->
                append("\n- ")
                append(tool.name.toSingleLine())
                append('：')
                append(tool.description.toSingleLine().take(MAX_TOOL_DESCRIPTION_CHARS))
            }
            if (connected.size > MAX_LISTED_TOOLS) {
                append("\n（还有更多工具未列出；没有列出的工具不能用于 TOOL 节点。）")
            }
        }
    }

    /** Collapses every run of whitespace to one space, so a value stays one line. */
    private fun String.toSingleLine(): String = WHITESPACE.replace(this, " ").trim()

    /** Matches the whitespace runs [toSingleLine] collapses. */
    private val WHITESPACE = Regex("\\s+")

    private const val PREAMBLE =
        "你是「织问」的流程设计助手：把用户的一句话需求，设计成一条可运行的节点流程。\n" +
            "你的输出必须能被「织问」的流程 JSON 格式直接读取。"

    private const val NODE_CATALOG = "可用节点（type：用途；可写的 config 字段）：\n" +
        "- INPUT：流程入口，接收用户输入；不需要 config。\n" +
        "- OUTPUT：流程出口，把最终结果交给用户；config.systemPrompt 可选，留空表示原样输出上游内容。\n" +
        "- LITE_RT：在本机小模型上做一步推理；config.systemPrompt 必填。\n" +
        "- CLOUD：在云端模型上做一步推理；config.systemPrompt 必填；" +
        "config.cloudProvider 可选，取值如 \"deepseek\"。\n" +
        "- TOOL：调用一个工具；config.toolName 必须取下面工具列表里的名字；config.systemPrompt 可选。\n" +
        "- SUMMARY：把上游结果汇总成一段；config.systemPrompt 必填。\n" +
        "- CLARIFICATION：向用户追问一个澄清问题；config.systemPrompt 可选；" +
        "config.quickReplies 可选（逗号分隔的候选回答）。\n" +
        "- IF_CONDITION：按关键词或复杂度判断走哪条分支；config.conditionKeywords 或 " +
        "config.conditionPrompt；出边的 label 必须是 \"True\" 或 \"False\"。\n" +
        "- INTENT_ROUTER：按意图分到一条出边；每条出边都要有 label。\n" +
        "- DECOMPOSITION：把复杂任务拆成若干子任务；config.maxSubtasks 可选（整数 1-8）。\n" +
        "- QUEUE_PROCESSOR：逐个处理子任务；出边 label 必须是 \"Item\" 或 \"Done\"。\n" +
        "- EVALUATION：判断上一步结果是否合格；出边 label 必须是 \"Pass\"、\"Retry\" 或 \"Fail\"。\n" +
        "- PIPELINE、SKILL：引用已有流程或技能；一句话生成里不要使用。"

    private const val TOOLS_HEADER = "已连接的 MCP 工具（TOOL 节点的 config.toolName 只能取这些名字）："

    private const val NO_TOOLS = "已连接的 MCP 工具：当前没有已连接且启用的 MCP 工具，因此不要使用 TOOL 节点。"

    private const val OUTPUT_RULES = "输出格式（严格遵守）：\n" +
        "只输出一个 JSON 对象（可以放在 json 代码块里），不要输出其他文字。字段如下：\n" +
        "{\n" +
        "  \"schemaVersion\": 1,\n" +
        "  \"plan\": \"2-4 句中文设计说明\",\n" +
        "  \"name\": \"简短的流程名\",\n" +
        "  \"nodes\": [ { \"id\": \"n1\", \"type\": \"INPUT\", \"label\": \"输入\" } ],\n" +
        "  \"connections\": [ { \"id\": \"c1\", \"fromNodeId\": \"n1\", \"toNodeId\": \"n2\" } ]\n" +
        "}\n" +
        "约束：\n" +
        "- schemaVersion 必须等于 1。\n" +
        "- 不要输出坐标（position、x、y）：画布位置由应用自动排版。\n" +
        "- nodes 和 connections 必须是数组；node 的 config 只写上面列出的字段。\n" +
        "- 节点 id 和连线 id 必须唯一，只能含字母、数字、下划线和连字符。\n" +
        "- 每条连线两端都必须是已声明的节点。\n" +
        "- 必须且只能有一个 INPUT 节点和一个 OUTPUT 节点；" +
        "所有节点都要从 INPUT 可达，并且都能到达 OUTPUT。\n" +
        "- 图不能有环；不要使用 PIPELINE 或 SKILL 节点。\n" +
        "- 节点 label 用中文，简短。"

    private const val EXAMPLE = "精简示例（需求：「把长文总结成三点」）：\n" +
        "{\n" +
        "  \"schemaVersion\": 1,\n" +
        "  \"plan\": \"用一个 CLOUD 节点做摘要，再交给 OUTPUT 输出。\",\n" +
        "  \"name\": \"长文三点总结\",\n" +
        "  \"nodes\": [\n" +
        "    { \"id\": \"in\", \"type\": \"INPUT\", \"label\": \"输入\" },\n" +
        "    { \"id\": \"sum\", \"type\": \"CLOUD\", \"label\": \"摘要\",\n" +
        "      \"config\": { \"systemPrompt\": \"把输入总结成三点，直接输出这三点。\" } },\n" +
        "    { \"id\": \"out\", \"type\": \"OUTPUT\", \"label\": \"输出\" }\n" +
        "  ],\n" +
        "  \"connections\": [\n" +
        "    { \"id\": \"e1\", \"fromNodeId\": \"in\", \"toNodeId\": \"sum\" },\n" +
        "    { \"id\": \"e2\", \"fromNodeId\": \"sum\", \"toNodeId\": \"out\" }\n" +
        "  ]\n" +
        "}"

    private const val CORRECTION_HEADER = "上一次的输出没有通过校验。请修正后重新输出完整的 JSON，不要只输出差异。\n" +
        "校验错误："

    private const val PREVIOUS_OUTPUT_HEADER = "上一次的输出（供参考，需要重写）："
}
