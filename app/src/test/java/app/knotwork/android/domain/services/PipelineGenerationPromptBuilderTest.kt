package app.knotwork.android.domain.services

import app.knotwork.android.domain.models.AgentTool
import app.knotwork.android.domain.models.ToolSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generation prompt is the whole product decision of this feature, so it is
 * asserted directly: what it asks the model to do, what it names, and what it must
 * never name.
 */
class PipelineGenerationPromptBuilderTest {

    private val mcpTools = listOf(
        AgentTool(
            name = "search_web",
            description = "Search\nthe web for a page",
            parameters = "{}",
            source = ToolSource.MCP,
        ),
        AgentTool(
            name = "builtin_only_tool",
            description = "A local tool nobody connected",
            parameters = "{}",
            source = ToolSource.BUILT_IN,
        ),
    )

    @Test
    fun `initial prompt carries the demand, the node catalogue and the plan request`() {
        val prompt = PipelineGenerationPromptBuilder.buildInitial("把长文总结成三点", null, mcpTools)

        assertTrue(prompt.contains("把长文总结成三点"))
        assertTrue(prompt.contains("INPUT"))
        assertTrue(prompt.contains("云端模型"))
        assertTrue(prompt.contains("先规划再输出"))
        assertTrue(prompt.contains("\"schemaVersion\": 1"))
        assertTrue(prompt.contains("不要输出坐标"))
        assertTrue(prompt.contains("schemaVersion 必须等于 1"))
        assertTrue(prompt.contains("精简示例"))
    }

    @Test
    fun `initial prompt lists only the connected MCP tools`() {
        val prompt = PipelineGenerationPromptBuilder.buildInitial("做点事", null, mcpTools)

        assertTrue(prompt.contains("search_web"))
        assertTrue(prompt.contains("Search the web for a page"))
        assertFalse(prompt.contains("builtin_only_tool"))
    }

    @Test
    fun `prompt states plainly that there are no MCP tools`() {
        val prompt = PipelineGenerationPromptBuilder.buildInitial("做点事", null, emptyList())

        assertTrue(prompt.contains("当前没有已连接"))
        assertTrue(prompt.contains("不要使用 TOOL 节点"))
    }

    @Test
    fun `a refinement sentence is carried as an additional requirement`() {
        val prompt = PipelineGenerationPromptBuilder.buildInitial("总结长文", "输出三点", emptyList())

        assertTrue(prompt.contains("补充修改要求"))
        assertTrue(prompt.contains("输出三点"))
    }

    @Test
    fun `a tool description is flattened to one bounded line`() {
        val longDescription = "word\n".repeat(300)
        val tools = listOf(
            AgentTool(name = "verbose_tool", description = longDescription, parameters = "{}", source = ToolSource.MCP),
        )

        val prompt = PipelineGenerationPromptBuilder.buildInitial("做点事", null, tools)
        val listed = prompt.lineSequence().first { it.startsWith("- verbose_tool：") }

        assertFalse(listed.contains('\n'))
        assertTrue(
            listed.length <= "- verbose_tool：".length + PipelineGenerationPromptBuilder.MAX_TOOL_DESCRIPTION_CHARS,
        )
    }

    @Test
    fun `correction prompt carries the findings and the previous reply`() {
        val prompt = PipelineGenerationPromptBuilder.buildCorrection(
            demand = "总结长文",
            revision = "输出三点",
            mcpTools = mcpTools,
            previousOutput = "{\"schemaVersion\":1,\"nodes\":[]}",
            problems = listOf("validation error: MissingOutput"),
        )

        assertTrue(prompt.contains("总结长文"))
        assertTrue(prompt.contains("输出三点"))
        assertTrue(prompt.contains("MissingOutput"))
        assertTrue(prompt.contains("{\"schemaVersion\":1,\"nodes\":[]}"))
        assertTrue(prompt.contains("校验错误"))
    }

    @Test
    fun `correction prompt bounds the previous reply it echoes`() {
        val huge = "x".repeat(PipelineGenerationPromptBuilder.MAX_PREVIOUS_OUTPUT_CHARS + 500)

        val prompt = PipelineGenerationPromptBuilder.buildCorrection(
            demand = "总结长文",
            revision = null,
            mcpTools = emptyList(),
            previousOutput = huge,
            problems = emptyList(),
        )

        val ceiling = "x".repeat(PipelineGenerationPromptBuilder.MAX_PREVIOUS_OUTPUT_CHARS)
        assertTrue(prompt.contains(ceiling))
        assertFalse(prompt.contains(ceiling + "x"))
    }
}
