package com.laws.gravador_teste

//import com.laws.gravador_teste.utils.ExportManager
//import Marker
import ExportManager
import Project
import com.laws.gravador_teste.adapters.MarkerAdapter
import com.laws.gravador_teste.utils.ProjectManager
//import Project
import com.laws.gravador_teste.models.VideoFile
import com.laws.gravador_teste.utils.VideoManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import com.laws.gravador_teste.models.Marker
//import com.laws.gravador_teste.models.Project

class VideoPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var videoManager: VideoManager
    private var currentVideoFile: VideoFile? = null
    private val markers = mutableListOf<Marker>()
    private lateinit var markerAdapter: MarkerAdapter
    private var currentMarker: Marker? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentProject: Project? = null
    private lateinit var projectManager: ProjectManager


    private lateinit var waveformView: WaveformView
    private lateinit var currentTimeTextView: TextView
    private lateinit var totalTimeTextView: TextView
    private lateinit var transcriptionEditText: EditText
    private lateinit var translationEditText: EditText
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            loadVideo(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        // Inicializar ProjectManager
        projectManager = ProjectManager(this)

        // Carregar projeto existente, se houver
        val currentProjectId = projectManager.getCurrentProjectId()
        if (currentProjectId != -1L) {
            currentProject = projectManager.loadProject(currentProjectId)
        }

        setupViews()
        setupVideoPlayer()

        // Verificar se há um URI de vídeo na intent
        if (intent.data != null) {
            loadVideo(intent.data!!)
        } else {
            // Se não houver URI, abrir o seletor de vídeo automaticamente
            // Mas com um pequeno atraso para garantir que tudo esteja inicializado
            Handler(Looper.getMainLooper()).postDelayed({
                pickVideo()
            }, 300) // 300ms de atraso
        }
    }

    private fun setupViews() {
        playerView = findViewById(R.id.videoView)
        waveformView = findViewById(R.id.waveformView)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        totalTimeTextView = findViewById(R.id.totalTimeTextView)
        transcriptionEditText = findViewById(R.id.transcriptionEditText)
        translationEditText = findViewById(R.id.translationEditText)

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.markersRecyclerView)
        markerAdapter = MarkerAdapter(
            markers = markers,
            onMarkerClick = { time -> seekToTime(time) },
            onTranscriptionChanged = { position, newText ->
                markers[position].transcription = newText
                markerAdapter.notifyItemChanged(position)
            },
            onTranslationChanged = { position, newText ->
                markers[position].translation = newText
                markerAdapter.notifyItemChanged(position)
            },
            onDeleteMarker = { position ->
                markers.removeAt(position)
                markerAdapter.notifyItemRemoved(position)
            },
            onPlaySegment = { marker, shouldLoop -> playSegment(marker, shouldLoop) },
            onRecordTranscription = { position ->  },
            onRecordTranslation = { position ->  },
            onSpeakerSelect = { position -> showSelectSpeakerForMarkerDialog(position) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = markerAdapter

        // Setup botões
        findViewById<Button>(R.id.playButton).setOnClickListener { player?.play() }
        findViewById<Button>(R.id.pauseButton).setOnClickListener { player?.pause() }
        findViewById<Button>(R.id.addMarkerButton).setOnClickListener { addMarker() }
    }

    private fun setupVideoPlayer() {
        videoManager = VideoManager(this)
        player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            updateTotalTime()
                            startProgressUpdate()
                        }
                    }
                }
            })
        }
        playerView.player = player
    }

    private fun showSelectSpeakerForMarkerDialog(position: Int) {
        try {
            // Verificar se tem um projeto com falantes
            if (currentProject?.speakers?.isEmpty() != false) {
                Toast.makeText(this, "Não há falantes cadastrados. Adicione falantes primeiro.", Toast.LENGTH_SHORT).show()
                return
            }

            // Obter o marcador e o falante atual
            val marker = markers[position]
            val currentSpeaker = marker.speaker

            // Criar a lista de nomes de falantes + opção "Nenhum"
            val speakerNames = mutableListOf("Nenhum falante")
            speakerNames.addAll(currentProject?.speakers?.map { it.name } ?: emptyList())

            // Determinar qual falante está selecionado atualmente
            val currentSelection = if (currentSpeaker == null) {
                0
            } else {
                val index = currentProject?.speakers?.indexOfFirst { it.id == currentSpeaker.id } ?: -1
                if (index == -1) 0 else index + 1
            }

            // Mostrar diálogo de seleção
            AlertDialog.Builder(this)
                .setTitle("Selecionar Falante para o Marcador")
                .setSingleChoiceItems(speakerNames.toTypedArray(), currentSelection) { dialog, which ->
                    // Obter o falante selecionado (null se "Nenhum" foi selecionado)
                    val selectedSpeaker = if (which == 0) null else currentProject?.speakers?.get(which - 1)

                    // Atualizar o marcador
                    marker.speaker = selectedSpeaker
                    markerAdapter.notifyItemChanged(position)

                    // Atualizar o projeto com os novos dados
                    currentProject?.let { project ->
                        project.markers = markers

                    }

                    Toast.makeText(this, "Falante atualizado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } catch (e: Exception) {
            Log.e("SpeakerDebug", "Erro ao mostrar diálogo de seleção de falante", e)
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VIDEO_PICK_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                loadVideo(uri)
            } ?: Toast.makeText(this, "Erro ao obter o vídeo selecionado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVideo(uri: Uri) {
        try {
            val videoFile = copyVideoToInternalStorage(uri)
            currentVideoFile = videoManager.getVideoMetadata(videoFile.absolutePath)

            if (currentProject == null) {
                currentProject = Project(
                    name = videoFile.name.substringBeforeLast("."),
                    dateCreated = System.currentTimeMillis()
                )
                projectManager.saveProject(currentProject!!)
                projectManager.saveCurrentProjectId(currentProject!!.id)
            }

            // Configura o player
            val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
            player?.setMediaItem(mediaItem)
            player?.prepare()

            // Extrai o áudio
            extractAudioAndUpdateWaveform(videoFile)

        } catch (e: Exception) {
            Log.e("VideoPlayer", "Erro ao carregar vídeo: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar vídeo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractAudioAndUpdateWaveform(videoFile: File) {
        val outputFile = File(getExternalFilesDir(null),
            "audio_${System.currentTimeMillis()}.aac")

        if (videoManager.extractAudioFromVideo(videoFile.absolutePath, outputFile.absolutePath)) {
            updateWaveformView(outputFile)
        }
    }

    private fun updateWaveformView(audioFile: File) {
        val audioData = ByteArray(audioFile.length().toInt())
        audioFile.inputStream().use { it.read(audioData) }
        waveformView.setAudioData(audioData)

        waveformView.onProgressChanged = { progress ->
            player?.seekTo((player?.duration?.times(progress))?.toLong() ?: 0)
        }
    }

    private fun addMarker() {
        player?.currentPosition?.let { currentTime ->
            if (currentMarker == null) {
                // Iniciando novo marcador
                currentMarker = Marker(
                    startTime = currentTime,
                    transcription = transcriptionEditText.text.toString(),
                    translation = translationEditText.text.toString()
                )
                findViewById<Button>(R.id.addMarkerButton).text = "Finalizar Marcador"

                // Desabilita campos de edição
                transcriptionEditText.isEnabled = false
                translationEditText.isEnabled = false
            } else {
                // Finalizando marcador
                currentMarker?.let { marker ->
                    if (currentTime > marker.startTime) {
                        marker.endTime = currentTime
                        markers.add(marker)
                        markerAdapter.notifyItemInserted(markers.size - 1)

                        // Limpa e reabilita campos
                        transcriptionEditText.setText("")
                        translationEditText.setText("")
                        transcriptionEditText.isEnabled = true
                        translationEditText.isEnabled = true

                        currentMarker = null
                        findViewById<Button>(R.id.addMarkerButton).text = "Adicionar Marcador"
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        // Verificar permissão de leitura de armazenamento
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Verificar permissão de escrita de armazenamento (para Android < 10)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // Se faltar alguma permissão, solicitar
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                STORAGE_PERMISSION_CODE
            )
            return false
        }

        return true
    }

    private fun playSegment(marker: Marker, shouldLoop: Boolean) {
        player?.apply {
            seekTo(marker.startTime)
            play()

            if (shouldLoop) {
                handler.post(object : Runnable {
                    override fun run() {
                        if (currentPosition >= (marker.endTime ?: duration)) {
                            seekTo(marker.startTime)
                        }
                        handler.postDelayed(this, 50)
                    }
                })
            }
        }
    }

    private fun seekToTime(time: Long) {
        player?.seekTo(time)
    }

    private fun startProgressUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                player?.let {
                    if (it.isPlaying) {
                        updateProgress()
                        handler.postDelayed(this, 50)
                    }
                }
            }
        })
    }

    private fun updateProgress() {
        player?.let {
            val position = it.currentPosition
            val duration = it.duration
            val progress = if (duration > 0) position.toFloat() / duration else 0f

            waveformView.setProgress(progress)
            currentTimeTextView.text = formatTime(position)
        }
    }

    private fun updateTotalTime() {
        player?.let {
            totalTimeTextView.text = formatTime(it.duration)
        }
    }

    private fun formatTime(timeMs: Long): String {
        val seconds = (timeMs / 1000) % 60
        val minutes = (timeMs / (1000 * 60)) % 60
        val hours = (timeMs / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun copyVideoToInternalStorage(uri: Uri): File {
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val file = File(getExternalFilesDir(null), fileName)

        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_project -> {
                showNewProjectDialog()
                true
            }
            R.id.open_project -> {
                showRecordingsDialog()
                true
            }
            R.id.export_csv -> {
                exportData("csv")
                true
            }
            R.id.export_elan -> {
                exportData("eaf")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNewProjectDialog() {
        val options = arrayOf("Carregar Áudio Existente", "Carregar Vídeo")

        AlertDialog.Builder(this)
            .setTitle("Novo Projeto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    }
                    1 -> pickVideo()
                }
            }
            .show()
    }

    private fun exportData(format: String) {
        if (checkStoragePermission()) {
            val exportManager = ExportManager(this)

            currentProject?.let { project ->
                // Atualiza os marcadores do projeto antes de exportar
                project.markers = markers

                val success = when (format) {
                    "csv" -> exportManager.exportToCsv(project)
                    "eaf" -> exportManager.exportToElan(project, currentVideoFile?.filePath ?: "")
                    else -> false
                }

                val message = if (success) {
                    "Arquivo exportado com sucesso para Downloads"
                } else {
                    "Erro ao exportar arquivo"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "Nenhum projeto ativo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRecordingsDialog() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
            return false
        }
        return true
    }

    private fun pickVideo() {
        try {
            pickVideoLauncher.launch("video/*")
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Erro ao abrir seletor de vídeo: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir seletor: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
        private const val VIDEO_PICK_REQUEST = 1003
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        handler.removeCallbacksAndMessages(null)
    }
}



