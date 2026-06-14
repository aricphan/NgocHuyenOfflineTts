package com.aricphan.offlinetts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.GenerationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var speakJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TtsScreen(
                    initEngine = ::initEngineIfNeeded,
                    speak = ::speakText,
                    stop = ::stopPlayback
                )
            }
        }
    }

    private fun initEngineIfNeeded(): String {
        if (tts != null) return "Sẵn sàng"

        val threadCount = max(1, Runtime.getRuntime().availableProcessors() / 2)
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "voice/ngoc-huyen.onnx",
                    tokens = "voice/tokens.txt",
                    dataDir = "voice/espeak-ng-data",
                    noiseScale = 0.667f,
                    noiseScaleW = 0.8f,
                    lengthScale = 1.0f
                ),
                numThreads = threadCount,
                debug = false,
                provider = "cpu"
            ),
            maxNumSentences = 1,
            silenceScale = 0.2f
        )

        tts = OfflineTts(assetManager = assets, config = config)
        return "Đã tải model Ngọc Huyền - offline"
    }

    private suspend fun speakText(text: String, speed: Float, onStatus: (String) -> Unit) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) {
            onStatus("Bạn chưa nhập nội dung")
            return
        }

        stopPlayback()
        onStatus("Đang khởi tạo model...")

        speakJob = kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            try {
                val audio = withContext(Dispatchers.Default) {
                    initEngineIfNeeded()
                    onStatus("Đang tạo giọng đọc offline...")
                    tts!!.generateWithConfig(
                        text = cleaned,
                        config = GenerationConfig(
                            speed = speed,
                            sid = 0,
                            silenceScale = 0.2f
                        )
                    )
                }

                onStatus("Đang phát audio...")
                playFloatAudio(audio.samples, audio.sampleRate)
                onStatus("Đã đọc xong")
            } catch (e: Exception) {
                onStatus("Lỗi: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun playFloatAudio(samples: FloatArray, sampleRate: Int) {
        val pcm = ShortArray(samples.size)
        for (i in samples.indices) {
            val v = samples[i].coerceIn(-1.0f, 1.0f)
            pcm[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBufferSize)
            .build()

        audioTrack?.play()
        audioTrack?.write(pcm, 0, pcm.size)
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    private fun stopPlayback() {
        speakJob?.cancel()
        speakJob = null
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
    }

    override fun onDestroy() {
        stopPlayback()
        tts?.release()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun TtsScreen(
    initEngine: () -> String,
    speak: suspend (String, Float, (String) -> Unit) -> Unit,
    stop: () -> Unit
) {
    var text by remember {
        mutableStateOf("Xin chào, đây là ứng dụng đọc tiếng Việt offline bằng giọng Ngọc Huyền.")
    }
    var status by remember { mutableStateOf("Đang chờ") }
    var speed by remember { mutableFloatStateOf(1.0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        status = withContext(Dispatchers.Default) { initEngine() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Offline Ngọc Huyền TTS", style = MaterialTheme.typography.headlineSmall)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            label = { Text("Nhập nội dung cần đọc") }
        )

        Text("Tốc độ: ${"%.2f".format(speed)}x")
        Slider(
            value = speed,
            onValueChange = { speed = it },
            valueRange = 0.7f..1.4f
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                scope.launch {
                    speak(text, speed) { newStatus -> status = newStatus }
                }
            }) {
                Text("Đọc")
            }

            Button(onClick = {
                stop()
                status = "Đã dừng"
            }) {
                Text("Dừng")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Ghi chú: Lần build đầu tiên sẽ tự tải assets từ repo onnxvoice rồi nhúng vào APK. Sau khi cài, app đọc offline.")
    }
}
