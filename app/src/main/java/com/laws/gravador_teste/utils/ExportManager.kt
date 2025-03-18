import android.content.Context
import android.os.Environment

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ExportManager(private val context: Context) {

    fun exportToCsv(project: Project, fileName: String = "transcricao.csv"): Boolean {
        return try {
            val content = buildString {
                // Cabeçalho com metadados do projeto
                appendLine("# Metadados do Projeto")
                appendLine("Nome do Projeto: ${project.name}")
                appendLine("Data de Criação: ${formatDate(project.dateCreated)}")

                // Informações dos falantes
                if (project.speakers.isNotEmpty()) {
                    appendLine("\n# Falantes")
                    project.speakers.forEachIndexed { index, speaker ->
                        appendLine("\nFalante ${index + 1}")
                        appendLine("Nome: ${speaker.name}")
                        appendLine("Idade: ${speaker.age ?: "Não informado"}")
                        appendLine("Gênero: ${speaker.gender ?: "Não informado"}")
                        appendLine("Língua: ${speaker.language}")
                        appendLine("Dialeto: ${speaker.dialect ?: "Não informado"}")
                    }
                }

                // Marcadores com informação do falante
                appendLine("\n# Transcrições")
                appendLine("Início,Fim,Falante,Transcrição,Tradução")
                project.markers.forEach { marker ->
                    appendLine("${marker.getFormattedStartTime()},${marker.getFormattedEndTime()}," +
                            "\"${marker.speaker?.name ?: "Não atribuído"}\",\"${marker.transcription}\",\"${marker.translation}\"")
                }
            }

            saveFile(fileName, content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportToElan(project: Project, audioPath: String, fileName: String = "transcricao.eaf"): Boolean {
        return try {
            val content = buildElanXml(project, audioPath)
            saveFile(fileName, content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildElanXml(project: Project, audioPath: String): String {
        val timeOrder = buildString {
            project.markers.forEachIndexed { index, marker ->
                appendLine("<TIME_SLOT TIME_SLOT_ID=\"ts${index * 2}\" TIME_VALUE=\"${marker.startTime}\"/>")
                appendLine("<TIME_SLOT TIME_SLOT_ID=\"ts${index * 2 + 1}\" TIME_VALUE=\"${marker.endTime ?: marker.startTime}\"/>")
            }
        }

        // Anotações de transcrição e tradução
        val annotations = buildString {
            project.markers.forEachIndexed { index, marker ->
                // Obter o nome do falante de forma segura
                val speakerPrefix = marker.speaker?.let { "[${it.name}] " } ?: ""

                // Anotação da transcrição com o falante incluído
                appendLine("""
            <ANNOTATION>
                <ALIGNABLE_ANNOTATION ANNOTATION_ID="a${index * 2}" 
                    TIME_SLOT_REF1="ts${index * 2}" 
                    TIME_SLOT_REF2="ts${index * 2 + 1}">
                    <ANNOTATION_VALUE>${speakerPrefix}${marker.transcription}</ANNOTATION_VALUE>
                </ALIGNABLE_ANNOTATION>
            </ANNOTATION>
        """.trimIndent())

                // Anotação da tradução
                appendLine("""
            <ANNOTATION>
                <ALIGNABLE_ANNOTATION ANNOTATION_ID="a${index * 2 + 1}" 
                    TIME_SLOT_REF1="ts${index * 2}" 
                    TIME_SLOT_REF2="ts${index * 2 + 1}">
                    <ANNOTATION_VALUE>${marker.translation}</ANNOTATION_VALUE>
                </ALIGNABLE_ANNOTATION>
            </ANNOTATION>
        """.trimIndent())



                // Anotação da tradução
                appendLine("""
                    <ANNOTATION>
                        <ALIGNABLE_ANNOTATION ANNOTATION_ID="a${index * 2 + 1}" 
                            TIME_SLOT_REF1="ts${index * 2}" 
                            TIME_SLOT_REF2="ts${index * 2 + 1}">
                            <ANNOTATION_VALUE>${marker.translation}</ANNOTATION_VALUE>
                        </ALIGNABLE_ANNOTATION>
                    </ANNOTATION>
                """.trimIndent())
            }
        }

        // Tiers específicas para cada falante
        val speakerTiers = buildString {
            // Agrupar marcadores por falante
            val speakerMarkers = project.markers
                .filter { it.speaker != null }
                .groupBy { it.speaker }

            speakerMarkers.forEach { (speaker, markerList) ->
                speaker?.let {
                    appendLine("""<TIER LINGUISTIC_TYPE_NAME="default-lt" TIER_ID="falante_${speaker.id.replace(" ", "_")}">""")

                    markerList.forEachIndexed { _, marker ->
                        // Encontrar o índice do marcador na lista original
                        val markerIndex = project.markers.indexOfFirst { it.id == marker.id }
                        if (markerIndex != -1) {
                            val annotationId = "speaker_${speaker.id.replace(" ", "_")}_${markerIndex}"
                            val timeSlotRef1 = markerIndex * 2
                            val timeSlotRef2 = timeSlotRef1 + 1

                            appendLine("""
                                <ANNOTATION>
                                    <ALIGNABLE_ANNOTATION ANNOTATION_ID="${annotationId}" 
                                        TIME_SLOT_REF1="ts${timeSlotRef1}" 
                                        TIME_SLOT_REF2="ts${timeSlotRef2}">
                                        <ANNOTATION_VALUE>${marker.transcription}</ANNOTATION_VALUE>
                                    </ALIGNABLE_ANNOTATION>
                                </ANNOTATION>
                            """.trimIndent())
                        }
                    }

                    appendLine("</TIER>")
                }
            }
        }

        // Construir metadados do projeto em formato XML
        val projectMetadata = buildString {
            appendLine("<PROPERTY NAME=\"Project_Name\">${project.name}</PROPERTY>")
            appendLine("<PROPERTY NAME=\"Project_Date\">${formatDate(project.dateCreated)}</PROPERTY>")

            // Metadados dos falantes
            project.speakers.forEachIndexed { index, speaker ->
                appendLine("<PROPERTY NAME=\"Speaker_${index+1}_Name\">${speaker.name}</PROPERTY>")
                appendLine("<PROPERTY NAME=\"Speaker_${index+1}_Age\">${speaker.age ?: "N/A"}</PROPERTY>")
                appendLine("<PROPERTY NAME=\"Speaker_${index+1}_Gender\">${speaker.gender ?: "N/A"}</PROPERTY>")
                appendLine("<PROPERTY NAME=\"Speaker_${index+1}_Language\">${speaker.language}</PROPERTY>")
                appendLine("<PROPERTY NAME=\"Speaker_${index+1}_Dialect\">${speaker.dialect ?: "N/A"}</PROPERTY>")
            }
        }

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ANNOTATION_DOCUMENT xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                VERSION="2.8"
                DATE="${System.currentTimeMillis()}"
                AUTHOR="GravadorApp">
                <HEADER MEDIA_FILE="" TIME_UNITS="milliseconds">
                    <MEDIA_DESCRIPTOR
                        MEDIA_URL="file://$audioPath"
                        MIME_TYPE="audio/mp3"
                        RELATIVE_MEDIA_URL="$audioPath"/>
                    <PROPERTY NAME="lastUsedAnnotationId">0</PROPERTY>
                    $projectMetadata
                </HEADER>
                <TIME_ORDER>
                    $timeOrder
                </TIME_ORDER>
                <TIER LINGUISTIC_TYPE_NAME="default-lt" TIER_ID="transcricao">
                    $annotations
                </TIER>
                <TIER LINGUISTIC_TYPE_NAME="default-lt" TIER_ID="traducao" 
                    PARENT_REF="transcricao">
                    $annotations
                </TIER>
                $speakerTiers
                <LINGUISTIC_TYPE LINGUISTIC_TYPE_ID="default-lt" 
                    TIME_ALIGNABLE="true"/>
                <CONSTRAINT 
                    STEREOTYPE="Time_Subdivision"
                    DESCRIPTION="Time subdivision of parent annotation's time interval, no time gaps allowed within this interval"/>
                <CONSTRAINT 
                    STEREOTYPE="Symbolic_Subdivision"
                    DESCRIPTION="Symbolic subdivision of a parent annotation. Annotations refering to the same parent are ordered"/>
            </ANNOTATION_DOCUMENT>
        """.trimIndent()
    }

    fun exportToXml(project: Project, fileName: String = "transcricao.xml"): Boolean {
        return try {
            val content = buildString {
                // Cabeçalho XML
                appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                appendLine("<transcription>")

                // Metadados do projeto
                appendLine("  <project>")
                appendLine("    <name>${escapeXml(project.name)}</name>")
                appendLine("    <date>${formatDate(project.dateCreated)}</date>")
                appendLine("  </project>")

                // Informações dos falantes
                appendLine("  <speakers>")
                project.speakers.forEach { speaker ->
                    appendLine("    <speaker>")
                    appendLine("      <id>${speaker.id}</id>")
                    appendLine("      <name>${escapeXml(speaker.name)}</name>")
                    appendLine("      <age>${speaker.age ?: ""}</age>")
                    appendLine("      <gender>${escapeXml(speaker.gender ?: "")}</gender>")
                    appendLine("      <language>${escapeXml(speaker.language)}</language>")
                    appendLine("      <dialect>${escapeXml(speaker.dialect ?: "")}</dialect>")
                    appendLine("    </speaker>")
                }
                appendLine("  </speakers>")

                // Marcadores
                appendLine("  <markers>")
                project.markers.forEach { marker ->
                    appendLine("    <marker>")
                    appendLine("      <id>${marker.id}</id>")
                    appendLine("      <startTime>${marker.startTime}</startTime>")
                    appendLine("      <endTime>${marker.endTime ?: ""}</endTime>")
                    appendLine("      <transcription>${escapeXml(marker.transcription)}</transcription>")
                    appendLine("      <translation>${escapeXml(marker.translation)}</translation>")

                    // Incluir falante associado, se houver
                    marker.speaker?.let { speaker ->
                        appendLine("      <speaker>")
                        appendLine("        <id>${speaker.id}</id>")
                        appendLine("        <name>${escapeXml(speaker.name)}</name>")
                        appendLine("      </speaker>")
                    }

                    // Caminhos dos arquivos de áudio, se houver
                    marker.transcriptionAudioPath?.let {
                        appendLine("      <transcriptionAudio>${escapeXml(it)}</transcriptionAudio>")
                    }
                    marker.translationAudioPath?.let {
                        appendLine("      <translationAudio>${escapeXml(it)}</translationAudio>")
                    }

                    appendLine("    </marker>")
                }
                appendLine("  </markers>")

                appendLine("</transcription>")
            }

            saveFile(fileName, content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Função auxiliar para escapar caracteres especiais em XML
    private fun escapeXml(string: String): String {
        return string
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }





    private fun saveFile(fileName: String, content: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        file.writeText(content)
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(timestamp)
    }
}