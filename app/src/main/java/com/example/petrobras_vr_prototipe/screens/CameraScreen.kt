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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petrobras_vr_prototipe.R
import com.example.petrobras_vr_prototipe.components.IrisAuthOverlay
import com.example.petrobras_vr_prototipe.components.MenuHome
import com.example.petrobras_vr_prototipe.components.WelcomeMessageOverlay
import com.example.petrobras_vr_prototipe.model.EyeAnalyzer
import com.example.petrobras_vr_prototipe.util.BiometricUtils
import com.example.petrobras_vr_prototipe.util.rememberSafeBitmap
import com.example.petrobras_vr_prototipe.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class VrAppState {
    AUTHENTICATING, WELCOME, AR_IDLE, NETWORKING, TASKS
}

@Composable
fun CameraScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isCameraPaused by remember { mutableStateOf(false) }
    var detectedFaceId by remember { mutableStateOf<Int?>(null) }
    var currentAppState by remember { mutableStateOf<VrAppState>(VrAppState.AUTHENTICATING) }

    // 1. PERMISSÕES
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    // 2. TTS COM GERENCIAMENTO DE CICLO DE VIDA
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.init(context)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                val voices = tts?.voices
                val maleVoice = voices?.find {
                    it.name.lowercase().contains("male") || it.name.lowercase().contains("pt-br-x-afs-local")
                }
                if (maleVoice != null) tts?.voice = maleVoice

                val biometricManager = BiometricManager.from(context)
                when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> tts?.speak("Brás informando: Sem sensores de Face ID.", TextToSpeech.QUEUE_FLUSH, null, null)
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> tts?.speak("Configure sua biometria.", TextToSpeech.QUEUE_FLUSH, null, null)
                    BiometricManager.BIOMETRIC_SUCCESS -> tts?.speak("Sistemas prontos.", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    // Limpeza crucial para o TTS não travar
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
    }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    val networkingBitmap = rememberSafeBitmap(R.drawable.networking_screen, targetWidth = 800)
    val tasksBitmap = rememberSafeBitmap(R.drawable.tarefas_screen, targetWidth = 800)

    // 4. VOZ (BRÁS) - MELHORADO
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

                if (text.contains("brás") || text.contains("braz")) {
                    when {
                        text.contains("networking") || text.contains("bate-papo") -> {
                            tts?.speak("Abrindo Networking.", TextToSpeech.QUEUE_FLUSH, null, null)
                            currentAppState = VrAppState.NETWORKING
                        }
                        text.contains("tarefa") || text.contains("agenda") -> {
                            tts?.speak("Exibindo tarefas.", TextToSpeech.QUEUE_FLUSH, null, null)
                            currentAppState = VrAppState.TASKS
                        }
                        text.contains("fechar") || text.contains("sair") -> {
                            tts?.speak("Minimizando interfaces.", TextToSpeech.QUEUE_FLUSH, null, null)
                            currentAppState = VrAppState.AR_IDLE
                        }
                    }
                }
                // Reinicia a escuta
                speechRecognizer.startListening(intent)
            }
            override fun onError(error: Int) {
                // Evita loops infinitos de erro reiniciando com delay se necessário
                speechRecognizer.startListening(intent)
            }
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

        onDispose { speechRecognizer.destroy() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission && !isCameraPaused) {
            SmartCameraPreview(
                isFrontCamera = (currentAppState == VrAppState.AUTHENTICATING),
                onEyeDetectedState = { id -> detectedFaceId = id }
            )
        }

        when (currentAppState) {
            VrAppState.AUTHENTICATING -> {
                IrisAuthOverlay(detectedFaceId = detectedFaceId) {
                    activity?.let { fragmentActivity ->
                        isCameraPaused = true
                        scope.launch {
                            delay(800)
                            BiometricUtils.autenticar(
                                activity = fragmentActivity,
                                onSucesso = {
                                    isCameraPaused = false
                                    tts?.speak("Identidade confirmada.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    currentAppState = VrAppState.WELCOME
                                },
                                onError = {
                                    isCameraPaused = false
                                    tts?.speak("Falha na identificação.", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            )
                        }
                    }
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
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 25.dp, vertical = 40.dp)) {
                    Box(modifier = Modifier.align(Alignment.BottomStart)) {
                        MenuHome(onNavigate = { novoEstado ->
                            // 1. Atualiza o estado da tela
                            currentAppState = novoEstado

                            // 2. Dispara o áudio correspondente ao clique manual
                            when (novoEstado) {
                                VrAppState.NETWORKING -> tts?.speak("Abrindo Networking.", TextToSpeech.QUEUE_FLUSH, null, null)
                                VrAppState.TASKS -> tts?.speak("Exibindo tarefas.", TextToSpeech.QUEUE_FLUSH, null, null)
                                VrAppState.AR_IDLE -> tts?.speak("Minimizando interfaces.", TextToSpeech.QUEUE_FLUSH, null, null)
                                else -> {}
                            }
                        })
                    }
                    Column(modifier = Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
                        if (currentAppState == VrAppState.NETWORKING && networkingBitmap != null) {
                            Image(bitmap = networkingBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(350.dp).clickable { currentAppState = VrAppState.TASKS })
                        } else if (currentAppState == VrAppState.TASKS && tasksBitmap != null) {
                            Image(bitmap = tasksBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(400.dp).clickable { currentAppState = VrAppState.AR_IDLE })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartCameraPreview(isFrontCamera: Boolean, onEyeDetectedState: (Int?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

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