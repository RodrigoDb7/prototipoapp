package com.laws.gravador_teste.utils

import Project
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
//import com.laws.gravador_teste.models.Project

// org.utils.ProjectManager.kt
class ProjectManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("ProjectData", Context.MODE_PRIVATE)
    private val gson = GsonBuilder().setLenient().create()

    fun saveProject(project: Project) {
        try {
            Log.d("ProjectDebug", "Salvando projeto: ${project.name}, ID: ${project.id}")
            Log.d("ProjectDebug", "Número de falantes: ${project.speakers.size}")

            // Listar todos os falantes para verificação
            project.speakers.forEachIndexed { index, speaker ->
                Log.d("ProjectDebug", "Falante $index: ${speaker.name}, ID: ${speaker.id}")
            }

            val projectJson = gson.toJson(project)
            Log.d("ProjectDebug", "JSON gerado: $projectJson")

            sharedPreferences.edit()
                .putString("project_${project.id}", projectJson)
                .apply()

            Log.d("ProjectDebug", "Projeto salvo com sucesso")
        } catch (e: Exception) {
            Log.e("ProjectDebug", "Erro ao salvar projeto", e)
        }
    }

    fun loadProject(projectId: Long): Project? {
        try {
            Log.d("ProjectDebug", "Carregando projeto ID: $projectId")

            val projectJson = sharedPreferences.getString("project_${projectId}", null)
            if (projectJson == null) {
                Log.w("ProjectDebug", "Nenhum JSON encontrado para o projeto ID: $projectId")
                return null
            }

            Log.d("ProjectDebug", "JSON carregado: $projectJson")

            val project = gson.fromJson(projectJson, Project::class.java)

            // Verificar se project.speakers é nulo e inicializar se necessário
            if (project.speakers == null) {
                Log.w("ProjectDebug", "Lista de falantes é nula, inicializando com lista vazia")
                project.speakers = mutableListOf()
            }

            Log.d("ProjectDebug", "Projeto carregado: ${project.name}, ID: ${project.id}")
            Log.d("ProjectDebug", "Número de falantes: ${project.speakers.size}")

            // Listar todos os falantes para verificação
            project.speakers.forEachIndexed { index, speaker ->
                Log.d("ProjectDebug", "Falante $index: ${speaker.name}, ID: ${speaker.id}")
            }

            return project
        } catch (e: Exception) {
            Log.e("ProjectDebug", "Erro ao carregar projeto", e)
            return null
        }
    }

    fun saveCurrentProjectId(projectId: Long) {
        sharedPreferences.edit()
            .putLong("current_project_id", projectId)
            .apply()
    }

    fun getCurrentProjectId(): Long {
        return sharedPreferences.getLong("current_project_id", -1)
    }
}