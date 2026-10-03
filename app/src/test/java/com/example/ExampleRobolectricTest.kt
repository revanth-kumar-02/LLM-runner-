package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FilePickerManager
import com.example.data.model.ChatMessage
import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import com.example.data.model.ModelStatus
import com.example.data.repository.ConversationRepository
import com.example.data.repository.ModelRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.manager.ModelManager
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Local AI", appName)
  }

  @Test
  fun `test FilePickerManager open document intent`() {
    val intent = FilePickerManager.createOpenDocumentIntent()
    assertNotNull(intent)
    assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, intent.action)
  }

  @Test
  fun `test dynamic ModelRepository and ModelManager single heavy model rule`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = ModelRepository(context)
    val manager = ModelManager(context, repository)

    // Clean initial state
    val initialCount = repository.models.value.size

    val modelA = ModelMetadata(
      name = "Gemma 2 2B",
      fileName = "gemma-2-2b.gguf",
      filePath = "path/gemma-2-2b.gguf",
      architecture = "gemma-2",
      parameterCount = "2B",
      quantization = "Q4_K_M",
      fileSize = 1_600_000_000L,
      formattedSize = "1.60 GB",
      capabilities = setOf(ModelCapability.TEXT)
    )

    val modelB = ModelMetadata(
      name = "Llama 3.2 3B",
      fileName = "llama-3.2-3b.gguf",
      filePath = "path/llama-3.2-3b.gguf",
      architecture = "llama-3.2",
      parameterCount = "3B",
      quantization = "Q4_K_M",
      fileSize = 2_000_000_000L,
      formattedSize = "2.00 GB",
      capabilities = setOf(ModelCapability.TEXT)
    )

    repository.addModel(modelA)
    repository.addModel(modelB)

    // 1. Load Model A
    val loadA = manager.loadModel(modelA.id).toList()
    assertTrue(loadA.last().isComplete)
    assertEquals(modelA.id, manager.activeModel.value?.id)
    assertEquals(ModelStatus.LOADED, repository.getModelById(modelA.id)?.status)

    // 2. Load Model B -> Enforces single model rule: automatically unloads Model A!
    val loadB = manager.loadModel(modelB.id).toList()
    assertTrue(loadB.last().isComplete)
    assertEquals(modelB.id, manager.activeModel.value?.id)
    assertEquals(ModelStatus.LOADED, repository.getModelById(modelB.id)?.status)
    assertEquals(ModelStatus.NOT_LOADED, repository.getModelById(modelA.id)?.status)

    // Clean up
    repository.removeModel(modelA.id)
    repository.removeModel(modelB.id)
    assertEquals(initialCount, repository.models.value.size)
  }

  @Test
  fun `test dynamic ConversationRepository persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ConversationRepository(context)

    repo.clearConversations()
    assertTrue(repo.messages.value.isEmpty())

    val msg = ChatMessage(
      isUser = true,
      text = "What is local inference?",
      timestamp = "10:30 AM"
    )
    repo.addMessage(msg)
    assertEquals(1, repo.messages.value.size)
    assertEquals("What is local inference?", repo.messages.value[0].text)

    repo.clearConversations()
    assertTrue(repo.messages.value.isEmpty())
  }

  @Test
  fun `test dynamic SettingsRepository persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = SettingsRepository(context)

    repo.updateContextLength(8192)
    assertEquals(8192, repo.settings.value.contextLength)

    repo.updateTemperature(0.5f)
    assertEquals(0.5f, repo.settings.value.temperature)

    repo.updateHistoryEnabled(false)
    assertFalse(repo.settings.value.conversationHistoryEnabled)
  }

  @Test
  fun `test LocalAIViewModel instantiation via AndroidViewModelFactory`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    val viewModel = factory.create(com.example.ui.viewmodel.LocalAIViewModel::class.java)
    assertNotNull(viewModel)
    assertNotNull(viewModel.models)
    assertNotNull(viewModel.telemetry.value)
    assertNotNull(viewModel.activeCapabilities.value)
  }
}
