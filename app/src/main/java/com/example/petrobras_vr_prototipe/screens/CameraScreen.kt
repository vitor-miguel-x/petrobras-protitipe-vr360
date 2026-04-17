package com.example.petrobras_vr_prototipe.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.example.petrobras_vr_prototipe.components.SettingsOverlay
import com.example.petrobras_vr_prototipe.components.WelcomeMessageOverlay
import com.example.petrobras_vr_prototipe.model.EyeAnalyzer
import com.example.petrobras_vr_prototipe.util.BiometricUtils
import com.example.petrobras_vr_prototipe.util.rememberSafeBitmap
import com.example.petrobras_vr_prototipe.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class VrAppState {
    AUTHENTICATING, WELCOME, AR_IDLE, NETWORKING, TASKS, SETTINGS
}

val audioMap = mapOf(
    "abrindo_rede" to R.raw.abrindo_ambiente_rede,
    "analisando_sensores" to R.raw.analizando_dados_sensores, // Nome do arquivo com 'z'
    "buscando_corp" to R.raw.buscando_dados_corporativo,
    "com_certeza" to R.raw.com_certeza,
    "conexao_instavel" to R.raw.conexao_instavel_detect,
    "consulta_ok" to R.raw.consulta_concluida,
    "consultando_gemini" to R.raw.consultando_infos_gemini,
    "erro_inconsistencia" to R.raw.detectei_inconsistencia,
    "conexao_rede" to R.raw.estabelecendo_conexao_rede_corporativa,
    "cuidando_disso" to R.raw.estou_cuidando_disso_para_voce,
    "exibindo_tarefas" to R.raw.exibindo_tarefa_pendentes,
    "multiplas_fontes" to R.raw.indentifiquei_multiplas_fontes, // Nome do arquivo com 'n'
    "lembrete_ok" to R.raw.lembrete_configurado,
    "nenhum_erro" to R.raw.nenhum_erro_encontrado,
    "nova_atividade" to R.raw.nova_atividade_registrada,
    "organizando_agenda" to R.raw.organizando_agenda_corporativa,
    "priorizando_prazos" to R.raw.priorizando_atividade_corformee_prazos_definidos, // Nome exato
    "procedimento_padrao" to R.raw.procedimento_padrao,
    "recursos_tela" to R.raw.recursos_disponiveis_tela,
    "sessao_ok" to R.raw.sessao_autenticada,
    "sincronizando" to R.raw.sincronizando_base_dados,
    "instante" to R.raw.so_um_instante,
    "validando_dados" to R.raw.validando_integridade_dados,
    "tarefas_pendentes" to R.raw.voce_possui_tarefas_pendentes
)

fun falar(context: Context, tts: TextToSpeech?, audioResId: Int?, textoBackup: String) {
    if (audioResId != null) {
        try {
            val mediaPlayer = MediaPlayer.create(context, audioResId)
            mediaPlayer.setOnCompletionListener { it.release() } // Libera a memória após tocar
            mediaPlayer.start()
        } catch (e: Exception) {
            // Se o arquivo falhar por algum motivo, usa o TTS como backup
            tts?.speak(textoBackup, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    } else {
        // Se passarmos null (não há áudio correspondente), usa o TTS padrão
        tts?.speak(textoBackup, TextToSpeech.QUEUE_FLUSH, null, null)
    }
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
                    // Mantive o Fallback (null) para mensagens de erro de hardware
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> falar(context, tts, null, "Brás informando: Sem sensores de Face ID.")
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> falar(context, tts, null, "Configure sua biometria.")
                    BiometricManager.BIOMETRIC_SUCCESS -> falar(context, tts, audioMap["validando_dados"], "Sistemas prontos.")
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

                // Gatilhos de ativação expandidos
                if (text.contains("brás") || text.contains("braz") || text.contains("assistente")) {
                    when {
                        text.contains("networking") || text.contains("rede") || text.contains("bate-papo") || text.contains("conexão") -> {
                            falar(context, tts, audioMap["abrindo_rede"], "Abrindo Networking.")
                            currentAppState = VrAppState.NETWORKING
                        }
                        text.contains("tarefa") || text.contains("tarefas") || text.contains("agenda") || text.contains("atividades") -> {
                            falar(context, tts, audioMap["exibindo_tarefas"], "Exibindo tarefas.")
                            currentAppState = VrAppState.TASKS
                        }
                        text.contains("fechar") || text.contains("sair") || text.contains("minimizar") || text.contains("ocultar") -> {
                            falar(context, tts, null, "Minimizando interfaces.")
                            currentAppState = VrAppState.AR_IDLE
                        }
                        text.contains("status") || text.contains("sensores") || text.contains("diagnóstico") -> {
                            falar(context, tts, audioMap["analisando_sensores"], "Analisando dados dos sensores.")
                        }
                        text.contains("configurações") || text.contains("ajustes") || text.contains("sistema") || text.contains("opções") -> {
                            falar(context, tts, audioMap["recursos_tela"], "Abrindo painel de configurações.")
                            currentAppState = VrAppState.SETTINGS
                        }
                        else -> {
                            // Feedback para o usuário saber que a IA o ouviu, mas não identificou o comando
                            falar(context, tts, null, "Comando não reconhecido. Pode repetir?")
                        }
                    }
                }
                // Reinicia a escuta
                speechRecognizer.startListening(intent)
            }
            override fun onError(error: Int) {
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
                                    falar(
                                        context,
                                        tts,
                                        audioMap["sessao_ok"],
                                        "Identidade confirmada."
                                    )
                                    currentAppState = VrAppState.WELCOME
                                },
                                onError = {
                                    isCameraPaused = false
                                    falar(
                                        context,
                                        tts,
                                        audioMap["erro_inconsistencia"],
                                        "Falha na identificação."
                                    )
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
            // Concentramos todas as janelas AR no else para compartilhar o fundo e as animações
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 25.dp, vertical = 40.dp)
                ) {

                    // MENU HOME (Fica sempre visível no fundo)
                    Box(modifier = Modifier.align(Alignment.BottomStart)) {
                        MenuHome(
                            onNavigate = { novoEstado ->
                                currentAppState = novoEstado

                                when (novoEstado) {
                                    VrAppState.NETWORKING -> falar(
                                        context,
                                        tts,
                                        audioMap["abrindo_rede"],
                                        "Abrindo Networking."
                                    )

                                    VrAppState.TASKS -> falar(
                                        context,
                                        tts,
                                        audioMap["exibindo_tarefas"],
                                        "Exibindo tarefas."
                                    )

                                    VrAppState.SETTINGS -> falar(
                                        context,
                                        tts,
                                        audioMap["recursos_tela"],
                                        "Abrindo painel de configurações."
                                    )

                                    VrAppState.AR_IDLE -> falar(
                                        context,
                                        tts,
                                        null,
                                        "Minimizando interfaces."
                                    )

                                    else -> {}
                                }
                            }
                        )
                    }

                    // JANELAS ANIMADAS (Networking e Tasks)
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalAlignment = Alignment.End
                    ) {

                        AnimatedVisibility(
                            visible = currentAppState == VrAppState.NETWORKING && networkingBitmap != null,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(500)
                            ),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(300)
                            )
                        ) {
                            networkingBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(350.dp)
                                        .clickable { currentAppState = VrAppState.TASKS }
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = currentAppState == VrAppState.TASKS && tasksBitmap != null,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(500)
                            ),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(300)
                            )
                        ) {
                            tasksBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(400.dp)
                                        .clickable { currentAppState = VrAppState.AR_IDLE }
                                )
                            }
                        }
                    }

                    // SETTINGS OVERLAY ANIMADO (Centralizado)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                        AnimatedVisibility(
                            visible = currentAppState == VrAppState.SETTINGS,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(500)
                            ),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(300)
                            )
                        ) {
                            SettingsOverlay(
                                onClose = {
                                    falar(context, tts, null, "Minimizando configurações.")
                                    currentAppState = VrAppState.AR_IDLE
                                },
                                onResetBiometrics = {
                                    falar(
                                        context,
                                        tts,
                                        audioMap["procedimento_padrao"],
                                        "Iniciando recalibração biométrica."
                                    )
                                    currentAppState =
                                        VrAppState.AUTHENTICATING // Volta pra tela de login
                                }
                            )
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