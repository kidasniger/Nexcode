package com.example

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.ai.ParsedCodeBlock
import com.nexcode.app.ui.screens.chat.ChatMessage
import com.nexcode.app.ui.screens.chat.ChatScreenState
import com.nexcode.app.ui.screens.chat.ChatViewModel
import com.nexcode.app.ui.screens.chat.DiffStatusVariant
import com.nexcode.app.ui.screens.chat.MessageAuthor
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
        assertTrue(state.contextFiles.isEmpty())
        assertEquals(ChatScreenState.DEFAULT, state.chatState)
        assertTrue(state.messages.isEmpty())
        assertTrue(state.conversations.isEmpty())
        assertEquals(0, state.totalConversationsCount)
        assertTrue(state.diffItems.isEmpty())
        assertEquals(0, state.totalAdditions)
        assertEquals(0, state.totalDeletions)
    }

    @Test
    fun testContextToggleAndAnalyzingTransition() {
        viewModel.toggleContextFile("MainActivity.kt")
        assertEquals(listOf("MainActivity.kt"), viewModel.uiState.value.contextFiles)

        viewModel.updateInputMessage("Refactorise CityRepository pour utiliser kotlinx-serialization.")
        assertEquals("Refactorise CityRepository pour utiliser kotlinx-serialization.", viewModel.uiState.value.inputMessage)

        viewModel.setChatScreenState(ChatScreenState.ANALYZING)
        assertEquals(ChatScreenState.ANALYZING, viewModel.uiState.value.chatState)

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
        val proposalMessage = ChatMessage(
            id = "ai_1",
            author = MessageAuthor.AI,
            authorLabel = "CLAUDE",
            timeString = "10:15",
            text = "Propositions de modifications",
            codeBlocks = listOf(
                ParsedCodeBlock(
                    language = "kotlin",
                    filePath = "MainActivity.kt",
                    headerLabel = "MainActivity.kt · kotlin",
                    content = "package com.example\nfun main() = println(\"updated\")"
                ),
                ParsedCodeBlock(
                    language = "kotlin",
                    filePath = "City.kt",
                    headerLabel = "City.kt · kotlin",
                    content = "data class City(val name: String)"
                )
            )
        )

        viewModel.prepareReviewFromMessage(proposalMessage)

        val initialState = viewModel.uiState.value
        val mainActivityDiff = initialState.diffItems.find { it.fileName == "MainActivity.kt" }
        assertNotNull(mainActivityDiff)
        assertEquals(2, mainActivityDiff!!.additionsCount)
        assertEquals(0, mainActivityDiff.deletionsCount)

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
