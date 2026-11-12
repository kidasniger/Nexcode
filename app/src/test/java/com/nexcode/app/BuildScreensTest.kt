package com.nexcode.app

import com.nexcode.app.ui.screens.build.BuildUiState
import com.nexcode.app.ui.screens.build.BuildVariant
import com.nexcode.app.ui.screens.build.BuildViewModel
import com.nexcode.app.ui.screens.build.CompilationStatus
import com.nexcode.app.ui.screens.build.LogFilter
import com.nexcode.app.ui.screens.build.LogViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildScreensTest {

    private lateinit var viewModel: BuildViewModel

    @Before
    fun setUp() {
        viewModel = BuildViewModel()
    }

    @Test
    fun testInitialBuildConfigState() {
        val state = viewModel.uiState.value
        assertEquals("Nimbus Météo", state.projectName)
        assertEquals("1.2.0", state.versionName)
        assertEquals("13", state.buildNumber)
        assertEquals(BuildVariant.RELEASE, state.selectedVariant)
        assertEquals("nexcode-release.jks", state.keystoreName)
        assertTrue(state.r8Minification)
        assertTrue(state.exportMapping)
        assertFalse(state.debuggable)
    }

    @Test
    fun testUpdateBuildConfig() {
        viewModel.updateVersion("1.3.0")
        viewModel.updateBuildNumber("14")
        viewModel.selectVariant(BuildVariant.DEBUG)
        viewModel.toggleR8(false)
        viewModel.toggleExportMapping(false)
        viewModel.toggleDebuggable(true)

        val state = viewModel.uiState.value
        assertEquals("1.3.0", state.versionName)
        assertEquals("14", state.buildNumber)
        assertEquals(BuildVariant.DEBUG, state.selectedVariant)
        assertFalse(state.r8Minification)
        assertFalse(state.exportMapping)
        assertTrue(state.debuggable)
    }

    @Test
    fun testCompilationTransitions() {
        assertEquals(CompilationStatus.RUNNING, viewModel.uiState.value.compilationStatus)
        assertEquals(7, viewModel.uiState.value.runningSteps.size)

        viewModel.simulateSuccess()
        assertEquals(CompilationStatus.SUCCESS, viewModel.uiState.value.compilationStatus)
        assertEquals(100, viewModel.uiState.value.progressPercent)
        assertEquals(7, viewModel.uiState.value.successSteps.size)

        viewModel.toggleCompilationStatus()
        assertEquals(CompilationStatus.RUNNING, viewModel.uiState.value.compilationStatus)
    }

    @Test
    fun testLogsAndErrorsFilters() {
        assertEquals(LogViewMode.DEFAULT, viewModel.uiState.value.logViewMode)
        assertEquals(LogFilter.ALL, viewModel.uiState.value.activeLogFilter)
        assertEquals(8, viewModel.uiState.value.build12Logs.size)

        viewModel.selectLogFilter(LogFilter.ERRORS)
        assertEquals(LogFilter.ERRORS, viewModel.uiState.value.activeLogFilter)

        viewModel.toggleLogViewMode()
        assertEquals(LogViewMode.SUCCESS, viewModel.uiState.value.logViewMode)
        assertEquals(5, viewModel.uiState.value.build13Logs.size)
    }

    @Test
    fun testApkActions() {
        val apks = viewModel.uiState.value.apks
        assertEquals(3, apks.size)
        assertTrue(apks[0].isSignedAccent)

        viewModel.installApk(apks[0].id)
        assertNotNull(viewModel.uiState.value.userNotice)
        assertTrue(viewModel.uiState.value.userNotice!!.contains("Installation"))

        viewModel.shareApk(apks[0].id)
        assertTrue(viewModel.uiState.value.userNotice!!.contains("partage"))

        viewModel.downloadApk(apks[0].id)
        assertTrue(viewModel.uiState.value.userNotice!!.contains("Téléchargement"))

        viewModel.dismissNotice()
        assertEquals(null, viewModel.uiState.value.userNotice)
    }
}
