package com.example.petrobras_vr_prototipe.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.petrobras_vr_prototipe.R
import com.example.petrobras_vr_prototipe.components.IrisAuthOverlay
import com.example.petrobras_vr_prototipe.components.MenuHome
import com.example.petrobras_vr_prototipe.components.WelcomeMessageOverlay
import com.example.petrobras_vr_prototipe.model.EyeAnalyzer
import com.example.petrobras_vr_prototipe.util.rememberSafeBitmap
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CameraScreen() {

    val context = LocalContext.current
    var detectedFaceId by remember { mutableStateOf<Int?>(null) }
    var currentAppState by remember { mutableStateOf<VrAppState>(VrAppState.AUTHENTICATING) }

    // 1. PERMISSÕES
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    // 2. VOZ MASCULINA E TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")

                // Tenta encontrar uma voz masculina no sistema
                val voices = tts?.voices
                val maleVoice = voices?.find { it.name.lowercase().contains("male") || it.name.lowercase().contains("pt-br-x-afs-local") }
                if (maleVoice != null) {
                    tts?.voice = maleVoice
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
    }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    // 3. RECURSOS
    val iconBitmap = rememberSafeBitmap(R.drawable.icone_petrobras, targetWidth = 200)
    val dashboardBitmap = rememberSafeBitmap(R.drawable.frame_1, targetWidth = 800)
    val networkingBitmap = rememberSafeBitmap(R.drawable.networking_screen, targetWidth = 800)
    val tasksBitmap = rememberSafeBitmap(R.drawable.group_1, targetWidth = 800)

    // 4. LÓGICA DE IA AVANÇADA (BRÁS)
    DisposableEffect(hasAudioPermission) {
        if (!hasAudioPermission) return@DisposableEffect onDispose {}

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        }

        val listener = object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                val text = matches.joinToString(" ").lowercase()

                // Gatilho Principal: "Brás"
                if (text.contains("brás") || text.contains("braz") || text.contains("bras")) {

                    when {
                        // Comando: Abrir Menu/Dashboard
                        text.contains("bate-papo") || text.contains("chat")  -> {
                            tts?.speak("Com certeza. Abrindo Secção Networking principal.", TextToSpeech.QUEUE_FLUSH, null, null)
                            currentAppState = VrAppState.NETWORKING
                        }

                        // Comando: Barra de Tarefas
                        text.contains("tarefa") || text.contains("trabalho") || text.contains("work") || text.contains("working")-> {
                            tts?.speak("Entendido. Exibindo suas tarefas pendentes.", TextToSpeech.QUEUE_FLUSH, null, null)
                            currentAppState = VrAppState.TASKS
                        }

                        // Comando: Fechar tudo / Voltar
                        text.contains("fechar") || text.contains("sair") || text.contains("limpar") || text.contains("voltar") || text.contains("exit") || text.contains("return") -> {
                            tts?.speak("Certo. Minimizando interfaces.", TextToSpeech.QUEUE_FLUSH, null, null)
                            currentAppState = VrAppState.AR_IDLE
                        }

                        // Resposta padrão caso só chame o nome
                        else -> {
                            tts?.speak("Sim? Como posso ajudar?", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                }
                speechRecognizer.startListening(intent)
            }
            override fun onError(error: Int) { speechRecognizer.startListening(intent) }
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(intent)

        onDispose {
            speechRecognizer.destroy()
            tts?.stop()
            tts?.shutdown()
        }
    }

    // --- 5. INTERFACE DO USUÁRIO ---
    Box(modifier = Modifier.fillMaxSize()) {
        // CAMADA DA CÂMERA
        if (hasCameraPermission) {
            SmartCameraPreview(
                isFrontCamera = currentAppState == VrAppState.AUTHENTICATING,
                onEyeDetectedState = { id -> detectedFaceId = id }
            )
        }

        // CAMADA DE UI (OVERLAYS E DASHBOARD)
        when (currentAppState) {
            VrAppState.AUTHENTICATING -> {
                IrisAuthOverlay(detectedFaceId = detectedFaceId) {
                    currentAppState = VrAppState.WELCOME
                }
            }
            VrAppState.WELCOME -> {
                LaunchedEffect(Unit) {
                    delay(3000)
                    currentAppState = VrAppState.AR_IDLE
                }
                WelcomeMessageOverlay()
            }
            else -> {
                // Este Box agora serve como o "espaço sideral" onde posicionamos os itens
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 25.dp, end = 25.dp, bottom = 40.dp) // Padding nas duas laterais
                ) {

                    // --- 1. MENU/ÍCONE À ESQUERDA ---
                    Box(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        if (iconBitmap != null) {
                            MenuHome(onNavigate = { newState ->
                                currentAppState = newState

                                // Dica: Você pode até fazer o Brás falar ao clicar!
                                val mensagem = if(newState == VrAppState.TASKS) "Abrindo tarefas" else "Abrindo Networking"
                                tts?.speak(mensagem, TextToSpeech.QUEUE_FLUSH, null, null)
                            })
                        }
                    }

                    // --- 2. TAREFAS/DASHBOARD À DIREITA ---
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd), // Alinha a coluna toda à direita
                        horizontalAlignment = Alignment.End,           // Alinha o conteúdo da coluna à direita
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (currentAppState == VrAppState.NETWORKING && networkingBitmap != null) {
                            Image(
                                bitmap = networkingBitmap.asImageBitmap(),
                                contentDescription = "Dashboard",
                                modifier = Modifier
                                    .size(350.dp)
                                    .clickable { currentAppState = VrAppState.TASKS },
                                contentScale = ContentScale.Fit
                            )
                        } else if (currentAppState == VrAppState.TASKS && tasksBitmap != null) {
                            Image(
                                bitmap = tasksBitmap.asImageBitmap(),
                                contentDescription = "Tarefas",
                                modifier = Modifier
                                    .size(400.dp)
                                    .clickable { currentAppState = VrAppState.AR_IDLE },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

// Certifique-se de que o Enum e a SmartCameraPreview continuam abaixo no mesmo arquivo
enum class VrAppState {
    AUTHENTICATING, WELCOME, AR_IDLE, NETWORKING, TASKS
}
@Composable
fun SmartCameraPreview(isFrontCamera: Boolean, onEyeDetectedState: (Int?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } },
        update = { previewView ->
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                if (isFrontCamera) {
                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { it.setAnalyzer(executor, EyeAnalyzer { id -> onEyeDetectedState(id) }) }
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
                } else {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                }
            }, executor)
        }
    )
}