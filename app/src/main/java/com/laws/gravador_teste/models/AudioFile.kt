package com.laws.gravador_teste.models

import com.laws.gravador_teste.Speaker

data class AudioFile(
    val id: Long = System.currentTimeMillis(),
    val fileName: String,
    val filePath: String,
    val duration: Long = 0,
    var speakers: MutableList<Speaker> = mutableListOf(),
    val dateCreated: Long = System.currentTimeMillis()

)