package com.laws.gravador_teste.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.laws.gravador_teste.R
import com.laws.gravador_teste.utils.TimeUtils
import com.laws.gravador_teste.models.AudioFile


class AudioFileAdapter(
    private val audioFiles: List<AudioFile>,
    private val onItemClick: (AudioFile) -> Unit,
    private val onDeleteClick: (AudioFile) -> Unit
) : RecyclerView.Adapter<AudioFileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
        val dateCreated: TextView = view.findViewById(R.id.dateCreated)
        val duration: TextView = view.findViewById(R.id.duration)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audio_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioFile = audioFiles[position]

        holder.fileName.text = audioFile.fileName
        holder.dateCreated.text = TimeUtils.formatDate(audioFile.dateCreated)
        holder.duration.text = TimeUtils.formatDuration(audioFile.duration)

        holder.itemView.setOnClickListener { onItemClick(audioFile) }
        holder.deleteButton.setOnClickListener { onDeleteClick(audioFile) }
    }

    override fun getItemCount() = audioFiles.size
}