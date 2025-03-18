package com.laws.gravador_teste.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.laws.gravador_teste.R
import com.laws.gravador_teste.Speaker

class SelectSpeakersAdapter(
    private val speakers: List<Speaker>,
    private val selectedSpeakers: MutableSet<Speaker> = mutableSetOf()
) : RecyclerView.Adapter<SelectSpeakersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val speakerName: TextView = view.findViewById(R.id.speakerName)
        val speakerDetails: TextView = view.findViewById(R.id.speakerDetails)
        val checkBox: CheckBox = view.findViewById(R.id.speakerCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_speaker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val speaker = speakers[position]
        holder.speakerName.text = speaker.name
        holder.speakerDetails.text = buildSpeakerDetails(speaker)

        // Configurar o CheckBox
        holder.checkBox.isChecked = selectedSpeakers.contains(speaker)

        // Configurar clique no item
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
            if (holder.checkBox.isChecked) {
                selectedSpeakers.add(speaker)
            } else {
                selectedSpeakers.remove(speaker)
            }
        }

        // Configurar clique no CheckBox
        holder.checkBox.setOnClickListener {
            if (holder.checkBox.isChecked) {
                selectedSpeakers.add(speaker)
            } else {
                selectedSpeakers.remove(speaker)
            }
        }
    }

    override fun getItemCount() = speakers.size

    // Método para obter os falantes selecionados
    fun getSelectedSpeakers(): List<Speaker> {
        return selectedSpeakers.toList()
    }

    private fun buildSpeakerDetails(speaker: Speaker): String {
        return buildString {
            append("Língua: ${speaker.language}")
            speaker.dialect?.let { append(" | Dialeto: $it") }
            speaker.age?.let { append(" | Idade: $it") }
            speaker.gender?.let { append(" | Gênero: $it") }
        }
    }
}