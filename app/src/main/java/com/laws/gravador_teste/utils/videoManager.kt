package com.laws.gravador_teste.utils

import com.laws.gravador_teste.models.VideoFile
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

class VideoManager(private val context: Context) {

    fun extractAudioFromVideo(videoPath: String, outputPath: String): Boolean {
        return try {
            // Comando FFmpeg para extrair áudio
            val command = "-i $videoPath -vn -acodec aac -b:a 192k $outputPath"
            val session = FFmpegKit.execute(command)

            // Verifica se a execução foi bem sucedida
            ReturnCode.isSuccess(session.returnCode)
        } catch (e: Exception) {
            Log.e("VideoManager", "Erro ao extrair áudio: ${e.message}")
            false
        }
    }

    fun getVideoMetadata(videoPath: String): VideoFile? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0

            val fileName = File(videoPath).name

            VideoFile(
                fileName = fileName,
                filePath = videoPath,
                duration = duration,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            Log.e("VideoManager", "Erro ao obter metadados: ${e.message}")
            null
        } finally {
            retriever.release()
        }
    }

    fun generateThumbnail(videoPath: String, outputPath: String): Boolean {
        return try {
            // Comando FFmpeg para extrair um frame como thumbnail
            val command = "-i $videoPath -ss 00:00:01 -frames:v 1 $outputPath"
            val session = FFmpegKit.execute(command)

            ReturnCode.isSuccess(session.returnCode)
        } catch (e: Exception) {
            Log.e("VideoManager", "Erro ao gerar thumbnail: ${e.message}")
            false
        }
    }

    fun isVideoFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }
}