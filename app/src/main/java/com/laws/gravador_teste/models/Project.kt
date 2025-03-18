import com.laws.gravador_teste.Speaker

import com.laws.gravador_teste.models.Marker


data class Project(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val dateCreated: Long = System.currentTimeMillis(),
    var speakers: MutableList<Speaker> = mutableListOf(),
    var markers: MutableList<Marker> = mutableListOf()
)