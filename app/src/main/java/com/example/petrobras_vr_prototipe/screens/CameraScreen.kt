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
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petrobras_vr_prototipe.R
import com.example.petrobras_vr_prototipe.components.AiAssistantSprite
import com.example.petrobras_vr_prototipe.components.IrisAuthOverlay
import com.example.petrobras_vr_prototipe.components.MenuHome
import com.example.petrobras_vr_prototipe.components.NfcReadingOverlay
import com.example.petrobras_vr_prototipe.components.SettingsOverlay
import com.example.petrobras_vr_prototipe.components.SmartCameraPreview
import com.example.petrobras_vr_prototipe.components.WelcomeMessageOverlay
import com.example.petrobras_vr_prototipe.model.EyeAnalyzer
import com.example.petrobras_vr_prototipe.util.BiometricUtils
import com.example.petrobras_vr_prototipe.util.rememberSafeBitmap
import com.example.petrobras_vr_prototipe.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class VrAppState {
    AUTHENTICATING, WELCOME, AR_IDLE, NETWORKING, TASKS, SETTINGS, NFC_READING
}

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

var currentMediaPlayer: MediaPlayer? = null

fun falar(
    context: Context,
    tts: TextToSpeech?,
    audioResId: Int?,
    textoBackup: String,
    onSpeakStateChange: (String) -> Unit
) {
    // 1. Para e limpa qualquer áudio que esteja tocando agora
    currentMediaPlayer?.stop()
    currentMediaPlayer?.release()
    currentMediaPlayer = null

    onSpeakStateChange(textoBackup)

    if (audioResId != null) {
        try {
            val mediaPlayer = MediaPlayer.create(context, audioResId)
            currentMediaPlayer = mediaPlayer // Salva a referência global

            mediaPlayer.setOnCompletionListener {
                it.release()
                if (currentMediaPlayer == it) currentMediaPlayer = null
                onSpeakStateChange("")
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            falarComTTS(tts, textoBackup, onSpeakStateChange)
        }
    } else {
        falarComTTS(tts, textoBackup, onSpeakStateChange)
    }
}

private fun falarComTTS(
    tts: TextToSpeech?,
    texto: String,
    onSpeakStateChange: (String) -> Unit
) {
    val utteranceId = UUID.randomUUID().toString()

    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            onSpeakStateChange("")
        }

        override fun onError(utteranceId: String?) {
            onSpeakStateChange("")
        }
    })

    tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
}

@Composable
fun CameraScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var aiSpokenText by remember { mutableStateOf("") }
    var isCameraPaused by remember { mutableStateOf(false) }
    var detectedFaceId by remember { mutableStateOf<Int?>(null) }

    var currentAppState by remember { mutableStateOf<VrAppState>(VrAppState.NFC_READING) }

    var isNetworkingOpen by remember { mutableStateOf(false) }
    var isTasksOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var anchorYaw by remember { mutableFloatStateOf(0f) }
    var anchorPitch by remember { mutableFloatStateOf(0f) }

    var initialPitch by remember { mutableFloatStateOf(Float.NaN) }
    var lastYaw by remember { mutableFloatStateOf(Float.NaN) }
    var accumulatedYaw by remember { mutableFloatStateOf(0f) }

    var rawSensorYaw by remember { mutableFloatStateOf(0f) }
    var rawSensorPitch by remember { mutableFloatStateOf(0f) }

    var cursorOffsetNormalizado by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isClicking by remember { mutableStateOf(false) }
    var wasClicking by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidth = remember(configuration) { with(density) { configuration.screenWidthDp.dp.toPx() } }
    val screenHeight = remember(configuration) { with(density) { configuration.screenHeightDp.dp.toPx() } }

    val smoothYaw by animateFloatAsState(
        targetValue = rawSensorYaw,
        animationSpec = tween(150),
        label = "smoothYaw"
    )
    val smoothPitch by animateFloatAsState(
        targetValue = rawSensorPitch,
        animationSpec = tween(150),
        label = "smoothPitch"
    )

    fun snapWindowsToCenter() {
        if (!isNetworkingOpen && !isTasksOpen && !isSettingsOpen) {
            anchorYaw = rawSensorYaw
            anchorPitch = rawSensorPitch
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
                    event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR
                ) {

                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    val remappedMatrix = FloatArray(9)
                    SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedMatrix
                    )

                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(remappedMatrix, orientationAngles)

                    val currentYawDegrees =
                        Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val currentPitchDegrees =
                        Math.toDegrees(orientationAngles[1].toDouble()).toFloat()

                    if (lastYaw.isNaN()) {
                        lastYaw = currentYawDegrees
                        initialPitch = currentPitchDegrees
                        anchorYaw = currentYawDegrees
                        anchorPitch = 0f
                    }

                    var yawDiff = currentYawDegrees - lastYaw
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

        rotationSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.init(context)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                val voices = tts?.voices
                val maleVoice = voices?.find {
                    it.name.lowercase().contains("male") || it.name.lowercase()
                        .contains("pt-br-x-afs-local")
                }
                if (maleVoice != null) tts?.voice = maleVoice

                val biometricManager = BiometricManager.from(context)
                when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> falar(
                        context,
                        tts,
                        null,
                        "Brás informando: Sem sensores de Face ID."
                    ) { aiSpokenText = it }

                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> falar(
                        context,
                        tts,
                        null,
                        "Configure sua biometria."
                    ) { aiSpokenText = it }

                    BiometricManager.BIOMETRIC_SUCCESS -> falar(
                        context,
                        tts,
                        audioMap["validando_dados"],
                        "Sistemas prontos."
                    ) { aiSpokenText = it }
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
            hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
        }

    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    val networkingBitmap = rememberSafeBitmap(R.drawable.networking_screen, targetWidth = 800)
    val tasksBitmap = rememberSafeBitmap(R.drawable.tarefas_screen, targetWidth = 800)

    DisposableEffect(hasAudioPermission) {
        if (!hasAudioPermission) return@DisposableEffect onDispose {}
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        }

        val listener = object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                val text = matches.joinToString(" ").lowercase()

                if (text.contains("brás") || text.contains("braz")) {
                    when {
                        text.contains("networking") || text.contains("bate-papo") || text.contains(" abrir bate-papo") || text.contains(
                            "Abrir networking"
                        ) -> {
                            snapWindowsToCenter()
                            isNetworkingOpen = true
                            falar(
                                context,
                                tts,
                                audioMap["sessao_networking"],
                                "Abrindo sessão de networking."
                            ) { aiSpokenText = it }
                        }

                        text.contains("diagnóstico") || text.contains("status") || text.contains("iniciar diagnóstico") -> {
                            falar(
                                context,
                                tts,
                                audioMap["executando_diagnostico"],
                                "Executando diagnóstico do sistema."
                            ) { aiSpokenText = it }
                            scope.launch {
                                delay(3000)
                                falar(
                                    context,
                                    tts,
                                    audioMap["resolvendo_eficiencia"],
                                    "Resolvido com eficiência."
                                ) { aiSpokenText = it }
                            }
                        }

                        text.contains("atualizar") || text.contains("refresh") -> {
                            falar(
                                context,
                                tts,
                                audioMap["atualizando_realtime"],
                                "Atualizando informações em tempo real."
                            ) { aiSpokenText = it }
                        }

                        text.contains("tarefa") || text.contains("tarefas") || text.contains("abrir tarefas") || text.contains(
                            "abrir tarefa"
                        ) || text.contains("agenda") || text.contains("working") || text.contains("work") -> {
                            snapWindowsToCenter()
                            isTasksOpen = true
                            falar(
                                context,
                                tts,
                                audioMap["exibindo_tarefas"],
                                "Exibindo tarefas."
                            ) { aiSpokenText = it }
                        }

                        text.contains("fechar") || text.contains("sair") || text.contains("ocultar") -> {
                            isNetworkingOpen = false
                            isTasksOpen = false
                            isSettingsOpen = false
                            falar(context, tts, null, "Minimizando interfaces.") {
                                aiSpokenText = it
                            }
                        }

                        text.contains("configurações") || text.contains("abrir configurações") || text.contains(
                            "ajustes"
                        ) || text.contains("abrir ajustes") || text.contains("sistema") || text.contains(
                            "abrir sistema"
                        ) -> {
                            snapWindowsToCenter()
                            isSettingsOpen = true
                            falar(
                                context,
                                tts,
                                audioMap["recursos_tela"],
                                "Abrindo painel de configurações."
                            ) { aiSpokenText = it }
                        }

                        else -> falar(
                            context,
                            tts,
                            null,
                            "Comando não reconhecido."
                        ) { aiSpokenText = it }
                    }
                }
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
                onEyeDetectedState = { id -> detectedFaceId = id },
                onHandTrackingUpdate = { offset, clicando ->
                    // 1. DESFAZENDO A INVERSÃO: O mouse volta a seguir sua mão normalmente!
                    cursorOffsetNormalizado = androidx.compose.ui.geometry.Offset(offset.x, offset.y)

                    if (clicando && !wasClicking) {
                        val x = cursorOffsetNormalizado.x
                        val y = cursorOffsetNormalizado.y

                        when {
                            // ZONA TAREFAS (Logo após a logo)
                            x in 0.12f..0.25f -> {
                                isTasksOpen = !isTasksOpen
                                if (isTasksOpen) {
                                    falar(context, tts, audioMap["exibindo_tarefas"], "Exibindo tarefas") { aiSpokenText = it }
                                }
                            }

                            // ZONA NETWORKING (Meio do menu)
                            x in 0.25f..0.38f -> {
                                isNetworkingOpen = !isNetworkingOpen
                                if (isNetworkingOpen) {
                                    falar(context, tts, audioMap["sessao_networking"], "Abrindo networking") { aiSpokenText = it }
                                }
                            }

                            // ZONA CONFIGURAÇÕES (Final do menu)
                            x in 0.38f..0.55f -> {
                                isSettingsOpen = !isSettingsOpen
                                if (isSettingsOpen) {
                                    falar(context, tts, audioMap["recursos_tela"], "Abrindo configurações") { aiSpokenText = it }
                                }
                            }
                        }
                    }

                    isClicking = clicando
                    wasClicking = clicando
                }
            )
        }

        when (currentAppState) {
            VrAppState.NFC_READING -> {
                NfcReadingOverlay()

                LaunchedEffect(Unit) {
                    delay(3000)
                    currentAppState = VrAppState.AUTHENTICATING
                }
            }

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
                                    ) { aiSpokenText = it }
                                    currentAppState = VrAppState.WELCOME
                                },
                                onError = {
                                    isCameraPaused = false
                                    falar(
                                        context,
                                        tts,
                                        audioMap["erro_inconsistencia"],
                                        "Falha na identificação."
                                    ) { aiSpokenText = it }
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 25.dp, vertical = 40.dp)
                ) {

                    // 1. TELAS FLUTUANTES (Tarefas, Networking, Config)
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 150.dp)
                            .graphicsLayer {
                                var diffYaw = smoothYaw - anchorYaw
                                if (diffYaw > 180f) diffYaw -= 360f
                                if (diffYaw < -180f) diffYaw += 360f
                                translationX = -(diffYaw * 20f).coerceIn(-500f, 500f) // Limita a fuga da tela
                                translationY = -((smoothPitch - anchorPitch) * 15f).coerceIn(-300f, 300f)
                            },
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TELA DE NETWORKING
                        AnimatedVisibility(
                            visible = isNetworkingOpen,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                        ) {
                            if (networkingBitmap != null) {
                                Image(
                                    bitmap = networkingBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(350.dp).clickable { isNetworkingOpen = false }
                                )
                            } else {
                                Box(modifier = Modifier.size(350.dp).background(Color.Blue.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                                    Text("Networking: Imagem não carregou", color = Color.White)
                                }
                            }
                        }

                        // TELA DE TAREFAS
                        AnimatedVisibility(
                            visible = isTasksOpen,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                        ) {
                            if (tasksBitmap != null) {
                                Image(
                                    bitmap = tasksBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(400.dp).clickable { isTasksOpen = false }
                                )
                            } else {
                                Box(modifier = Modifier.size(400.dp).background(Color.Red.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                                    Text("Tarefas: Imagem não carregou", color = Color.White)
                                }
                            }
                        }

                        // TELA DE CONFIGURAÇÕES
                        AnimatedVisibility(
                            visible = isSettingsOpen,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                        ) {
                            Box(modifier = Modifier.width(350.dp)) {
                                SettingsOverlay(
                                    onClose = {
                                        isSettingsOpen = false
                                        falar(context, tts, null, "Minimizando configurações.") { aiSpokenText = it }
                                    },
                                    onResetBiometrics = {
                                        falar(context, tts, audioMap["procedimento_padrao"], "Iniciando recalibração biométrica.") { aiSpokenText = it }
                                        currentAppState = VrAppState.AUTHENTICATING
                                    }
                                )
                            }
                        }
                    }

                    // 2. ASSISTENTE BRÁS E BALÃO DE FALA
                    // Fica um pouco mais acima (bottom = 120.dp) para não cobrir o MenuHome
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 120.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Box(
                                modifier = Modifier.size(75.dp).clip(CircleShape).border(3.dp, Color(0xFF00E5FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (aiSpokenText.isEmpty()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.bras_static),
                                        contentDescription = "Brás Ocioso",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().padding(3.dp).background(Color.White)
                                    )
                                } else {
                                    AiAssistantSprite(texto = aiSpokenText, modifier = Modifier.fillMaxSize())
                                }
                            }

                            AnimatedVisibility(
                                visible = aiSpokenText.isNotEmpty(),
                                enter = expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp),
                                    color = Color.White.copy(alpha = 0.95f),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 10.dp).widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Assistente Brás", style = TextStyle(color = Color(0xFF008542), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(aiSpokenText, style = TextStyle(color = Color.DarkGray, fontSize = 15.sp, fontWeight = FontWeight.Medium))
                                    }
                                }
                            }
                        }
                    }

                    // 3. MENU HOME (Fica exatamente no canto inferior esquerdo)
                    Box(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        MenuHome(
                            isNetworkingOpen = isNetworkingOpen,
                            isTasksOpen = isTasksOpen,
                            isSettingsOpen = isSettingsOpen,
                            onNavigate = { abaClicada ->
                                snapWindowsToCenter()
                                // Como abaClicada já é do tipo VrAppState, o when fica muito mais limpo!
                                when (abaClicada) {
                                    VrAppState.NETWORKING -> isNetworkingOpen = !isNetworkingOpen
                                    VrAppState.TASKS -> isTasksOpen = !isTasksOpen
                                    VrAppState.SETTINGS -> isSettingsOpen = !isSettingsOpen
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }

        if (currentAppState != VrAppState.AUTHENTICATING && currentAppState != VrAppState.NFC_READING) {
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            x = (cursorOffsetNormalizado.x * screenWidth).toInt(),
                            y = (cursorOffsetNormalizado.y * screenHeight).toInt()
                        )
                    }
                    .size(30.dp)
                    .background(
                        color = if (isClicking) Color.Green else Color.Cyan,
                        shape = CircleShape
                    )
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    } // Fim da Box Principal
} // Fim da função CameraScreen