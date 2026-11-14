package com.example

import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.ai.ParsedCodeBlock
import com.nexcode.app.ui.screens.chat.ChatMessage
import com.nexcode.app.ui.screens.chat.ChatScreenState
import com.nexcode.app.ui.screens.chat.ChatViewModel
import com.nexcode.app.ui.screens.chat.DiffStatusVariant
import com.nexcode.app.ui.screens.chat.MessageAuthor
import com.nexcode.app.ui.screens.projects.ProjectBuildStatus
import com.nexcode.app.ui.screens.projects.ProjectItem
import org.junit.Rule
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
    private lateinit var projectDir: java.io.File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        viewModel = ChatViewModel(context)

        // Projet réel sur disque : les écritures de diffs doivent réussir pour de vrai
        projectDir = java.nio.file.Files.createTempDirectory("nexcode_chat_test").toFile()
    }

    private fun bindRealProject() {
        viewModel.bindProject(
            ProjectItem(
                id = 1L,
                name = "Démo",
                packageName = "com.demo",
                initials = "D",
                language = "Kotlin",
                uiFramework = "Compose",
                modifiedText = "",
                buildStatus = ProjectBuildStatus.DRAFT,
                buildStatusLabel = "",
                localPath = projectDir.absolutePath
            )
        )
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
    fun testDiffAcceptanceAndRejection() = kotlinx.coroutines.runBlocking {
        bindRealProject()
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

        // Accepter tout : écriture disque réelle et déterministe (variante synchrone)
        viewModel.acceptAllDiffsSync()
        assertTrue(viewModel.uiState.value.diffItems.all { it.statusText == "Appliqué" })
        assertTrue(java.io.File(projectDir, "MainActivity.kt").readText().contains("fun main()"))

        // Refuser tout
        viewModel.rejectAllDiffs()
        assertTrue(viewModel.uiState.value.diffItems.all { it.statusText == "Refusé" })

        // Accepter un fichier individuel
        assertTrue(viewModel.acceptDiffItemSync("City.kt"))
        val cityDiff = viewModel.uiState.value.diffItems.find { it.fileName == "City.kt" }
        assertNotNull(cityDiff)
        assertEquals("Appliqué", cityDiff!!.statusText)
        assertEquals(DiffStatusVariant.SUCCESS, cityDiff.statusVariant)
    }
}
