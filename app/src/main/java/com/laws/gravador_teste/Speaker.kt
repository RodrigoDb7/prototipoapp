package com.laws.gravador_teste

data class Speaker(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val age: Int?,
    val gender: String?,
    val language: String,
    val dialect: String?
) {
    // Construtor vazio para o Gson
    constructor() : this("", "", null, null, "", null)

    override fun toString(): String {
        return "Speaker(id=$id, name=$name, language=$language)"
    }
}