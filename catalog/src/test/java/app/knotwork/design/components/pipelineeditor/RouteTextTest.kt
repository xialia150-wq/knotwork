package app.knotwork.design.components.pipelineeditor

import app.knotwork.design.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Ensures display localization leaves routing keys and custom labels intact. */
class RouteTextTest {
    @Test
    fun `built in routes map to display strings and unknown names remain unchanged`() {
        val labels = mapOf(
            "True" to R.string.canvas_route_true,
            "False" to R.string.canvas_route_false,
            "Item" to R.string.canvas_route_item,
            "Done" to R.string.canvas_route_done,
            "Pass" to R.string.canvas_route_pass,
            "Retry" to R.string.canvas_route_retry,
            "Fail" to R.string.canvas_route_fail,
        )
        labels.forEach { (label, resource) -> assertEquals(resource, routeTextResource(label)) }
        assertNull(routeTextResource("unknown"))
        assertNull(OutboundPort.Custom("True").displayResource())
        assertEquals("True", OutboundPort.Custom("True").label)
    }
}
