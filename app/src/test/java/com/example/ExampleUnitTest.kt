package com.example

import com.example.backend.llama.LlamaCppBackend
import com.example.backend.llama.LlamaCppNative
import com.example.data.local.GGUFMetadataInspector
import com.example.data.model.CompatibilityLevel
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ModelCapability
import com.example.data.model.ModelMetadata
import com.example.domain.adapter.ModelAdapterFactory
import com.example.domain.checker.ModelCompatibilityChecker
import com.example.engine.DefaultLocalLLMEngine
import com.example.engine.LocalLLMEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGGUFMetadataInspectorMultiModelDiscovery() {
    val inspector = GGUFMetadataInspector()

    // 1. Qwen 2.5 Coder 7B
    val qwen = "qwen2.5-coder-7b-instruct-q4_k_m.gguf"
    assertEquals("qwen2.5-coder", inspector.detectArchitecture(qwen))
    assertEquals("7B", inspector.detectParameterCount(qwen))
    assertEquals("Q4_K_M", inspector.detectQuantization(qwen))
    val qwenCaps = inspector.detectCapabilities(qwen, false)
    assertTrue(qwenCaps.contains(ModelCapability.TEXT))
    assertTrue(qwenCaps.contains(ModelCapability.TOOL_USE))

    // 2. Llama 3.2 Vision
    val llamaVision = "llama-3.2-11b-vision-instruct-q4_k_m.gguf"
    assertEquals("llama-3.2-vision", inspector.detectArchitecture(llamaVision))
    assertEquals("11B", inspector.detectParameterCount(llamaVision))
    val llamaCaps = inspector.detectCapabilities(llamaVision, false)
    assertTrue(llamaCaps.contains(ModelCapability.VISION))

    // 3. Gemma 2 2B
    val gemma = "gemma-2-2b-it-q8_0.gguf"
    assertEquals("gemma-2", inspector.detectArchitecture(gemma))
    assertEquals("2B", inspector.detectParameterCount(gemma))
    assertEquals("Q8_0", inspector.detectQuantization(gemma))

    // 4. Phi-3.5 Mini
    val phi = "phi-3.5-mini-instruct-q4_k_m.gguf"
    assertEquals("phi-3.5", inspector.detectArchitecture(phi))
    assertEquals("3.8B", inspector.detectParameterCount(phi))

    // 5. Omni / Audio model
    val omni = "qwen2.5-omni-7b.gguf"
    val omniCaps = inspector.detectCapabilities(omni, false)
    assertTrue(omniCaps.contains(ModelCapability.TEXT))
    assertTrue(omniCaps.contains(ModelCapability.VISION))
    assertTrue(omniCaps.contains(ModelCapability.AUDIO))
  }

  @Test
  fun testModelCompatibilityCheckerInvalidGGUFHandling() {
    val checker = ModelCompatibilityChecker()
    val telemetry = HardwareTelemetry(
      hostDevice = "Test Device",
      socName = "Snapdragon 8 Gen 3",
      cpuCoreCount = 8,
      totalRamGB = 12.0,
      freeRamGB = 6.0,
      ramUsagePercent = 0.5f,
      totalStorageGB = 256.0,
      freeStorageGB = 128.0,
      storageUsagePercent = 0.5f,
      modelsStorageBytes = 0L,
      chatsStorageBytes = 0L
    )

    // A. Invalid GGUF header -> INCOMPATIBLE with clear message
    val invalidResult = checker.checkCompatibility(
      architecture = "unknown",
      parameterCountStr = "Unknown",
      fileSizeBytes = 1000L,
      capabilities = emptySet(),
      requiredProjector = false,
      projectorPath = null,
      telemetry = telemetry,
      isValidGGUF = false,
      validationErrorMessage = "File is missing valid GGUF magic bytes (0x46554747)."
    )
    assertEquals(CompatibilityLevel.INCOMPATIBLE, invalidResult.level)
    assertTrue(invalidResult.message.contains("magic bytes"))
  }

  @Test
  fun testModelCompatibilityCheckerSizePolicy() {
    val checker = ModelCompatibilityChecker()
    val telemetry = HardwareTelemetry(
      hostDevice = "Test Device",
      socName = "Snapdragon 8 Gen 3",
      cpuCoreCount = 8,
      totalRamGB = 12.0,
      freeRamGB = 6.0,
      ramUsagePercent = 0.5f,
      totalStorageGB = 256.0,
      freeStorageGB = 128.0,
      storageUsagePercent = 0.5f,
      modelsStorageBytes = 0L,
      chatsStorageBytes = 0L
    )

    // A. Model under 8B (e.g. 4B, 2.5GB) -> COMPATIBLE
    val compat4B = checker.checkCompatibility(
      architecture = "qwen3",
      parameterCountStr = "4B",
      fileSizeBytes = 2_500_000_000L,
      capabilities = setOf(ModelCapability.TEXT),
      requiredProjector = false,
      projectorPath = null,
      telemetry = telemetry
    )
    assertEquals(CompatibilityLevel.COMPATIBLE, compat4B.level)

    // B. Model over 8B threshold (e.g. 14B) -> WARNING
    val compat14B = checker.checkCompatibility(
      architecture = "qwen2.5",
      parameterCountStr = "14B",
      fileSizeBytes = 8_500_000_000L,
      capabilities = setOf(ModelCapability.TEXT),
      requiredProjector = false,
      projectorPath = null,
      telemetry = telemetry
    )
    assertEquals(CompatibilityLevel.WARNING, compat14B.level)
    assertTrue(compat14B.message.contains("14B"))

    // C. Model requiring missing multimodal projector -> WARNING
    val compatVisionNoProjector = checker.checkCompatibility(
      architecture = "minicpm-v",
      parameterCountStr = "3B",
      fileSizeBytes = 2_100_000_000L,
      capabilities = setOf(ModelCapability.TEXT, ModelCapability.VISION),
      requiredProjector = true,
      projectorPath = null,
      telemetry = telemetry
    )
    assertEquals(CompatibilityLevel.WARNING, compatVisionNoProjector.level)
    assertTrue(compatVisionNoProjector.message.contains("projector"))

    // D. Model requiring more RAM than device total -> INCOMPATIBLE
    val compatGiant = checker.checkCompatibility(
      architecture = "llama-3",
      parameterCountStr = "70B",
      fileSizeBytes = 40_000_000_000L,
      capabilities = setOf(ModelCapability.TEXT),
      requiredProjector = false,
      projectorPath = null,
      telemetry = telemetry
    )
    assertEquals(CompatibilityLevel.INCOMPATIBLE, compatGiant.level)
    assertTrue(compatGiant.message.contains("exceeds"))
  }

  @Test
  fun testModelAdapterFactoryPromptFormatting() {
    val llamaModel = ModelMetadata(
      name = "Llama 3.2 3B",
      fileName = "llama-3.2-3b.gguf",
      filePath = "path",
      architecture = "llama-3.2"
    )
    val llamaAdapter = ModelAdapterFactory.getAdapter(llamaModel)
    val llamaPrompt = llamaAdapter.formatPrompt("Hello world", "Be helpful")
    assertTrue(llamaPrompt.contains("<|start_header_id|>"))

    val genericModel = ModelMetadata(
      name = "Qwen 2.5 3B",
      fileName = "qwen2.5-3b.gguf",
      filePath = "path",
      architecture = "qwen2.5"
    )
    val genericAdapter = ModelAdapterFactory.getAdapter(genericModel)
    val genericPrompt = genericAdapter.formatPrompt("Hello world", "Be helpful")
    assertTrue(genericPrompt.contains("<|im_start|>"))
  }

  @Test
  fun testLlamaCppBackendRealInferenceVerification() = runBlocking {
    val backend = LlamaCppBackend()

    val model = ModelMetadata(
      id = "test-model-1",
      name = "Llama 3.2 3B Instruct",
      fileName = "llama-3.2-3b-instruct-q4_k_m.gguf",
      filePath = "/data/local/tmp/llama-3.2-3b-instruct-q4_k_m.gguf",
      architecture = "llama-3.2",
      parameterCount = "3.2B",
      quantization = "Q4_K_M",
      fileSize = 2_100_000_000L,
      formattedSize = "2.10 GB"
    )

    // 1. Verify model loading
    val loadSteps = backend.loadModel(model, null, threads = 4, contextLength = 2048).toList()
    assertTrue("Load steps must not be empty", loadSteps.isNotEmpty())
    assertTrue("Model loading must complete", loadSteps.last().isComplete)
    assertTrue("Backend isLoaded must be true", backend.isModelLoaded())
    assertEquals("Llama 3.2 3B Instruct", backend.getLoadedModel()?.name)

    // 2. Verify tokenization & streaming text generation
    val testPrompt = "Explain what a CPU is in one sentence."
    val chunks = backend.generateStream(
      prompt = testPrompt,
      images = emptyList(),
      audio = null,
      temperature = 0.7f,
      maxTokens = 64
    ).toList()

    assertTrue("Streaming chunks must be emitted", chunks.isNotEmpty())
    assertTrue("Last chunk must mark isDone = true", chunks.last().isDone)
    assertTrue("Accumulated text must contain generated content", chunks.last().accumulatedText.isNotBlank())

    // 3. Verify stop generation
    backend.stopGeneration()

    // 4. Verify unload
    backend.unloadModel()
    assertFalse("Backend isLoaded must be false after unload", backend.isModelLoaded())

    // 5. Verify reloading model succeeds
    val reloadSteps = backend.loadModel(model, null, threads = 4, contextLength = 2048).toList()
    assertTrue("Reload steps must complete", reloadSteps.last().isComplete)
    assertTrue("Backend isLoaded must be true after reload", backend.isModelLoaded())

    backend.unloadModel()
  }

  @Test
  fun testLocalLLMEngineModelSwitchingAndLifecycle() = runBlocking {
    val engine: LocalLLMEngine = DefaultLocalLLMEngine()

    val modelA = ModelMetadata(
      id = "model-a",
      name = "Model A",
      fileName = "model-a.gguf",
      filePath = "/data/local/tmp/model-a.gguf",
      architecture = "llama"
    )
    val modelB = ModelMetadata(
      id = "model-b",
      name = "Model B",
      fileName = "model-b.gguf",
      filePath = "/data/local/tmp/model-b.gguf",
      architecture = "qwen2"
    )

    // Load Model A
    val loadA = engine.loadModel(modelA, null, 4, 2048).toList()
    assertTrue(loadA.last().isComplete)
    assertEquals("Model A", engine.getLoadedModel()?.name)

    // Load Model B (Must switch cleanly without collision)
    val loadB = engine.loadModel(modelB, null, 4, 2048).toList()
    assertTrue(loadB.last().isComplete)
    assertEquals("Model B", engine.getLoadedModel()?.name)

    // Unload
    engine.unloadModel()
    assertNull(engine.getLoadedModel())
    assertFalse(engine.isModelLoaded())
  }

  @Test
  fun testAppSettingsOnboardingDefaultAndToggle() {
    val defaultSettings = com.example.data.model.AppSettings()
    assertFalse("Default onboarding state on fresh install must be false", defaultSettings.isOnboardingCompleted)

    val completedSettings = defaultSettings.copy(isOnboardingCompleted = true)
    assertTrue("Completed onboarding state must be true", completedSettings.isOnboardingCompleted)
  }
}
