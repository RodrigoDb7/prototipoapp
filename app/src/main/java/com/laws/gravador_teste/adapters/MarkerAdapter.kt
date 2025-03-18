package com.laws.gravador_teste.adapters

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.laws.gravador_teste.models.Marker
import com.laws.gravador_teste.R
import java.util.Timer

class MarkerAdapter(
    private val markers: MutableList<Marker>,
    private val onMarkerClick: (Long) -> Unit,
    private val onTranscriptionChanged: (Int, String) -> Unit,
    private val onTranslationChanged: (Int, String) -> Unit,
    private val onDeleteMarker: (Int) -> Unit,
    private val onPlaySegment: (Marker, Boolean) -> Unit,
    private val onRecordTranscription: (Int) -> Unit,
    private val onRecordTranslation: (Int) -> Unit,
    private val onSpeakerSelect: (Int) -> Unit,

) : RecyclerView.Adapter<MarkerAdapter.MarkerViewHolder>() {

    private var currentMediaPlayer: MediaPlayer? = null
    private var currentPlayingButton: ImageButton? = null
    private var isRecordingTranscription = false
    private var isRecordingTranslation = false
    private var currentDialog: Dialog? = null

    private var recordingTimer: Timer? = null


    class MarkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val transcriptionTextView: TextView = itemView.findViewById(R.id.transcriptionTextView)
        val translationTextView: TextView = itemView.findViewById(R.id.translationTextView)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val playSegmentButton: ImageButton = itemView.findViewById(R.id.playSegmentButton)
        val loopButton: ImageButton = itemView.findViewById(R.id.loopButton)
        val containerView: View = itemView
        val speakerTextView: TextView = itemView.findViewById(R.id.speakerTextView)
        val selectSpeakerButton: ImageButton = itemView.findViewById(R.id.selectSpeakerButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.marker_item, parent, false)
        return MarkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        val marker = markers[position]

        holder.apply {
            timeTextView.text = "${marker.getFormattedStartTime()} - ${marker.getFormattedEndTime()}"
            transcriptionTextView.text = marker.transcription
            translationTextView.text = marker.translation

            // Se houver um speaker, atualize o texto
            speakerTextView.text = if (marker.speaker != null)
                "Falante: ${marker.speaker?.name}"
            else
                "Falante: Não atribuído"

            containerView.setOnClickListener {
                onMarkerClick(marker.startTime)
            }


            selectSpeakerButton.setOnClickListener {
                Log.d("SpeakerDebug", "Botão de seleção de falante clicado para posição $position")
                onSpeakerSelect(position)
            }
            editButton.setOnClickListener {
                showEditDialog(holder.itemView.context, position, marker)
            }

            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(holder.itemView.context, position)
            }

            playSegmentButton.setOnClickListener {
                onPlaySegment(marker, false)
            }

            loopButton.setOnClickListener {
                onPlaySegment(marker, true)
            }



        }
    }

    override fun getItemCount() = markers.size

    private fun showEditDialog(context: Context, position: Int, marker: Marker) {
        currentDialog?.dismiss()

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_marker, null)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Editar Marcador")
            .setView(dialogView)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                stopRecording(isRecordingTranscription)
                stopRecording(isRecordingTranslation)
            }
            .create()

        dialog.show()

        // Obter a referência ao botão de reprodução da transcrição e definir a descrição
        val playTranscriptionButton = dialogView.findViewById<ImageButton>(R.id.playTranscriptionButton)
        playTranscriptionButton?.contentDescription = "Fala Cuidadosa"

        // Configurar botões de gravação
        val recordTranscriptionButton = dialogView.findViewById<ImageButton>(R.id.recordTranscriptionButton)
        val recordTranslationButton = dialogView.findViewById<ImageButton>(R.id.recordTranslationButton)

        // Configurar textos de tempo
        val transcriptionTimeText = dialogView.findViewById<TextView>(R.id.transcriptionTimeText)
        val translationTimeText = dialogView.findViewById<TextView>(R.id.translationTimeText)

        // Configurar eventos de clique para os botões de gravação
        recordTranscriptionButton.setOnClickListener {
            if (isRecordingTranscription) {
                // Parar gravação
                onRecordTranscription(-1)
                isRecordingTranscription = false
                recordTranscriptionButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            } else {
                // Iniciar gravação
                onRecordTranscription(position)
                isRecordingTranscription = true
                isRecordingTranslation = false // Garante que apenas uma gravação ocorra de cada vez
                recordTranscriptionButton.setImageResource(android.R.drawable.ic_media_pause)
                recordTranslationButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            }
        }

        recordTranslationButton.setOnClickListener {
            if (isRecordingTranslation) {
                // Parar gravação
                onRecordTranslation(-1)
                isRecordingTranslation = false
                recordTranslationButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            } else {
                // Iniciar gravação
                onRecordTranslation(position)
                isRecordingTranslation = true
                isRecordingTranscription = false // Garante que apenas uma gravação ocorra de cada vez
                recordTranslationButton.setImageResource(android.R.drawable.ic_media_pause)
                recordTranscriptionButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            }
        }

        // Configurar eventos de clique para os botões de reprodução
        playTranscriptionButton.setOnClickListener {
            marker.transcriptionAudioPath?.let { audioPath ->
                playAudio(audioPath, playTranscriptionButton)
            } ?: run {
                Toast.makeText(context, "Não há gravação de fala cuidadosa", Toast.LENGTH_SHORT).show()
            }
        }

        val playTranslationButton = dialogView.findViewById<ImageButton>(R.id.playTranslationButton)
        playTranslationButton.setOnClickListener {
            marker.translationAudioPath?.let { audioPath ->
                playAudio(audioPath, playTranslationButton)
            } ?: run {
                Toast.makeText(context, "Não há gravação da tradução", Toast.LENGTH_SHORT).show()
            }
        }

        val transcriptionEdit = dialogView.findViewById<EditText>(R.id.transcriptionEditText)
        val translationEdit = dialogView.findViewById<EditText>(R.id.translationEditText)

        transcriptionEdit.setText(marker.transcription)
        translationEdit.setText(marker.translation)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val newTranscription = transcriptionEdit.text.toString()
            val newTranslation = translationEdit.text.toString()

            marker.transcription = newTranscription
            marker.translation = newTranslation

            onTranscriptionChanged(position, newTranscription)
            onTranslationChanged(position, newTranslation)

            notifyItemChanged(position)
            dialog.dismiss()
        }

        currentDialog = dialog
    }

    // Método auxiliar para reprodução de áudio (adicionar na classe)
    private fun playAudio(audioPath: String, button: ImageButton) {
        try {
            currentMediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            currentPlayingButton?.setImageResource(android.R.drawable.ic_media_play)

            currentMediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()

                button.setImageResource(android.R.drawable.ic_media_pause)
                currentPlayingButton = button

                setOnCompletionListener {
                    button.setImageResource(android.R.drawable.ic_media_play)
                    currentPlayingButton = null
                    release()
                    currentMediaPlayer = null
                }
            }
        } catch (e: Exception) {
            Log.e("MarkerAdapter", "Erro ao reproduzir áudio", e)
            Toast.makeText(button.context, "Erro ao reproduzir áudio: ${e.message}", Toast.LENGTH_SHORT).show()
            currentMediaPlayer?.release()
            currentMediaPlayer = null
            button.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Excluir Marcador")
            .setMessage("Tem certeza que deseja excluir este marcador?")
            .setPositiveButton("Excluir") { _, _ ->
                onDeleteMarker(position)
                notifyItemRemoved(position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

   

    fun cleanupMediaResources() {
        stopRecordingTimer()
        currentMediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        currentMediaPlayer = null
        currentPlayingButton?.setImageResource(android.R.drawable.ic_media_play)
        currentPlayingButton = null
        currentDialog?.dismiss()
        currentDialog = null
    }

    private fun stopRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = null
    }

    private fun stopRecording(isTranscription: Boolean) {
        if (isTranscription) {
            onRecordTranscription(-1)
            isRecordingTranscription = false
        } else {
            onRecordTranslation(-1)
            isRecordingTranslation = false
        }
    }

    override fun onViewRecycled(holder: MarkerViewHolder) {
        super.onViewRecycled(holder)
        cleanupMediaResources()
    }
}