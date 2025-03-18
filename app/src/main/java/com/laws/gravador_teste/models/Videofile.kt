package com.laws.gravador_teste.models

data class VideoFile(
    val id: Long = System.currentTimeMillis(),
    val fileName: String,
    val filePath: String,
    val duration: Long = 0,
    val dateCreated: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val audioExtractPath: String? = null,
    val width: Int = 0,
    val height: Int = 0
)