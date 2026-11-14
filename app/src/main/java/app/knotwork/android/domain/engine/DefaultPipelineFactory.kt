package app.knotwork.android.domain.engine

import app.knotwork.android.domain.models.CloudProvider
import app.knotwork.android.domain.models.ConnectionModel
import app.knotwork.android.domain.models.NodeContextConfig
import app.knotwork.android.domain.models.NodeModel
import app.knotwork.android.domain.models.NodeType
import app.knotwork.android.domain.models.PipelineGraph
import app.knotwork.android.domain.models.PipelineSamplePrompt
import java.util.UUID

/**
 * Factory for creating default pipelines.
 */
object DefaultPipelineFactory {

    /**
     * Creates the default complex execution pipeline.
     *
     * @param name The name of the pipeline.
     * @return A new instance of PipelineGraph containing the default nodes and connections.
     */
    // Reason: declarative graph construction — 10 nodes + 12 connections each
    // built as one builder-style expression. Splitting into helpers would
    // hurt readability more than the length cost.
    @Suppress("LongMethod")
    fun create(name: String = "默认系统流程"): PipelineGraph {
        val inputNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.INPUT,
            label = "输入",
            x = 100f,
            y = 300f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.INPUT),
        )
        val intentRouterNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.INTENT_ROUTER,
            label = "分类",
            x = 300f,
            y = 300f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.INTENT_ROUTER),
        )

        // Simple Branch
        val liteRtNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.LITE_RT,
            label = "本地模型",
            x = 500f,
            y = 100f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.LITE_RT),
        )

        // Data Branch
        val searchToolNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.TOOL,
            label = "搜索工具",
            toolName = "search_tool",
            x = 500f,
            y = 250f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.TOOL),
        )

        // Complex Branch
        val cloudNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.CLOUD,
            label = "云端模型",
            cloudProvider = CloudProvider.AUTO_KEY,
            x = 500f,
            y = 400f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.CLOUD),
        )

        // Task Branch
        val decompositionNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.DECOMPOSITION,
            label = "任务拆解",
            x = 500f,
            y = 550f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.DECOMPOSITION),
        )
        val queueProcessorNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.QUEUE_PROCESSOR,
            label = "队列",
            x = 700f,
            y = 550f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.QUEUE_PROCESSOR),
        )
        val taskToolNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.LITE_RT,
            label = "执行子任务",
            x = 900f,
            y = 550f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.LITE_RT),
        )
        val summaryNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.SUMMARY,
            label = "汇总",
            x = 1100f,
            y = 550f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.SUMMARY),
        )

        val outputNode = NodeModel(
            id = UUID.randomUUID().toString(),
            type = NodeType.OUTPUT,
            label = "输出",
            // No systemPrompt: the OUTPUT node runs in pass-through mode and forwards
            // the upstream text verbatim, instead of adding an extra formatting pass
            // that pollutes simple answers with LLM filler.
            systemPrompt = null,
            x = 1300f,
            y = 300f,
            contextConfig = NodeContextConfig.defaultForType(NodeType.OUTPUT),
        )

        val nodes = listOf(
            inputNode,
            intentRouterNode,
            liteRtNode,
            searchToolNode,
            cloudNode,
            decompositionNode,
            queueProcessorNode,
            taskToolNode,
            summaryNode,
            outputNode,
        )

        val connections = listOf(
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = inputNode.id,
                targetNodeId = intentRouterNode.id,
            ),

            // Routing edges
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = intentRouterNode.id,
                targetNodeId = liteRtNode.id,
                label = "Simple",
            ),
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = intentRouterNode.id,
                targetNodeId = searchToolNode.id,
                label = "Data",
            ),
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = intentRouterNode.id,
                targetNodeId = cloudNode.id,
                label = "Complex",
            ),
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = intentRouterNode.id,
                targetNodeId = decompositionNode.id,
                label = "Task",
            ),

            // Simple Path
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = liteRtNode.id,
                targetNodeId = outputNode.id,
            ),

            // Data Path
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = searchToolNode.id,
                targetNodeId = outputNode.id,
            ),

            // Complex Path
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = cloudNode.id,
                targetNodeId = outputNode.id,
            ),

            // Task Path
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = decompositionNode.id,
                targetNodeId = queueProcessorNode.id,
            ),
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = queueProcessorNode.id,
                targetNodeId = taskToolNode.id,
            ),
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = taskToolNode.id,
                targetNodeId = summaryNode.id,
            ),
            ConnectionModel(
                id = UUID.randomUUID().toString(),
                sourceNodeId = summaryNode.id,
                targetNodeId = outputNode.id,
            ),
        )

        return PipelineGraph(
            id = UUID.randomUUID().toString(),
            name = name,
            updatedAt = System.currentTimeMillis(),
            nodes = nodes,
            connections = connections,
            // Starter prompts chosen to match what THIS pipeline can actually
            // do: a web lookup (Data branch → search_tool), an on-device answer
            // (Simple branch → local LLM, privacy identity), and a multi-step
            // plan (Task branch → decomposition). Only the search card claims a
            // tool, because only search_tool is wired here.
            samplePrompts = listOf(
                PipelineSamplePrompt(
                    title = "查找最新的端侧大模型基准测试",
                    toolsHint = "search_tool",
                ),
                PipelineSamplePrompt(
                    title = "解释端侧推理如何保护我的数据隐私",
                ),
                PipelineSamplePrompt(
                    title = "将学习 Kotlin 协程拆解成逐步计划",
                ),
            ),
        )
    }
}
