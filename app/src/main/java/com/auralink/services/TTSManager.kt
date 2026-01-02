package com.auralink.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import android.media.AudioAttributes
import android.os.Bundle

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onCompletion: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onCompletion?.invoke()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onCompletion?.invoke()
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Language not supported")
            } else {
                isInitialized = true
                
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(attrs)
            }
        } else {
            Log.e("TTSManager", "Initialization failed")
        }
    }

    fun setCompletionListener(listener: () -> Unit) {
        this.onCompletion = listener
    }

    fun speak(text: String) {
        Log.d("TTSManager", "Requesting speak: $text")
        if (isInitialized) {
            val params = Bundle()
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_VOICE_CALL)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "CallAnnouncement")
        } else {
            Log.e("TTSManager", "TTS not initialized")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
