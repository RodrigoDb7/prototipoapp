import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.laws.gravador_teste.R
import com.laws.gravador_teste.Speaker

class SpeakerAdapter(
    private val speakers: MutableList<Speaker>,
    private val onEditClick: (Speaker) -> Unit,
    private val onDeleteClick: (Speaker) -> Unit
) : RecyclerView.Adapter<SpeakerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val speakerName: TextView = view.findViewById(R.id.speakerName)
        val speakerDetails: TextView = view.findViewById(R.id.speakerDetails)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speaker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val speaker = speakers[position]
        holder.speakerName.text = speaker.name
        holder.speakerDetails.text = buildSpeakerDetails(speaker)

        holder.editButton.setOnClickListener { onEditClick(speaker) }
        holder.deleteButton.setOnClickListener { onDeleteClick(speaker) }
    }

    override fun getItemCount() = speakers.size

    private fun buildSpeakerDetails(speaker: Speaker): String {
        return buildString {
            append("Língua: ${speaker.language}")
            speaker.dialect?.let { append(" | Dialeto: $it") }
            speaker.age?.let { append(" | Idade: $it") }
            speaker.gender?.let { append(" | Gênero: $it") }
        }
    }
}