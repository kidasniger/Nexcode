package com.nexcode.app

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.ui.screens.designer.DesignerViewModel
import com.nexcode.app.ui.screens.designer.PaletteToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests du concepteur visuel : le plan de travail est un modèle local réel,
 * éditable par l'utilisateur — aucune ville météo ni bannière de compilation inventée.
 */
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
    fun testInitialDesignerStateIsHonestEmptyWorkspace() {
        val state = viewModel.uiState.value
        assertTrue(state.components.isEmpty())
        assertNull(state.selectedComponentId)
        assertNull(state.selectedTool)
        assertTrue(state.isDarkPreview)
        assertFalse(state.isLandscape)
        assertEquals("Pixel 8", state.deviceName)
        assertEquals(246, state.phoneWidthDp)
        assertEquals(520, state.phoneHeightDp)
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
    fun testAddComponentCreatesRealEditableComponent() {
        viewModel.selectTool(PaletteToolType.CARD)
        viewModel.addComponentFromTool()

        val state = viewModel.uiState.value
        assertEquals(1, state.components.size)
        val component = state.components.first()
        assertEquals(PaletteToolType.CARD, component.type)
        assertEquals("Carte 1", component.name)
        assertTrue(component.text.isNotBlank()) // composant à texte : contenu initial éditable
        assertEquals(component.id, state.selectedComponentId)
        assertNotNull(state.selectedComponent)
    }

    @Test
    fun testAddComponentRequiresSelectedTool() {
        viewModel.addComponentFromTool()
        assertTrue(viewModel.uiState.value.components.isEmpty())
    }

    @Test
    fun testUpdatePropertiesModifiesSelectedComponent() {
        viewModel.selectTool(PaletteToolType.TEXT)
        viewModel.addComponentFromTool()

        viewModel.updateComponentText("Bonjour réel")
        viewModel.updatePosX("32")
        viewModel.updatePosY("64")
        viewModel.updateHeight("120")
        viewModel.setCornerRadiusProgress(0.6f)
        viewModel.updateBgColor("#1C2438")
        viewModel.toggleComponentEnabled()

        val component = viewModel.uiState.value.selectedComponent!!
        assertEquals("Bonjour réel", component.text)
        assertEquals(32, component.posX)
        assertEquals(64, component.posY)
        assertEquals(120, component.heightDp)
        assertEquals(15, component.cornerRadius)
        assertEquals("#1C2438", component.bgColorHex)
        assertFalse(component.isEnabled)
    }

    @Test
    fun testCycleWidthModeOnSelectedComponent() {
        viewModel.selectTool(PaletteToolType.CARD)
        viewModel.addComponentFromTool()

        val initial = viewModel.uiState.value.selectedComponent!!.widthMode
        viewModel.cycleWidthMode()
        val afterOneCycle = viewModel.uiState.value.selectedComponent!!.widthMode
        assertNotEquals(initial, afterOneCycle)

        viewModel.cycleWidthMode()
        viewModel.cycleWidthMode()
        viewModel.cycleWidthMode()
        assertEquals(initial, viewModel.uiState.value.selectedComponent!!.widthMode)
    }

    @Test
    fun testDuplicateCopiesAllPropertiesAndSelectsCopy() {
        viewModel.selectTool(PaletteToolType.TEXT)
        viewModel.addComponentFromTool()
        viewModel.updateComponentText("Contenu réel")
        viewModel.updatePosX("40")
        viewModel.updateBgColor("#ABCDEF")

        val original = viewModel.uiState.value.selectedComponent!!
        viewModel.duplicateComponent()

        val state = viewModel.uiState.value
        assertEquals(2, state.components.size)
        val copy = state.selectedComponent!!
        assertNotEquals(original.id, copy.id)
        assertEquals("${original.name} (copie)", copy.name)
        assertEquals(original.text, copy.text)
        assertEquals(original.posX, copy.posX)
        assertEquals(original.bgColorHex, copy.bgColorHex)
        assertEquals(original.cornerRadius, copy.cornerRadius)
        assertEquals(original.type, copy.type)

        // La copie est insérée juste après l'original
        assertEquals(1, state.components.indexOfFirst { it.id == copy.id })
    }

    @Test
    fun testDeleteRemovesComponentAndSelectsNext() {
        viewModel.selectTool(PaletteToolType.CARD)
        viewModel.addComponentFromTool()
        viewModel.addComponentFromTool()

        val firstId = viewModel.uiState.value.components[0].id
        val secondId = viewModel.uiState.value.components[1].id

        // Sélection du premier composant puis suppression réelle
        viewModel.selectComponent(firstId)
        viewModel.deleteComponent()

        val afterDelete = viewModel.uiState.value
        assertEquals(1, afterDelete.components.size)
        assertEquals(secondId, afterDelete.selectedComponentId)
        assertTrue(afterDelete.components.none { it.id == firstId })

        // Suppression du dernier composant : plus aucune sélection
        viewModel.deleteComponent()
        assertTrue(viewModel.uiState.value.components.isEmpty())
        assertNull(viewModel.uiState.value.selectedComponentId)
    }

    @Test
    fun testDuplicateOrDeleteWithoutSelectionIsNoOp() {
        viewModel.duplicateComponent()
        viewModel.deleteComponent()
        assertTrue(viewModel.uiState.value.components.isEmpty())
    }

    @Test
    fun testPreviewControlsChangeRealFrameDimensions() {
        viewModel.togglePreviewTheme(false)
        assertFalse(viewModel.uiState.value.isDarkPreview)

        viewModel.toggleOrientation()
        assertTrue(viewModel.uiState.value.isLandscape)
        // Paysage : dimensions réellement permutées (520 x 246 pour Pixel 8)
        assertEquals(520, viewModel.uiState.value.phoneWidthDp)
        assertEquals(246, viewModel.uiState.value.phoneHeightDp)

        viewModel.cycleDevice()
        assertEquals("Pixel Tablet", viewModel.uiState.value.deviceName)
        // Nouvel appareil en paysage : 480 x 340 (dimensions réelles du modèle)
        assertEquals(480, viewModel.uiState.value.phoneWidthDp)
        assertEquals(340, viewModel.uiState.value.phoneHeightDp)

        viewModel.toggleOrientation()
        assertFalse(viewModel.uiState.value.isLandscape)
        assertEquals(340, viewModel.uiState.value.phoneWidthDp)
        assertEquals(480, viewModel.uiState.value.phoneHeightDp)
    }
}
