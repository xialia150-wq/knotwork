package app.knotwork.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke check that the instrumentation is wired to the app under test at all —
 * if this fails, every other instrumented test is failing for the same reason and
 * their messages will not say so.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // BuildConfig.APPLICATION_ID is the final ID for this build variant,
        // including the debug suffix, so it must equal the target context package.
        assertTrue(
            "Unexpected target package: ${appContext.packageName}",
            appContext.packageName == BuildConfig.APPLICATION_ID,
        )
    }
}
