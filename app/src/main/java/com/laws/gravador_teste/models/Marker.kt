package com.laws.gravador_teste.models

import com.laws.gravador_teste.Speaker

data class Marker(
    val id: Long = System.currentTimeMillis(),
    var startTime: Long,
    var endTime: Long? = null,
    var transcription: String,
    var translation: String = "",
    var transcriptionAudioPath: String? = null,
    var translationAudioPath: String? = null,
    var speaker: Speaker? = null
) {
    fun getFormattedStartTime(): String {
        val seconds = (startTime / 1000) % 60
        val minutes = (startTime / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getFormattedEndTime(): String {
        return endTime?.let { time ->
            val seconds = (time / 1000) % 60
            val minutes = (time / (1000 * 60)) % 60
            String.format("%02d:%02d", minutes, seconds)
        } ?: "--:--"
    }
}