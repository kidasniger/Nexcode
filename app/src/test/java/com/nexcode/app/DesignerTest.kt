package com.nexcode.app

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.ui.screens.designer.DesignerViewModel
import com.nexcode.app.ui.screens.designer.PaletteToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DesignerTest {

    private lateinit var viewModel: DesignerViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        viewModel = DesignerViewModel(context)
    }

    @Test
    fun testInitialDesignerState() {
        val state = viewModel.uiState.value
        assertEquals("carte_3", state.selectedComponentId)
        assertTrue(state.isCardSelected)
        assertEquals(3, state.citiesList.size)
        assertEquals("Lyon", state.citiesList[0].name)
        assertTrue(state.citiesList[0].isSelected)
        assertEquals("Paris", state.citiesList[1].name)
        assertTrue(state.citiesList[1].isFaded)
        assertEquals("Brest", state.citiesList[2].name)
        assertTrue(state.citiesList[2].isFaded)

        assertEquals("16 dp", state.posX)
        assertEquals("220 dp", state.posY)
        assertEquals("Remplir", state.widthMode)
        assertEquals("96 dp", state.heightVal)
        assertEquals(12, state.cornerRadius)
        assertEquals(1, state.alignmentIndex)
        assertTrue(state.isDarkPreview)
        assertEquals("Pixel 8", state.deviceName)
    }

    @Test
    fun testPaletteToolSelection() {
        assertNull(viewModel.uiState.value.selectedTool)

        viewModel.selectTool(PaletteToolType.BUTTON)
        assertEquals(PaletteToolType.BUTTON, viewModel.uiState.value.selectedTool)

        // Désélectionner en recliquant
        viewModel.selectTool(PaletteToolType.BUTTON)
        assertNull(viewModel.uiState.value.selectedTool)
    }

    @Test
    fun testPropertyUpdates() {
        viewModel.updatePosX("24 dp")
        assertEquals("24 dp", viewModel.uiState.value.posX)

        viewModel.updatePosY("250 dp")
        assertEquals("250 dp", viewModel.uiState.value.posY)

        viewModel.updateHeight("110 dp")
        assertEquals("110 dp", viewModel.uiState.value.heightVal)

        viewModel.setCornerRadiusProgress(0.6f)
        assertEquals(15, viewModel.uiState.value.cornerRadius)

        viewModel.setAlignment(0)
        assertEquals(0, viewModel.uiState.value.alignmentIndex)

        viewModel.toggleComponentEnabled()
        assertFalse(viewModel.uiState.value.isComponentEnabled)
    }

    @Test
    fun testDuplicateAndDelete() {
        viewModel.duplicateComponent()
        val duplicatedState = viewModel.uiState.value
        assertEquals("Carte #4", duplicatedState.componentName)
        assertEquals("324 dp", duplicatedState.posY)

        viewModel.deleteComponent()
        assertFalse(viewModel.uiState.value.isCardSelected)
        assertEquals("", viewModel.uiState.value.selectedComponentId)
    }

    @Test
    fun testPreviewControls() {
        viewModel.togglePreviewTheme(false)
        assertFalse(viewModel.uiState.value.isDarkPreview)

        viewModel.toggleOrientation()
        assertTrue(viewModel.uiState.value.isLandscape)

        viewModel.cycleDevice()
        assertEquals("Pixel Tablet", viewModel.uiState.value.deviceName)
    }
}
