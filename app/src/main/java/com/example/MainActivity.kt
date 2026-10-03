package com.example

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.FilePickerManager
import com.example.ui.components.AppTab
import com.example.ui.components.LocalAIBottomNavBar
import com.example.ui.components.MainAppHeader
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.models.ModelDetailsScreen
import com.example.ui.screens.models.ModelsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.setup.ModelSetupFlowScreen
import com.example.ui.screens.welcome.WelcomeScreen
import com.example.ui.theme.CanvasSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LocalAIViewModel

sealed class AppScreen {
    data class TabScreen(val tab: AppTab) : AppScreen()
    object Welcome : AppScreen()
    data class ModelDetails(val modelId: String) : AppScreen()
    data class ModelSetupFlow(val modelId: String) : AppScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Explicit SystemBarStyle forces transparent bars with dark (charcoal) icons so
        // the warm ivory CanvasSurface shows through the status and navigation bar areas.
        // The scrimColor is fully transparent — no grey overlay on the gesture area.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            )
        )

        setContent {
            MyApplicationTheme {
                LocalAIApp()
            }
        }
    }
}

@Composable
fun LocalAIApp(
    viewModel: LocalAIViewModel = viewModel()
) {
    val models by viewModel.models.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val tokenCount by viewModel.currentTokensCount.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isVisionSupported by viewModel.isVisionSupported.collectAsState()
    val isAudioSupported by viewModel.isAudioSupported.collectAsState()

    var currentScreen by remember {
        mutableStateOf<AppScreen>(AppScreen.Welcome)
    }

    var isNavigationInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(appSettings.isOnboardingCompleted) {
        if (!isNavigationInitialized) {
            currentScreen = if (!appSettings.isOnboardingCompleted) {
                AppScreen.Welcome
            } else {
                AppScreen.TabScreen(AppTab.CHAT)
            }
            isNavigationInitialized = true
        }
    }

    var pendingProjectorModelId by remember { mutableStateOf<String?>(null) }

    // SAF Document Picker for .gguf model files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.completeOnboarding()
            val imported = viewModel.importModel(uri)
            viewModel.startModelLoading(imported.id)
            currentScreen = AppScreen.ModelSetupFlow(imported.id)
        }
    }

    // SAF Document Picker for auxiliary multimodal projector (.mmproj GGUF)
    val projectorPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val targetId = pendingProjectorModelId
        if (uri != null && targetId != null) {
            viewModel.linkProjector(targetId, uri)
            pendingProjectorModelId = null
        }
    }

    when (val screen = currentScreen) {
        is AppScreen.Welcome -> {
            BackHandler {
                viewModel.completeOnboarding()
                currentScreen = AppScreen.TabScreen(AppTab.CHAT)
            }
            WelcomeScreen(
                recommendedModel = activeModel ?: models.firstOrNull(),
                telemetry = telemetry,
                onLoadRecommended = {
                    viewModel.completeOnboarding()
                    val targetId = activeModel?.id ?: models.firstOrNull()?.id
                    if (targetId != null) {
                        viewModel.startModelLoading(targetId)
                        currentScreen = AppScreen.ModelSetupFlow(targetId)
                    } else {
                        filePickerLauncher.launch(FilePickerManager.SUPPORTED_MIME_TYPES)
                    }
                },
                onChooseGGUFFile = {
                    viewModel.completeOnboarding()
                    filePickerLauncher.launch(FilePickerManager.SUPPORTED_MIME_TYPES)
                },
                onSkipToChat = {
                    viewModel.completeOnboarding()
                    currentScreen = AppScreen.TabScreen(AppTab.CHAT)
                }
            )
        }

        is AppScreen.ModelSetupFlow -> {
            BackHandler {
                viewModel.cancelModelLoading()
                currentScreen = AppScreen.TabScreen(AppTab.MODELS)
            }
            val targetModel = models.find { it.id == screen.modelId }
                ?: activeModel
                ?: models.firstOrNull()

            if (targetModel != null) {
                ModelSetupFlowScreen(
                    model = targetModel,
                    telemetry = telemetry,
                    loadingState = loadingState,
                    onCancel = {
                        viewModel.cancelModelLoading()
                        currentScreen = AppScreen.TabScreen(AppTab.MODELS)
                    },
                    onStartChatting = {
                        currentScreen = AppScreen.TabScreen(AppTab.CHAT)
                    }
                )
            } else {
                currentScreen = AppScreen.TabScreen(AppTab.MODELS)
            }
        }

        is AppScreen.ModelDetails -> {
            BackHandler {
                currentScreen = AppScreen.TabScreen(AppTab.MODELS)
            }
            val targetModel = models.find { it.id == screen.modelId }
                ?: activeModel
                ?: models.firstOrNull()

            if (targetModel != null) {
                ModelDetailsScreen(
                    model = targetModel,
                    telemetry = telemetry,
                    onBack = {
                        currentScreen = AppScreen.TabScreen(AppTab.MODELS)
                    },
                    onStartChatting = {
                        currentScreen = AppScreen.TabScreen(AppTab.CHAT)
                    },
                    onLoadModel = {
                        viewModel.startModelLoading(targetModel.id)
                        currentScreen = AppScreen.ModelSetupFlow(targetModel.id)
                    },
                    onUnloadFromRAM = {
                        viewModel.unloadModel(targetModel.id)
                    },
                    onRemoveModel = {
                        viewModel.removeModel(targetModel.id)
                        currentScreen = AppScreen.TabScreen(AppTab.MODELS)
                    },
                    onLinkProjector = {
                        pendingProjectorModelId = targetModel.id
                        projectorPickerLauncher.launch(FilePickerManager.SUPPORTED_MIME_TYPES)
                    }
                )
            } else {
                currentScreen = AppScreen.TabScreen(AppTab.MODELS)
            }
        }

        is AppScreen.TabScreen -> {
            val currentTab = screen.tab

            val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = CanvasSurface,
                topBar = {
                    MainAppHeader(
                        currentTabTitle = currentTab.title,
                        activeModelName = activeModel?.name ?: "No Model Active",
                        onModelPillClicked = {
                            activeModel?.id?.let {
                                currentScreen = AppScreen.ModelDetails(it)
                            } ?: run {
                                currentScreen = AppScreen.TabScreen(AppTab.MODELS)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isImeVisible) {
                        LocalAIBottomNavBar(
                            selectedTab = currentTab,
                            onTabSelected = { newTab ->
                                currentScreen = AppScreen.TabScreen(newTab)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = if (isImeVisible) 0.dp else innerPadding.calculateBottomPadding()
                        )
                        .background(CanvasSurface)
                ) {
                    when (currentTab) {
                        AppTab.CHAT -> {
                            ChatScreen(
                                messages = chatMessages,
                                inputText = inputText,
                                isGenerating = isGenerating,
                                tokenCount = tokenCount,
                                activeModelName = activeModel?.name ?: "No Model Active",
                                npuTemp = telemetry.npuTemperatureCelsius,
                                isVisionSupported = isVisionSupported,
                                isAudioSupported = isAudioSupported,
                                onInputChanged = { viewModel.onInputTextChanged(it) },
                                onSendMessage = { overrideText -> viewModel.sendMessage(overrideText) },
                                onStopGenerating = { viewModel.stopGenerating() },
                                onAttachImage = {
                                    // Supported on vision-capable models
                                    filePickerLauncher.launch(arrayOf("image/*"))
                                }
                            )
                        }

                        AppTab.MODELS -> {
                            ModelsScreen(
                                models = models,
                                telemetry = telemetry,
                                onSelectImportGGUF = {
                                    filePickerLauncher.launch(FilePickerManager.SUPPORTED_MIME_TYPES)
                                },
                                onModelDetails = { modelId ->
                                    viewModel.selectModelForDetails(modelId)
                                    currentScreen = AppScreen.ModelDetails(modelId)
                                },
                                onLoadModel = { modelId ->
                                    viewModel.startModelLoading(modelId)
                                    currentScreen = AppScreen.ModelSetupFlow(modelId)
                                },
                                onUnloadModel = { modelId ->
                                    viewModel.unloadModel(modelId)
                                },
                                onStartChat = { modelId ->
                                    viewModel.selectModelForDetails(modelId)
                                    currentScreen = AppScreen.TabScreen(AppTab.CHAT)
                                },
                                onLinkProjector = { modelId ->
                                    pendingProjectorModelId = modelId
                                    projectorPickerLauncher.launch(FilePickerManager.SUPPORTED_MIME_TYPES)
                                }
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                settings = appSettings,
                                activeModel = activeModel,
                                telemetry = telemetry,
                                onActiveModelClicked = {
                                    activeModel?.id?.let {
                                        currentScreen = AppScreen.ModelDetails(it)
                                    } ?: run {
                                        currentScreen = AppScreen.TabScreen(AppTab.MODELS)
                                    }
                                },
                                onClearConversations = {
                                    viewModel.clearAllConversations()
                                },
                                onUpdateContextLength = { newLength ->
                                    viewModel.updateContextLength(newLength)
                                },
                                onUpdateTemperature = { temp ->
                                    viewModel.updateTemperature(temp)
                                },
                                onToggleHistory = { enabled ->
                                    viewModel.toggleConversationHistory(enabled)
                                },
                                onToggleThinking = { enabled ->
                                    viewModel.updateEnableThinking(enabled)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
