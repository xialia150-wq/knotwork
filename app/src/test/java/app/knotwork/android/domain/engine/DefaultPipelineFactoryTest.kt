package app.knotwork.android.domain.engine

import app.knotwork.android.domain.models.NodeContextConfig
import app.knotwork.android.domain.models.NodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultPipelineFactoryTest {

    @Test
    fun `create returns pipeline with expected name and structure`() {
        val pipelineName = "Test Preset"
        val pipeline = DefaultPipelineFactory.create(pipelineName)

        assertEquals(pipelineName, pipeline.name)
        assertEquals(10, pipeline.nodes.size)
        assertEquals(12, pipeline.connections.size)
    }

    @Test
    fun `fallback display names are Chinese while router keys stay unchanged`() {
        val pipeline = DefaultPipelineFactory.create()
        assertEquals("默认系统流程", pipeline.name)
        assertEquals(
            listOf("输入", "分类", "本地模型", "搜索工具", "云端模型", "任务拆解", "队列", "执行子任务", "汇总", "输出"),
            pipeline.nodes.map { it.label },
        )
        assertEquals(setOf("Simple", "Data", "Complex", "Task"), pipeline.connections.mapNotNull { it.label }.toSet())
    }

    @Test
    fun `create leaves the OUTPUT node without a systemPrompt so it runs in pass-through mode`() {
        val pipeline = DefaultPipelineFactory.create()

        val outputNode = pipeline.nodes.single { it.type == NodeType.OUTPUT }
        assertNull(
            "The default OUTPUT node must forward upstream text verbatim, not run a formatting pass",
            outputNode.systemPrompt,
        )
    }

    @Test
    fun `create ships starter sample prompts and only claims tools the pipeline wires`() {
        val pipeline = DefaultPipelineFactory.create()

        assertEquals(
            "The default pipeline must ship three starter prompts for the empty state",
            3,
            pipeline.samplePrompts.size,
        )
        assertEquals(
            listOf("查找最新的端侧大模型基准测试", "解释端侧推理如何保护我的数据隐私", "将学习 Kotlin 协程拆解成逐步计划"),
            pipeline.samplePrompts.map { it.title },
        )
        // Only search_tool is actually wired (the Data branch), so it is the
        // sole tool a card may honestly advertise; the other cards make no
        // tool claim (null hint).
        val declaredTools = pipeline.samplePrompts.mapNotNull { it.toolsHint }
        assertEquals(listOf("search_tool"), declaredTools)
        assertTrue(
            "Every sample prompt must carry a non-blank title",
            pipeline.samplePrompts.all { it.title.isNotBlank() },
        )
    }

    @Test
    fun `create assigns recommended contextConfig defaults to every node`() {
        val pipeline = DefaultPipelineFactory.create()

        pipeline.nodes.forEach { node ->
            assertEquals(
                "Node ${node.label} (${node.type}) must use defaultForType()",
                NodeContextConfig.defaultForType(node.type),
                node.contextConfig,
            )
        }
    }
}
