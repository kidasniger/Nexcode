package com.nexcode.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexcode.app.data.local.NexcodeDatabase
import com.nexcode.app.data.local.entity.BuildEntity
import com.nexcode.app.data.local.entity.ConversationEntity
import com.nexcode.app.data.local.entity.FileEntity
import com.nexcode.app.data.local.entity.ProjectEntity
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import com.nexcode.app.ui.components.NexcodeNavTab
import com.nexcode.app.ui.navigation.NexcodeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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
class NexcodeDataTest {

    private lateinit var db: NexcodeDatabase
    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NexcodeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = NexcodePreferences(context)
        prefs.setAiModel(NexcodePreferences.DEFAULT_AI_MODEL)
        prefs.setThemeMode(NexcodePreferences.DEFAULT_THEME_MODE)
        prefs.setEditorFontSize(NexcodePreferences.DEFAULT_FONT_SIZE)
        prefs.setEditorWordWrap(NexcodePreferences.DEFAULT_WORD_WRAP)
        prefs.setEditorShowLineNumbers(NexcodePreferences.DEFAULT_LINE_NUMBERS)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRoomDatabaseEntities() = runBlocking {
        // Test ProjectEntity
        val project = ProjectEntity(
            name = "TestApp",
            packageName = "com.test.app",
            path = "/storage/projects/TestApp"
        )
        val projectId = db.projectDao().insertProject(project)
        assertTrue(projectId > 0)

        // Test FileEntity
        val file = FileEntity(
            projectId = projectId,
            relativePath = "src/MainActivity.kt",
            name = "MainActivity.kt",
            isDirectory = false,
            extension = "kt",
            size = 1024
        )
        val fileId = db.fileDao().insertFile(file)
        assertTrue(fileId > 0)

        // Test ConversationEntity
        val conversation = ConversationEntity(
            projectId = projectId,
            role = "user",
            content = "Cree un composant bouton"
        )
        val convId = db.conversationDao().insertMessage(conversation)
        assertTrue(convId > 0)

        // Test BuildEntity
        val build = BuildEntity(
            projectId = projectId,
            buildType = "debug",
            status = "SUCCESS",
            durationMs = 4500
        )
        val buildId = db.buildDao().insertBuild(build)
        assertTrue(buildId > 0)

        // Verification query
        val fetchedProject = db.projectDao().getProjectById(projectId)
        assertNotNull(fetchedProject)
        assertEquals("TestApp", fetchedProject?.name)
    }

    @Test
    fun testPreferencesDefaults() = runBlocking {
        val prefs = NexcodePreferences(context)
        assertEquals(NexcodePreferences.DEFAULT_THEME_MODE, prefs.themeModeFlow.first())
        assertEquals(NexcodePreferences.DEFAULT_FONT_SIZE, prefs.editorFontSizeFlow.first())
        assertTrue(prefs.editorWordWrapFlow.first())
        assertTrue(prefs.editorShowLineNumbersFlow.first())
        assertEquals(NexcodePreferences.DEFAULT_AI_MODEL, prefs.aiModelFlow.first())
    }

    @Test
    fun testSecureVault() {
        val vault = NexcodeSecureVault(context)
        assertFalse(vault.hasApiKey())

        vault.saveApiKey("secret_gemini_api_key_test")
        assertTrue(vault.hasApiKey())
        assertEquals("secret_gemini_api_key_test", vault.getApiKey())

        vault.clearApiKey()
        assertFalse(vault.hasApiKey())
    }

    @Test
    fun testNavigationScreensCountAndRules() {
        assertEquals(30, NexcodeScreen.ALL_SCREENS.size)

        // Ecrans hors onglets 1 a 4
        listOf(NexcodeScreen.S01, NexcodeScreen.S02, NexcodeScreen.S03, NexcodeScreen.S04).forEach {
            assertFalse(it.hasBottomBar)
            assertEquals(null, it.tab)
        }

        // Projets 5 a 9
        listOf(NexcodeScreen.S05, NexcodeScreen.S06, NexcodeScreen.S07, NexcodeScreen.S08, NexcodeScreen.S09).forEach {
            assertTrue(it.hasBottomBar)
            assertEquals(NexcodeNavTab.PROJECTS, it.tab)
        }

        // Editeur 10 a 14 et 18 a 20
        listOf(
            NexcodeScreen.S10, NexcodeScreen.S11, NexcodeScreen.S12, NexcodeScreen.S13, NexcodeScreen.S14,
            NexcodeScreen.S18, NexcodeScreen.S19, NexcodeScreen.S20
        ).forEach {
            assertTrue(it.hasBottomBar)
            assertEquals(NexcodeNavTab.EDITOR, it.tab)
        }

        // Chat IA 15 a 17
        listOf(NexcodeScreen.S15, NexcodeScreen.S16, NexcodeScreen.S17).forEach {
            assertTrue(it.hasBottomBar)
            assertEquals(NexcodeNavTab.CHAT, it.tab)
        }

        // Build 21 a 30
        listOf(
            NexcodeScreen.S21, NexcodeScreen.S22, NexcodeScreen.S23, NexcodeScreen.S24, NexcodeScreen.S25,
            NexcodeScreen.S26, NexcodeScreen.S27, NexcodeScreen.S28, NexcodeScreen.S29, NexcodeScreen.S30
        ).forEach {
            assertTrue(it.hasBottomBar)
            assertEquals(NexcodeNavTab.BUILD, it.tab)
        }
    }
}
