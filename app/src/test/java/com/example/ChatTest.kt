package com.example

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.ui.screens.chat.ChatScreenState
import com.nexcode.app.ui.screens.chat.ChatViewModel
import com.nexcode.app.ui.screens.chat.ConversationChipVariant
import com.nexcode.app.ui.screens.chat.DiffStatusVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatTest {

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        viewModel = ChatViewModel(context)
    }

    @Test
    fun testInitialChatState() {
        val state = viewModel.uiState.value
        assertEquals("claude-sonnet-4.5", state.modelName)
        assertEquals(3, state.contextFiles.size)
        assertEquals(ChatScreenState.DEFAULT, state.chatState)
        assertEquals(3, state.defaultMessages.size)
        assertEquals(4, state.conversations.size)
        assertEquals(18, state.totalConversationsCount)
        assertEquals(3, state.diffItems.size)
        assertEquals(27, state.totalAdditions)
        assertEquals(5, state.totalDeletions)
    }

    @Test
    fun testSendMessageAndAnalyzingStateTransition() {
        viewModel.updateInputMessage("Refactorise CityRepository pour utiliser kotlinx-serialization.")
        assertEquals("Refactorise CityRepository pour utiliser kotlinx-serialization.", viewModel.uiState.value.inputMessage)

        viewModel.sendMessage()
        val state = viewModel.uiState.value
        assertEquals(ChatScreenState.ANALYZING, state.chatState)
        assertEquals("", state.inputMessage)
        assertEquals("Refactorise CityRepository pour utiliser kotlinx-serialization.", state.analyzingUserPrompt)

        // Arrêt de l'analyse
        viewModel.stopAnalysis()
        assertEquals(ChatScreenState.DEFAULT, viewModel.uiState.value.chatState)
    }

    @Test
    fun testModelSelection() {
        viewModel.openModelSelector()
        assertTrue(viewModel.uiState.value.showModelSelector)

        viewModel.selectModel("gpt-4o")
        assertEquals("gpt-4o", viewModel.uiState.value.modelName)
        assertFalse(viewModel.uiState.value.showModelSelector)
    }

    @Test
    fun testHistorySearch() {
        viewModel.updateSearchQuery("TicketFlash")
        assertEquals("TicketFlash", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun testDiffAcceptanceAndRejection() {
        val initialState = viewModel.uiState.value
        val mainActivityDiff = initialState.diffItems.find { it.fileName == "MainActivity.kt" }
        assertNotNull(mainActivityDiff)
        assertEquals(18, mainActivityDiff!!.additionsCount)
        assertEquals(4, mainActivityDiff.deletionsCount)

        // Accepter tout
        viewModel.acceptAllDiffs()
        assertTrue(viewModel.uiState.value.diffItems.all { it.statusText == "Appliqué" })

        // Refuser tout
        viewModel.rejectAllDiffs()
        assertTrue(viewModel.uiState.value.diffItems.all { it.statusText == "Refusé" })

        // Accepter un fichier individuel
        viewModel.acceptDiffItem("City.kt")
        val cityDiff = viewModel.uiState.value.diffItems.find { it.fileName == "City.kt" }
        assertNotNull(cityDiff)
        assertEquals("Appliqué", cityDiff!!.statusText)
        assertEquals(DiffStatusVariant.SUCCESS, cityDiff.statusVariant)
    }
}
