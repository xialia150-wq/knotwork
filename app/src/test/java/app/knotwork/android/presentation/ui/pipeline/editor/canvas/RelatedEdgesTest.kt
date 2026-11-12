package app.knotwork.android.presentation.ui.pipeline.editor.canvas

import app.knotwork.android.domain.models.ConnectionModel
import org.junit.Assert.assertEquals
import org.junit.Test

class RelatedEdgesTest {
    @Test
    fun `selects incoming outgoing and self edges only`() {
        val edges = listOf(
            ConnectionModel(id = "in1", sourceNodeId = "a", targetNodeId = "b"),
            ConnectionModel(id = "in2", sourceNodeId = "c", targetNodeId = "b"),
            ConnectionModel(id = "out1", sourceNodeId = "b", targetNodeId = "d"),
            ConnectionModel(id = "out2", sourceNodeId = "b", targetNodeId = "e"),
            ConnectionModel(id = "self", sourceNodeId = "b", targetNodeId = "b"),
            ConnectionModel(id = "other", sourceNodeId = "d", targetNodeId = "e"),
        )
        assertEquals(setOf("in1", "in2", "out1", "out2", "self"), relatedEdges(edges, "b"))
        assertEquals(emptySet<String>(), relatedEdges(edges, null))
    }
}
