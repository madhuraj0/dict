package com.madhuraj.dict.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.madhuraj.dict.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val cacheDir = File(context.cacheDir, "audio_cache").apply { mkdirs() }
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
            }
        }
    }

    fun playWord(word: String, audioUrl: String? = null) {
        val mode = AppSettings.getAudioMode(context)

        // If user configured TTS only or offline, play TTS directly
        if (mode == "tts") {
            playTts(word)
            return
        }

        // Try playing human voice (either from URL or cached file or commons URL)
        val cleanWord = word.trim().lowercase()
        val candidateUrl = audioUrl ?: "https://commons.wikimedia.org/wiki/Special:FilePath/En-us-$cleanWord.ogg"
        val cachedFile = File(cacheDir, "$cleanWord.ogg")

        if (cachedFile.exists() && cachedFile.length() > 0) {
            playLocalFile(cachedFile) {
                // fallback to TTS if local file corrupt
                playTts(word)
            }
            return
        }

        if (!isNetworkAvailable()) {
            playTts(word)
            return
        }

        // Fetch & play online stream
        scope.launch {
            try {
                val tempDownloaded = downloadAudio(candidateUrl, cachedFile)
                if (tempDownloaded != null && tempDownloaded.exists() && tempDownloaded.length() > 1024) {
                    withContext(Dispatchers.Main) {
                        playLocalFile(tempDownloaded) {
                            playTts(word)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        playTts(word)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    playTts(word)
                }
            }
        }
    }

    private fun playLocalFile(file: File, onError: () -> Unit) {
        try {
            stopCurrent()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    onError()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            onError()
        }
    }

    private suspend fun downloadAudio(urlStr: String, targetFile: File): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 4000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "DictApp/1.0 (Android; offline dictionary)")
            }
            conn.connect()
            if (conn.responseCode in 200..299) {
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.length() > 1024) {
                    tempFile.renameTo(targetFile)
                    return@withContext targetFile
                } else {
                    tempFile.delete()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun playTts(word: String) {
        stopCurrent()
        if (isTtsReady) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "dict_${System.currentTimeMillis()}")
        }
    }

    private fun stopCurrent() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun release() {
        stopCurrent()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
