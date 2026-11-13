package com.nexcode.app

import com.nexcode.app.ui.screens.build.BuildUiState
import com.nexcode.app.ui.screens.build.BuildVariant
import com.nexcode.app.ui.screens.build.BuildViewModel
import com.nexcode.app.ui.screens.build.CompilationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildScreensTest {

    private lateinit var viewModel: BuildViewModel

    @Before
    fun setUp() {
        viewModel = BuildViewModel(context = null)
    }

    @Test
    fun testInitialBuildConfigState() {
        val state = viewModel.uiState.value
        assertEquals("Nimbus Météo", state.projectName)
        assertEquals("1.2.0", state.versionName)
        assertEquals("13", state.buildNumber)
        assertEquals(BuildVariant.RELEASE, state.selectedVariant)
        assertEquals("nexcode-release.jks", state.keystoreName)
        assertTrue(state.isR8Enabled)
        assertTrue(state.isExportMappingEnabled)
        assertFalse(state.isDebuggableEnabled)
    }

    @Test
    fun testUpdateBuildConfig() {
        viewModel.setVariant(BuildVariant.DEBUG)
        viewModel.toggleR8()
        viewModel.toggleExportMapping()
        viewModel.toggleDebuggable()

        val state = viewModel.uiState.value
        assertEquals(BuildVariant.DEBUG, state.selectedVariant)
        assertFalse(state.isR8Enabled)
        assertFalse(state.isExportMappingEnabled)
        assertTrue(state.isDebuggableEnabled)
    }

    @Test
    fun testCompilationTransitions() {
        assertEquals(CompilationStatus.RUNNING, viewModel.uiState.value.compilationStatus)
        assertEquals(7, viewModel.uiState.value.steps.size)

        var startedCalled = false
        viewModel.startCompilation {
            startedCalled = true
        }
        assertTrue(startedCalled)
        assertEquals(CompilationStatus.RUNNING, viewModel.uiState.value.compilationStatus)
        assertEquals(10, viewModel.uiState.value.progressPercent)

        viewModel.cancelCompilation()
        assertEquals(CompilationStatus.CANCELLED, viewModel.uiState.value.compilationStatus)
    }

    @Test
    fun testLogsAndErrorsFilters() {
        assertFalse(viewModel.uiState.value.logVariantSuccess)
        assertEquals("Tout", viewModel.uiState.value.selectedLogFilter)
        assertEquals(8, viewModel.uiState.value.buildLogs.size)

        viewModel.setLogFilter("Erreurs")
        assertEquals("Erreurs", viewModel.uiState.value.selectedLogFilter)

        viewModel.toggleLogVariant()
        assertTrue(viewModel.uiState.value.logVariantSuccess)
    }

    @Test
    fun testApkActionsAndNotices() {
        val apks = viewModel.uiState.value.apkList
        assertEquals(3, apks.size)
        assertTrue(apks[0].isAccent)

        // Dismiss notice
        viewModel.dismissNotice()
        assertNull(viewModel.uiState.value.workflowNotice)
    }
}
