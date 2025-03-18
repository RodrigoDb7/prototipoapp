package com.laws.gravador_teste




import com.laws.gravador_teste.models.AudioFile
import com.laws.gravador_teste.models.Marker
import com.laws.gravador_teste.adapters.AudioFileAdapter
import com.laws.gravador_teste.adapters.MarkerAdapter
import com.laws.gravador_teste.utils.ProjectManager
import ExportManager
import Project
import SpeakerAdapter
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner

import com.google.android.material.textfield.TextInputEditText
import com.laws.gravador_teste.adapters.SelectSpeakersAdapter


import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var currentProject: Project? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var transcriptionRecorder: MediaRecorder? = null
    private var translationRecorder: MediaRecorder? = null
    private var markerRecordingTimer: Timer? = null
    private lateinit var audioFilePath: String
    private val markers = mutableListOf<Marker>()
    private lateinit var markerAdapter: MarkerAdapter
    private lateinit var projectManager: ProjectManager

    private lateinit var speakerAdapter: SpeakerAdapter

    private lateinit var waveformView: WaveformView



    private val audioFiles = mutableListOf<AudioFile>()
    private lateinit var audioFileAdapter: AudioFileAdapter
    private var currentAudioFile: AudioFile? = null


    private var isRecording = false

    private var startRecordingTime: Long = 0
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())



    //private lateinit var seekBar: SeekBar
    private lateinit var currentTimeTextView: TextView
    private lateinit var totalTimeTextView: TextView
    private lateinit var timerTextView: TextView



    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
        private const val PICK_AUDIO_FILE = 1002
        private const val VIDEO_PICK_REQUEST = 1003
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
            R.id.manage_speakers -> {
                showSpeakersDialog()
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
            R.id.export_xml -> {  // Nova opção
                exportData("xml")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissões concedidas, tenta iniciar a gravação novamente

            } else {
                Toast.makeText(this, "Permissões necessárias para gravação", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun loadAudioFromUri(uri: Uri) {
        try {
            val fileName = getFileName(uri)
            val file = copyFileToInternalStorage(uri, fileName)

            audioFilePath = file.absolutePath
            val audioFile = AudioFile(
                fileName = fileName,
                filePath = audioFilePath
            )

            // Adiciona à lista de áudios
            audioFiles.add(audioFile)
            currentAudioFile = audioFile

            // Limpa marcadores anteriores
            markers.clear()
            markerAdapter.notifyDataSetChanged()

            // Mostra os controles
            findViewById<LinearLayout>(R.id.recordingControls).visibility = View.VISIBLE

            // Configurar interface para reprodução

            findViewById<Button>(R.id.playButton).visibility = View.VISIBLE
            findViewById<Button>(R.id.pauseButton).visibility = View.VISIBLE

            // Reseta os campos de texto
            findViewById<EditText>(R.id.transcriptionEditText).setText("")
            findViewById<EditText>(R.id.translationEditText).setText("")

            // Atualiza interface
            updateWaveform(file)

            // Verificar se tem um projeto, caso contrário cria um
            if (currentProject == null) {
                currentProject = Project(
                    name = fileName.substringBeforeLast("."),
                    dateCreated = System.currentTimeMillis()
                )
                currentProject?.let { project ->
                    projectManager.saveProject(project)
                    projectManager.saveCurrentProjectId(project.id)
                }
            }

            // Prepara o MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepare()
                totalTimeTextView.text = formatTime(duration.toLong())
            }

            Toast.makeText(this, "Áudio carregado com sucesso", Toast.LENGTH_SHORT).show()
            Log.d("AudioLoad", "Áudio carregado: $audioFilePath")



        } catch (e: Exception) {
            Log.e("AudioLoad", "Erro ao carregar áudio: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar áudio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result ?: "audio_${System.currentTimeMillis()}.mp3"
    }




    private fun copyFileToInternalStorage(uri: Uri, fileName: String): File {
        val file = File(getExternalFilesDir(null), fileName)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }


    private fun showRecordingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_recordings_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recordingsRecyclerView)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Gravações")
            .setView(dialogView)
            .setPositiveButton("Fechar", null)
            .create()

        dialog.show()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AudioFileAdapter(
            audioFiles,
            onItemClick = { audioFile ->
                currentAudioFile = audioFile
                loadAudioFile(audioFile)
                dialog.dismiss()
            },
            onDeleteClick = { audioFile ->
                deleteAudioFile(audioFile)
            }
        )


    }

    private fun showSelectSpeakersForAudioDialog(audioFile: AudioFile) {
        // Verificar se temos um projeto atual

        try {
            Log.d("SpeakerDebug", "Iniciando diálogo de seleção de falantes")
        if (currentProject == null || currentProject?.speakers?.isEmpty() == true) {
            // Se não houver falantes, mostrar mensagem e oferecer criação
            showNoSpeakersDialog(audioFile)
            return
        }

        // Criar diálogo
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_speakers_for_audio, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.speakersRecyclerView)
        val addNewSpeakerButton = dialogView.findViewById<Button>(R.id.addNewSpeakerButton)

        // Configurar adaptador com falantes do projeto atual
        val selectedSpeakers = mutableSetOf<Speaker>()

        // Adicionar os falantes já associados ao arquivo, se houver
        selectedSpeakers.addAll(audioFile.speakers)

        val adapter = SelectSpeakersAdapter(
            speakers = currentProject?.speakers ?: listOf(),
            selectedSpeakers = selectedSpeakers
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Criar o diálogo
        val dialog = AlertDialog.Builder(this)
            .setTitle("Associar Falantes")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                // Salvar os falantes selecionados no arquivo de áudio
                audioFile.speakers.clear()
                audioFile.speakers.addAll(adapter.getSelectedSpeakers())

                // Atualizar o projeto
                updateAudioFileInProject(audioFile)

                Toast.makeText(this, "Falantes associados com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        // Configurar botão para adicionar novo falante
        addNewSpeakerButton.setOnClickListener {
            dialog.dismiss()
            showAddSpeakerDialog { newSpeaker ->
                // Ao adicionar um novo falante, reabrir o diálogo de seleção
                showSelectSpeakersForAudioDialog(audioFile)
            }
        }



        dialog.show()

        } catch (e: Exception) {
            Log.e("SpeakerDebug", "Erro ao mostrar diálogo de seleção de falantes", e)
            Toast.makeText(this, "Erro ao mostrar seleção de falantes: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateAudioFileInProject(audioFile: AudioFile) {
        // Encontrar o arquivo na lista e atualizar
        val index = audioFiles.indexOfFirst { it.id == audioFile.id }
        if (index != -1) {
            audioFiles[index] = audioFile
        }

        // Atualizar o currentAudioFile se for o mesmo
        if (currentAudioFile?.id == audioFile.id) {
            currentAudioFile = audioFile
        }

        // Salvar o projeto atualizado
        currentProject?.let { projectManager.saveProject(it) }
    }

    // Função auxiliar para mostrar diálogo quando não há falantes
    private fun showNoSpeakersDialog(audioFile: AudioFile) {
        AlertDialog.Builder(this)
            .setTitle("Nenhum Falante")
            .setMessage("Não há falantes cadastrados. Deseja cadastrar um falante agora?")
            .setPositiveButton("Sim") { _, _ ->
                showAddSpeakerDialog { newSpeaker ->
                    // Ao adicionar um novo falante, mostrar diálogo de seleção
                    showSelectSpeakersForAudioDialog(audioFile)
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun updateWaveform(audioFile: File) {
        val audioData = ByteArray(audioFile.length().toInt())
        FileInputStream(audioFile).use { it.read(audioData) }
        waveformView.setAudioData(audioData)
    }

    private fun showSelectSpeakerForMarkerDialog(position: Int) {
        try {
            Log.d("SpeakerDebug", "showSelectSpeakerForMarkerDialog chamado para posição $position")

            // Verificar se temos um projeto com falantes
            if (currentProject == null) {
                Log.d("SpeakerDebug", "currentProject é nulo")
                Toast.makeText(this, "Nenhum projeto ativo. Crie um projeto primeiro.", Toast.LENGTH_SHORT).show()
                return
            }

            if (currentProject?.speakers?.isEmpty() == true) {
                Log.d("SpeakerDebug", "Nenhum falante cadastrado no projeto")
                Toast.makeText(this, "Não há falantes cadastrados. Adicione falantes primeiro.", Toast.LENGTH_SHORT).show()
                return
            }

            // Obter o marcador e o falante atual
            val marker = markers[position]
            val currentSpeaker = marker.speaker

            Log.d("SpeakerDebug", "Falante atual: ${currentSpeaker?.name ?: "nenhum"}")
            Log.d("SpeakerDebug", "Projeto tem ${currentProject?.speakers?.size ?: 0} falantes")

            // Criar a lista de nomes de falantes + opção "Nenhum"
            val speakerNames = mutableListOf("Nenhum falante")
            speakerNames.addAll(currentProject?.speakers?.map { it.name } ?: emptyList())

            Log.d("SpeakerDebug", "Nomes de falantes disponíveis: ${speakerNames.joinToString()}")

            // Determinar qual falante está selecionado atualmente
            val currentSelection = if (currentSpeaker == null) {
                0
            } else {
                val index = currentProject?.speakers?.indexOfFirst { it.id == currentSpeaker.id } ?: -1
                if (index == -1) 0 else index + 1
            }

            Log.d("SpeakerDebug", "Índice de seleção atual: $currentSelection")

            // Mostrar diálogo de seleção
            AlertDialog.Builder(this)
                .setTitle("Selecionar Falante para o Marcador")
                .setSingleChoiceItems(speakerNames.toTypedArray(), currentSelection) { dialog, which ->
                    Log.d("SpeakerDebug", "Item selecionado: $which (${speakerNames[which]})")

                    // Obter o falante selecionado (null se "Nenhum" foi selecionado)
                    val selectedSpeaker = if (which == 0) null else currentProject?.speakers?.get(which - 1)

                    Log.d("SpeakerDebug", "Falante selecionado: ${selectedSpeaker?.name ?: "nenhum"}")

                    // Atualizar o marcador
                    marker.speaker = selectedSpeaker
                    markerAdapter.notifyItemChanged(position)

                    // Atualizar o projeto com os novos dados
                    currentProject?.let { project ->
                        project.markers = markers
                        projectManager.saveProject(project)
                        Log.d("SpeakerDebug", "Projeto atualizado e salvo")
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




    private fun exportData(format: String) {
        if (checkStoragePermission()) {
            val exportManager = ExportManager(this)

            // Verifica se existe um projeto atual
            currentProject?.let { project ->
                // Atualiza os marcadores do projeto antes de exportar
                project.markers = markers

                val success = when (format) {
                    "csv" -> exportManager.exportToCsv(project)
                    "eaf" -> exportManager.exportToElan(project, audioFilePath)
                    "xml" -> exportManager.exportToXml(project)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("AppDebug", "Iniciando onCreate()")

        // Inicializa views
        waveformView = findViewById(R.id.waveformView)
        audioFilePath = "${externalCacheDir?.absolutePath}/audiorecord.mp3"

        // Inicializa ProjectManager
        projectManager = ProjectManager(this)

        // Carrega o projeto atual
        val currentProjectId = projectManager.getCurrentProjectId()
        Log.d("AppDebug", "ID do projeto atual: $currentProjectId")

        if (currentProjectId != -1L) {
            Log.d("AppDebug", "Carregando projeto...")
            currentProject = projectManager.loadProject(currentProjectId)
            Log.d("AppDebug", "Projeto carregado: ${currentProject?.name}, ID: ${currentProject?.id}")
            Log.d("AppDebug", "Número de falantes: ${currentProject?.speakers?.size ?: 0}")
        } else {
            Log.d("AppDebug", "Nenhum projeto atual definido")
        }

        // Configura o layout inicial
        findViewById<LinearLayout>(R.id.recordingControls).visibility = View.GONE

        // Setup dos componentes
        setupViews()
        setupSeekBar()

        requestAudioPermissions()

        Log.d("AppDebug", "onCreate() concluído")
    }













    private fun showSpeakersDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Gerenciar Falantes")
            .setView(R.layout.dialog_speakers_list)  // CORRIGIDO para usar o layout correto
            .setNegativeButton("Fechar", null)
            .create()

        dialog.show()

        // Configurar botões
        dialog.findViewById<Button>(R.id.addNewSpeakerButton)?.setOnClickListener {
            dialog.dismiss()
            showAddSpeakerDialog()
        }

        dialog.findViewById<Button>(R.id.selectSpeakerButton)?.setOnClickListener {
            dialog.dismiss()
            showSelectSpeakerDialog()
        }
    }

    private fun showAddSpeakerDialog(onSpeakerAdded: (Speaker) -> Unit = {}) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Adicionar Novo Falante")
            .setView(R.layout.dialog_metadata)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        // Configurar campos
        val speakerNameInput = dialog.findViewById<TextInputEditText>(R.id.speakerNameInput)
        val speakerAgeInput = dialog.findViewById<TextInputEditText>(R.id.speakerAgeInput)
        val speakerLanguageInput = dialog.findViewById<TextInputEditText>(R.id.speakerLanguageInput)
        val speakerDialectInput = dialog.findViewById<TextInputEditText>(R.id.speakerDialectInput)
        val speakerGenderSpinner = dialog.findViewById<Spinner>(R.id.speakerGenderSpinner)

        // Configurar spinner de gênero
        ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            speakerGenderSpinner?.adapter = adapter
        }

        // Configurar botão salvar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            // Verificar se o nome está preenchido
            val name = speakerNameInput?.text?.toString() ?: ""
            if (name.isBlank()) {
                Toast.makeText(this, "Nome do falante é obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Criar o objeto Speaker
            val speaker = Speaker(
                name = name,
                age = speakerAgeInput?.text?.toString()?.toIntOrNull(),
                gender = speakerGenderSpinner?.selectedItem?.toString(),
                language = speakerLanguageInput?.text?.toString() ?: "",
                dialect = speakerDialectInput?.text?.toString()
            )

            // Verificar se temos um projeto atual
            if (currentProject == null) {
                currentProject = Project(
                    name = "Projeto ${System.currentTimeMillis()}",
                    dateCreated = System.currentTimeMillis()
                )
            }

            // Adicionar o falante ao projeto
            currentProject?.let { project ->
                project.speakers.add(speaker)

                // Salvar o projeto
                projectManager.saveProject(project)
                projectManager.saveCurrentProjectId(project.id)

                Toast.makeText(this, "Falante adicionado com sucesso", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()

            // Chamar o callback com o novo falante
            onSpeakerAdded(speaker)
        }
    }



    private fun showSelectSpeakerDialog() {
        Log.d("SpeakerDebug", "Iniciando showSelectSpeakerDialog()")

        // Recarregar o projeto atual do armazenamento
        val projectId = projectManager.getCurrentProjectId()
        if (projectId != -1L) {
            Log.d("SpeakerDebug", "Recarregando projeto ID: $projectId")
            currentProject = projectManager.loadProject(projectId)
        } else {
            Log.d("SpeakerDebug", "Nenhum projeto atual definido (ID: -1)")
        }

        Log.d("SpeakerDebug", "Projeto atual: ${currentProject?.name}, ID: ${currentProject?.id}")
        Log.d("SpeakerDebug", "Número de falantes: ${currentProject?.speakers?.size ?: 0}")

        val dialog = AlertDialog.Builder(this)
            .setTitle("Selecionar Falante")
            .setView(R.layout.dialog_select_speakers_for_audio)
            .setNegativeButton("Fechar", null)
            .create()

        dialog.show()

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.speakersRecyclerView)

        // Garantir que temos uma lista (mesmo que vazia)
        val speakersList = currentProject?.speakers ?: mutableListOf()

        Log.d("SpeakerDebug", "Configurando adapter com ${speakersList.size} falantes")

        // Logar cada falante para verificação
        speakersList.forEachIndexed { index, speaker ->
            Log.d("SpeakerDebug", "Falante $index: ${speaker.name}, ID: ${speaker.id}")
        }

        // Configurar adapter
        speakerAdapter = SpeakerAdapter(
            speakers = speakersList,
            onEditClick = { speaker ->
                dialog.dismiss()
                showEditSpeakerDialog(speaker)
            },
            onDeleteClick = { speaker ->
                showDeleteSpeakerConfirmation(speaker)
            }
        )

        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = speakerAdapter

        Log.d("SpeakerDebug", "RecyclerView configurado")
    }

    private fun showEditSpeakerDialog(speaker: Speaker) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Falante")
            .setView(R.layout.dialog_metadata)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        // Configurar campos
        val speakerNameInput = dialog.findViewById<TextInputEditText>(R.id.speakerNameInput)
        val speakerAgeInput = dialog.findViewById<TextInputEditText>(R.id.speakerAgeInput)
        val speakerLanguageInput = dialog.findViewById<TextInputEditText>(R.id.speakerLanguageInput)
        val speakerDialectInput = dialog.findViewById<TextInputEditText>(R.id.speakerDialectInput)
        val speakerGenderSpinner = dialog.findViewById<Spinner>(R.id.speakerGenderSpinner)

        // Preencher campos com dados existentes
        speakerNameInput?.setText(speaker.name)
        speakerAgeInput?.setText(speaker.age?.toString())
        speakerLanguageInput?.setText(speaker.language)
        speakerDialectInput?.setText(speaker.dialect)

        // Configurar spinner de gênero
        ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            speakerGenderSpinner?.adapter = adapter
            speaker.gender?.let { gender ->
                val genderOptions = resources.getStringArray(R.array.gender_options)
                val position = genderOptions.indexOf(gender)
                if (position != -1) speakerGenderSpinner?.setSelection(position)
            }
        }

        // Configurar botão salvar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val updatedSpeaker = Speaker(
                name = speakerNameInput?.text?.toString() ?: "",
                age = speakerAgeInput?.text?.toString()?.toIntOrNull(),
                gender = speakerGenderSpinner?.selectedItem?.toString(),
                language = speakerLanguageInput?.text?.toString() ?: "",
                dialect = speakerDialectInput?.text?.toString()
            )

            // Atualizar o speaker na lista
            val index = currentProject?.speakers?.indexOf(speaker)
            if (index != null && index >= 0) {
                currentProject?.speakers?.set(index, updatedSpeaker)
                speakerAdapter.notifyItemChanged(index)

                // Salvar o projeto
                currentProject?.let { project ->
                    projectManager.saveProject(project)
                    projectManager.saveCurrentProjectId(project.id)
                }

                Toast.makeText(this, "Falante atualizado com sucesso", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
    }








    private fun showDeleteSpeakerConfirmation(speaker: Speaker) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Falante")
            .setMessage("Tem certeza que deseja excluir ${speaker.name}?")
            .setPositiveButton("Excluir") { _, _ ->
                val index = currentProject?.speakers?.indexOf(speaker)
                if (index != null && index >= 0) {
                    currentProject?.speakers?.removeAt(index)
                    speakerAdapter.notifyItemRemoved(index)

                    // Salvar o projeto após remover o falante
                    currentProject?.let { project ->
                        projectManager.saveProject(project)
                        Toast.makeText(this, "Falante removido com sucesso", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }




















    private fun loadAudioFile(audioFile: AudioFile) {
        try {
            audioFilePath = audioFile.filePath

            // Verificar se existe um projeto atual, se não, criar um novo
            if (currentProject == null) {
                currentProject = Project(
                    name = audioFile.fileName.substringBeforeLast("."),
                    dateCreated = audioFile.dateCreated
                )
                currentProject?.let { project ->
                    projectManager.saveProject(project)
                    projectManager.saveCurrentProjectId(project.id)
                }
            } else {
                // Se já existe um projeto, apenas atualizar o audioPath
                // e possivelmente outras propriedades relevantes, sem apagar os falantes
                currentProject?.let { project ->
                    // Aqui você pode atualizar propriedades do projeto se necessário,
                    // mas sem recriar o objeto Project inteiro
                    projectManager.saveProject(project)
                }
            }

            Toast.makeText(this, "Áudio carregado com sucesso", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("AudioLoad", "Erro ao carregar áudio: ${e.message}")
            Toast.makeText(this, "Erro ao carregar áudio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteAudioFile(audioFile: AudioFile) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Gravação")
            .setMessage("Tem certeza que deseja excluir esta gravação?")
            .setPositiveButton("Excluir") { _, _ ->
                val file = File(audioFile.filePath)
                file.delete()

                val index = audioFiles.indexOf(audioFile)
                audioFiles.remove(audioFile)
                audioFileAdapter.notifyItemRemoved(index)

                if (currentAudioFile == audioFile) {
                    currentAudioFile = null
                    // Limpar interface
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }




    private fun setupViews() {
        // Inicializar as views
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        totalTimeTextView = findViewById(R.id.totalTimeTextView)
        timerTextView = findViewById(R.id.timerTextView)

        // Configurar botões


        findViewById<Button>(R.id.associateSpeakersButton).setOnClickListener {
            currentAudioFile?.let { audioFile ->
                showSelectSpeakersForAudioDialog(audioFile)
            } ?: run {
                Toast.makeText(this, "Nenhum arquivo de áudio carregado", Toast.LENGTH_SHORT).show()
            }
        }

// Depois de carregar um áudio ou gravar, torne o botão visível
        findViewById<Button>(R.id.associateSpeakersButton).visibility = View.VISIBLE

        findViewById<Button>(R.id.playButton).setOnClickListener { startPlaying() }
        findViewById<Button>(R.id.pauseButton).setOnClickListener { pausePlayback() }
        findViewById<Button>(R.id.addMarkerButton).setOnClickListener { addMarker() }

        // Inicializar RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.markersRecyclerView)
        markerAdapter = MarkerAdapter(
            markers = markers,
            onMarkerClick = { time -> seekToMarker(time) },
            onTranscriptionChanged = { position, newText ->
                markers[position].transcription = newText
                markerAdapter.notifyItemChanged(position)
            },

            onSpeakerSelect = { position ->
                Log.d("SpeakerDebug", "onSpeakerSelect chamado para posição $position")
                showSelectSpeakerForMarkerDialog(position)
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
            onRecordTranscription = { position -> startRecordingForMarker(position, isTranscription = true) },
            onRecordTranslation = { position -> startRecordingForMarker(position, isTranscription = false) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = markerAdapter

        // Inicializar audioFileAdapter
        audioFileAdapter = AudioFileAdapter(
            audioFiles,
            onItemClick = { audioFile ->
                currentAudioFile = audioFile
                loadAudioFile(audioFile)
            },
            onDeleteClick = { audioFile ->
                deleteAudioFile(audioFile)
            }
        )
    }


    private fun stopRecordingForMarker() {
        markerRecordingTimer?.cancel()
        markerRecordingTimer = null

        try {
            transcriptionRecorder?.apply {
                try {
                    stop()
                } catch (e: IllegalStateException) {
                    // Ignora erro de gravação muito curta
                } finally {
                    release()
                }
            }
            transcriptionRecorder = null

            translationRecorder?.apply {
                try {
                    stop()
                } catch (e: IllegalStateException) {
                    // Ignora erro de gravação muito curta
                } finally {
                    release()
                }
            }
            translationRecorder = null

        } catch (e: Exception) {
            Log.e("Recording", "Erro ao parar gravação: ${e.message}")
        }
    }









    private fun setupSeekBar() {
        waveformView.onProgressChanged = { progress ->
            mediaPlayer?.let { player ->
                val newPosition = (player.duration * progress).toInt()
                player.seekTo(newPosition)
                updateCurrentTimeText(newPosition)
            }
        }
    }

    private fun playSegment(marker: Marker, shouldLoop: Boolean = false) {
        try {
            mediaPlayer?.apply {
                // Reseta quaisquer OnCompletionListener anteriores
                setOnCompletionListener(null)

                // Para qualquer reprodução em andamento
                if (isPlaying) {
                    pause()
                }

                // Define a posição inicial
                seekTo(marker.startTime.toInt())

                // Calcula o tempo final (usa duração total se não houver tempo final definido)
                val endTime = marker.endTime ?: duration.toLong()

                // Remove callbacks anteriores
                handler.removeCallbacksAndMessages(null)

                // Configura novo monitoramento
                handler.post(object : Runnable {
                    override fun run() {
                        if (isPlaying) {
                            val currentPos = currentPosition
                            if (currentPos >= endTime) {
                                if (shouldLoop) {
                                    seekTo(marker.startTime.toInt())
                                } else {
                                    pause()
                                    seekTo(marker.startTime.toInt())
                                    return
                                }
                            }
                            handler.postDelayed(this, 50)
                        }
                    }
                })

                // Inicia a reprodução
                start()

                // Atualiza a interface
                updateSeekBar()

            } ?: run {
                Toast.makeText(this, "Player não está pronto", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PlayerDebug", "Erro ao reproduzir segmento", e)
            Toast.makeText(this, "Erro ao reproduzir segmento: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    private fun createRecorder(filePath: String): MediaRecorder {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(filePath)
            prepare()
            start()
        }
    }




    private fun startRecordingForMarker(position: Int, isTranscription: Boolean) {
        if (position == -1) {
            stopRecordingForMarker()
            return
        }

        try {
            // Para qualquer gravação em andamento
            stopRecordingForMarker()

            // Cria o diretório se necessário
            val directory = getExternalFilesDir(null)
            directory?.mkdirs()

            // Gera nome único para o arquivo
            val timestamp = System.currentTimeMillis()
            val fileName = if (isTranscription)
                "transcription_${position}_${timestamp}.mp3"
            else
                "translation_${position}_${timestamp}.mp3"

            val file = File(directory, fileName)

            // Configura e inicia o recorder
            val recorder = createRecorder(file.absolutePath)

            if (isTranscription) {
                transcriptionRecorder = recorder
                markers[position].transcriptionAudioPath = file.absolutePath
            } else {
                translationRecorder = recorder
                markers[position].translationAudioPath = file.absolutePath
            }

            Toast.makeText(this, "Gravação iniciada", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("Recording", "Erro ao iniciar gravação: ${e.message}")
            Toast.makeText(this, "Erro ao iniciar gravação: ${e.message}", Toast.LENGTH_SHORT).show()
            stopRecordingForMarker()
        }
    }



    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    // Ignora erro de gravação muito curta
                    Log.e("Recording", "Erro ao parar gravação (possivelmente muito curta): ${e.message}")
                } finally {
                    reset()
                    release()
                }
            }
            mediaRecorder = null
            isRecording = false
            timer?.cancel()
            timer = null


            // Verificar se o arquivo foi criado com sucesso
            val file = File(audioFilePath)
            if (file.exists() && file.length() > 0) {
                // Criar o objeto AudioFile para a gravação
                val audioFile = AudioFile(
                    fileName = file.name,
                    filePath = audioFilePath,
                    dateCreated = System.currentTimeMillis()
                )

                // Adicionar à lista de áudios
                audioFiles.add(audioFile)
                currentAudioFile = audioFile


            }

        } catch (e: Exception) {
            Log.e("Recording", "Erro ao parar gravação: ${e.message}")
            Toast.makeText(this, "Erro ao parar gravação: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAssociatedSpeakersView() {
        val speakersTextView = findViewById<TextView>(R.id.associatedSpeakersTextView)

        currentAudioFile?.let { audioFile ->
            if (audioFile.speakers.isNotEmpty()) {
                val speakersText = "Falantes: " + audioFile.speakers.joinToString(", ") { it.name }
                speakersTextView.text = speakersText
                speakersTextView.visibility = View.VISIBLE
            } else {
                speakersTextView.visibility = View.GONE
            }
        } ?: run {
            speakersTextView.visibility = View.GONE
        }
    }



    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
    }

    private fun showNewProjectDialog() {
        val options = arrayOf("Carregar Áudio Existente", "Carregar Vídeo")

        AlertDialog.Builder(this)
            .setTitle("Novo Projeto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickAudioFile()
                    1 -> {
                        val intent = Intent(this, VideoPlayerActivity::class.java)
                        startActivity(intent)
                    }
                    2 -> {
                        val intent = Intent(this, VideoPlayerActivity::class.java)
                        intent.putExtra("SHOULD_PICK_VIDEO", true)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun pickAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
        }
        startActivityForResult(intent, PICK_AUDIO_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            VIDEO_PICK_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val videoUri = data.data
                    if (videoUri != null) {
                        // Inicia a VideoPlayerActivity com o URI do vídeo
                        val intent = Intent(this, VideoPlayerActivity::class.java)
                        intent.data = videoUri // Passa o URI do vídeo para a VideoPlayerActivity
                        startActivity(intent) // Inicia a VideoPlayerActivity
                    } else {
                        Toast.makeText(this, "Nenhum vídeo selecionado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Seleção de vídeo cancelada", Toast.LENGTH_SHORT).show()
                }
            }
            PICK_AUDIO_FILE -> {
                // Seu código existente para arquivos de áudio
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        loadAudioFromUri(uri)
                    }
                }
            }
        }
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
        }
        startActivityForResult(intent, VIDEO_PICK_REQUEST)
    }






    private fun startPlaying() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioFilePath)
                prepare()
                start()

                totalTimeTextView.text = formatTime(duration.toLong())

                val audioFile = File(audioFilePath)
                val audioData = ByteArray(audioFile.length().toInt())
                FileInputStream(audioFile).use { it.read(audioData) }
                waveformView.setAudioData(audioData)

                updateSeekBar()
                setOnCompletionListener {
                    pausePlayback()
                }
            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, "Erro ao reproduzir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        timer?.cancel()
    }

    private fun seekToMarker(time: Long) {
        mediaPlayer?.seekTo(time.toInt())
    }

    private var currentMarker: Marker? = null // Adicione esta variável na classe

    private fun addMarker() {
        try {
            val transcriptionEditText = findViewById<EditText>(R.id.transcriptionEditText)
            val translationEditText = findViewById<EditText>(R.id.translationEditText)
            val addMarkerButton = findViewById<Button>(R.id.addMarkerButton)

            val transcription = transcriptionEditText.text.toString()
            val translation = translationEditText.text.toString()

            if (mediaPlayer == null) {
                Toast.makeText(this, "Por favor, carregue um áudio primeiro", Toast.LENGTH_SHORT).show()
                return
            }

            val currentTime = mediaPlayer?.currentPosition?.toLong() ?: 0L

            if (currentMarker == null) {
                // Iniciando um novo marcador
                currentMarker = Marker(
                    startTime = currentTime,
                    transcription = transcription,
                    translation = translation
                )
                addMarkerButton.text = "Finalizar Marcador"
                Toast.makeText(this, "Início do marcador definido", Toast.LENGTH_SHORT).show()

                // Desabilita os campos de edição enquanto o marcador não for finalizado
                transcriptionEditText.isEnabled = false
                translationEditText.isEnabled = false

            } else {
                // Finalizando o marcador atual
                currentMarker?.let { marker ->
                    // Verifica se o tempo final é maior que o inicial
                    if (currentTime <= marker.startTime) {
                        Toast.makeText(this, "O tempo final deve ser maior que o inicial", Toast.LENGTH_SHORT).show()
                        return
                    }

                    marker.endTime = currentTime
                    markers.add(marker)
                    markerAdapter.notifyItemInserted(markers.size - 1)

                    // Limpa e reabilita os campos
                    transcriptionEditText.setText("")
                    translationEditText.setText("")
                    transcriptionEditText.isEnabled = true
                    translationEditText.isEnabled = true

                    currentMarker = null
                    addMarkerButton.text = "Adicionar Marcador"
                    Toast.makeText(this, "Marcador finalizado", Toast.LENGTH_SHORT).show()



                    // Salvar o projeto atualizado
                    currentProject?.let { project ->
                        project.markers = markers
                        projectManager.saveProject(project)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MarkerDebug", "Error adding marker", e)
            Toast.makeText(this, "Erro ao adicionar marcador: ${e.message}", Toast.LENGTH_LONG).show()

            // Reseta o estado em caso de erro
            currentMarker = null
            findViewById<Button>(R.id.addMarkerButton).text = "Adicionar Marcador"
            findViewById<EditText>(R.id.transcriptionEditText).isEnabled = true
            findViewById<EditText>(R.id.translationEditText).isEnabled = true
        }
    }



    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    timerTextView.text = formatTime(System.currentTimeMillis() - startRecordingTime)
                }
            }
        }, 0, 1000)
    }

    private fun updateSeekBar() {
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = player.currentPosition.toFloat() / player.duration
                        waveformView.setProgress(progress)
                        updateCurrentTimeText(player.currentPosition)
                        handler.postDelayed(this, 100)
                    }
                }
            }
        })
    }

    private fun updateCurrentTimeText(position: Int) {
        currentTimeTextView.text = formatTime(position.toLong())
    }

    private fun formatTime(timeInMillis: Long): String {
        val seconds = (timeInMillis / 1000) % 60
        val minutes = (timeInMillis / (1000 * 60)) % 60
        val hours = (timeInMillis / (1000 * 60 * 60)) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }



    private fun requestAudioPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                STORAGE_PERMISSION_CODE
            )
        }
    }





    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
        timer?.cancel()
    }



}