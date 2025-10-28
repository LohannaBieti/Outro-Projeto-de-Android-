package com.example.biblioteca.model

data class Livro(
    val id: String = "",
    val titulo: String = "",
    val autor: String = "",
    val categoria: String = "",
    val status: String = "Não lido"
) {
    fun atualizarStatus(novoStatus: String): Livro =
        copy(status = novoStatus)
}
