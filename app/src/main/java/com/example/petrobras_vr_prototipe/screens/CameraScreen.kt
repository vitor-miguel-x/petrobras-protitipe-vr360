package com.example.petrobras_vr_prototipe.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
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

// Mapa de Áudios Mantido igual
val audioMap = mapOf(
    "abrindo_rede" to R.raw.abrindo_ambiente_rede,
    "analisando_sensores" to R.raw.analizando_dados_sensores,
    "buscando_corp" to R.raw.buscando_dados_corporativo,
    "com_certeza" to R.raw.com_certeza,
    "conexao_instavel" to R.raw.conexao_instavel_detect,
    "consulta_ok" to R.raw.consulta_concluida,
    "consultando_gemini" to R.raw.consultando_infos_gemini,
    "erro_inconsistencia" to R.raw.detectei_inconsistencia,
    "conexao_rede" to R.raw.estabelecendo_conexao_rede_corporativa,
    "cuidando_disso" to R.raw.estou_cuidando_disso_para_voce,
    "exibindo_tarefas" to R.raw.exibindo_tarefa_pendentes,
    "multiplas_fontes" to R.raw.indentifiquei_multiplas_fontes,
    "lembrete_ok" to R.raw.lembrete_configurado,
    "nenhum_erro" to R.raw.nenhum_erro_encontrado,
    "nova_atividade" to R.raw.nova_atividade_registrada,
    "organizando_agenda" to R.raw.organizando_agenda_corporativa,
    "priorizando_prazos" to R.raw.priorizando_atividade_corformee_prazos_definidos,
    "procedimento_padrao" to R.raw.procedimento_padrao,
    "recursos_tela" to R.raw.recursos_disponiveis_tela,
    "sessao_ok" to R.raw.sessao_autenticada,
    "sincronizando" to R.raw.sincronizando_base_dados,
    "instante" to R.raw.so_um_instante,
    "validando_dados" to R.raw.validando_integridade_dados,
    "tarefas_pendentes" to R.raw.voce_possui_tarefas_pendentes,
    "executando_diagnostico" to R.raw.executando_diagnostico_do_sistema,
    "procedimento_identificado" to R.raw.procedimento_identificado_exibindo_instrucoes_passo_a_passo,
    "acesso_liberado" to R.raw.acesso_liberado_monitorando_trafego_de_rede,
    "resolvendo_eficiencia" to R.raw.resolvindo_com_eficiencia,
    "sessao_networking" to R.raw.abrindo_sessao_networking,
    "atualizando_realtime" to R.raw.atualizando_informacoes_em_tempo_real,
)

fun falar(context: Context, tts: TextToSpeech?, audioResId: Int?, textoBackup: String) {
    if (audioResId != null) {
        try {
            val mediaPlayer = MediaPlayer.create(context, audioResId)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            tts?.speak(textoBackup, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    } else {
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

    // Variáveis Multi-Tarefas
    var isNetworkingOpen by remember { mutableStateOf(false) }
    var isTasksOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Variáveis de Âncora (Evita que a tela nasça lá no teto ou no chão)
    var anchorYaw by remember { mutableFloatStateOf(0f) }
    var anchorPitch by remember { mutableFloatStateOf(0f) }

    // Sensores
    var initialPitch by remember { mutableFloatStateOf(Float.NaN) }
    var lastYaw by remember { mutableFloatStateOf(Float.NaN) }
    var accumulatedYaw by remember { mutableFloatStateOf(0f) }

    var rawSensorYaw by remember { mutableFloatStateOf(0f) }
    var rawSensorPitch by remember { mutableFloatStateOf(0f) }

    val smoothYaw by animateFloatAsState(targetValue = rawSensorYaw, animationSpec = tween(150), label = "smoothYaw")
    val smoothPitch by animateFloatAsState(targetValue = rawSensorPitch, animationSpec = tween(150), label = "smoothPitch")

    // Função que "puxa" a janela para frente do rosto do usuário
    fun snapWindowsToCenter() {
        if (!isNetworkingOpen && !isTasksOpen && !isSettingsOpen) {
            anchorYaw = rawSensorYaw
            anchorPitch = rawSensorPitch
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // 1. Mudamos para TYPE_GAME_ROTATION_VECTOR (ignora a bússola e foca no giroscópio)
        // Usamos o TYPE_ROTATION_VECTOR apenas como fallback caso o celular não tenha o sensor de jogo.
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
                    event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {

                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    val remappedMatrix = FloatArray(9)
                    // Mapeamento correto para a câmera traseira do celular no modo Retrato
                    SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedMatrix
                    )

                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(remappedMatrix, orientationAngles)

                    val currentYawDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val currentPitchDegrees = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()

                    if (lastYaw.isNaN()) {
                        lastYaw = currentYawDegrees
                        initialPitch = currentPitchDegrees
                        // Inicializa âncoras
                        anchorYaw = currentYawDegrees
                        anchorPitch = 0f
                    }

                    var yawDiff = currentYawDegrees - lastYaw
                    // Prevenção de pulo (wrap-around) quando o sensor passa de 180 para -180
                    if (yawDiff > 180f) yawDiff -= 360f
                    if (yawDiff < -180f) yawDiff += 360f

                    lastYaw = currentYawDegrees
                    accumulatedYaw += yawDiff

                    rawSensorYaw = accumulatedYaw
                    rawSensorPitch = currentPitchDegrees - initialPitch
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    var hasAudioPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.init(context)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                val voices = tts?.voices
                val maleVoice = voices?.find { it.name.lowercase().contains("male") || it.name.lowercase().contains("pt-br-x-afs-local") }
                if (maleVoice != null) tts?.voice = maleVoice

                val biometricManager = BiometricManager.from(context)
                when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> falar(context, tts, null, "Brás informando: Sem sensores de Face ID.")
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> falar(context, tts, null, "Configure sua biometria.")
                    BiometricManager.BIOMETRIC_SUCCESS -> falar(context, tts, audioMap["validando_dados"], "Sistemas prontos.")
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
    }

    LaunchedEffect(Unit) { launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }

    val networkingBitmap = rememberSafeBitmap(R.drawable.networking_screen, targetWidth = 800)
    val tasksBitmap = rememberSafeBitmap(R.drawable.tarefas_screen, targetWidth = 800)

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
                        text.contains("networking") || text.contains("rede") -> {
                            snapWindowsToCenter()
                            isNetworkingOpen = true
                            // SUBSTITUIÇÃO: Usando o novo áudio de sessão networking
                            falar(context, tts, audioMap["sessao_networking"], "Abrindo sessão de networking.")
                        }
                        text.contains("diagnóstico") || text.contains("status") -> {
                            // NOVO COMANDO: Executa um "fake" diagnóstico
                            falar(context, tts, audioMap["executando_diagnostico"], "Executando diagnóstico do sistema.")
                            scope.launch {
                                delay(3000)
                                falar(context, tts, audioMap["resolvendo_eficiencia"], "Resolvido com eficiência.")
                            }
                        }
                        text.contains("atualizar") || text.contains("refresh") -> {
                            // NOVO COMANDO: Feedback visual/sonoro de atualização
                            falar(context, tts, audioMap["atualizando_realtime"], "Atualizando informações em tempo real.")
                        }
                        text.contains("tarefa") || text.contains("tarefas") || text.contains("agenda") -> {
                            snapWindowsToCenter()
                            isTasksOpen = true
                            falar(context, tts, audioMap["exibindo_tarefas"], "Exibindo tarefas.")
                        }
                        text.contains("fechar") || text.contains("sair") || text.contains("ocultar") -> {
                            isNetworkingOpen = false
                            isTasksOpen = false
                            isSettingsOpen = false
                            falar(context, tts, null, "Minimizando interfaces.")
                        }
                        text.contains("configurações") || text.contains("ajustes") || text.contains("sistema") -> {
                            snapWindowsToCenter()
                            isSettingsOpen = true
                            falar(context, tts, audioMap["recursos_tela"], "Abrindo painel de configurações.")
                        }
                        else -> falar(context, tts, null, "Comando não reconhecido.")
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
                                    falar(context, tts, audioMap["sessao_ok"], "Identidade confirmada.")
                                    currentAppState = VrAppState.WELCOME
                                },
                                onError = {
                                    isCameraPaused = false
                                    falar(context, tts, audioMap["erro_inconsistencia"], "Falha na identificação.")
                                }
                            )
                        }
                    }
                }
            }

            VrAppState.WELCOME -> {
                LaunchedEffect(Unit) { delay(3000); currentAppState = VrAppState.AR_IDLE }
                WelcomeMessageOverlay()
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 25.dp, vertical = 40.dp)) {

                    // MENU HOME NO CANTO INFERIOR
                    Box(modifier = Modifier.align(Alignment.BottomStart)) {
                        MenuHome(
                            isNetworkingOpen = isNetworkingOpen,
                            isTasksOpen = isTasksOpen,
                            isSettingsOpen = isSettingsOpen,
                            onNavigate = { abaClicada ->
                                snapWindowsToCenter() // Puxa para a frente do usuário
                                when (abaClicada) {
                                    VrAppState.NETWORKING -> {
                                        isNetworkingOpen = !isNetworkingOpen
                                        if (isNetworkingOpen) falar(context, tts, audioMap["abrindo_rede"], "Abrindo Networking.")
                                    }
                                    VrAppState.TASKS -> {
                                        isTasksOpen = !isTasksOpen
                                        if (isTasksOpen) falar(context, tts, audioMap["exibindo_tarefas"], "Exibindo tarefas.")
                                    }
                                    VrAppState.SETTINGS -> {
                                        isSettingsOpen = !isSettingsOpen
                                        if (isSettingsOpen) falar(context, tts, audioMap["recursos_tela"], "Abrindo painel de configurações.")
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }

                    // JANELAS (TODAS JUNTAS E DESLOCADAS PARA BAIXO DO CENTRO)
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 150.dp) // <- Empurra o painel para baixo, liberando a visão central
                            .graphicsLayer {
                                translationX = -((smoothYaw - anchorYaw) * 20f)
                                translationY = -((smoothPitch - anchorPitch) * 15f).coerceIn(-400f, 400f)
                            },
                        horizontalArrangement = Arrangement.spacedBy(40.dp), // <- Dá o espaçamento entre as telas
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // TELA 1: NETWORKING
                        AnimatedVisibility(
                            visible = isNetworkingOpen && networkingBitmap != null,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                        ) {
                            networkingBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(350.dp)
                                        .clickable { isNetworkingOpen = false }
                                )
                            }
                        }

                        // TELA 2: TAREFAS
                        AnimatedVisibility(
                            visible = isTasksOpen && tasksBitmap != null,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                        ) {
                            tasksBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(400.dp)
                                        .clickable { isTasksOpen = false }
                                )
                            }
                        }

                        // TELA 3: SETTINGS (Agora lado a lado com as outras)
                        AnimatedVisibility(
                            visible = isSettingsOpen,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                        ) {
                            Box(modifier = Modifier.width(350.dp)) { // Limitando a largura para encaixar bem no layout multi-telas
                                SettingsOverlay(
                                    onClose = {
                                        isSettingsOpen = false
                                        falar(context, tts, null, "Minimizando configurações.")
                                    },
                                    onResetBiometrics = {
                                        falar(context, tts, audioMap["procedimento_padrao"], "Iniciando recalibração biométrica.")
                                        currentAppState = VrAppState.AUTHENTICATING
                                    }
                                )
                            }
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