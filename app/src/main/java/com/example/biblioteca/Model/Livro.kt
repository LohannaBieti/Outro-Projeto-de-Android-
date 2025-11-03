package com.example.biblioteca.model

sealed class Livro {
    abstract val id: String
    abstract val titulo: String
    abstract val autor: String
    abstract val status: String
}

data class LivroFisico(
    override val id: String = "",
    override val titulo: String = "",
    override val autor: String = "",
    val localizacaoPrateleira: String = "",
    override val status: String = "Não lido"
) : Livro()

data class LivroDigital(
    override val id: String = "",
    override val titulo: String = "",
    override val autor: String = "",
    val url: String = "",
    override val status: String = "Não lido"
) : Livro()

// helper extension to update status immutably for both types
fun Livro.atualizarStatus(novoStatus: String): Livro = when (this) {
    is LivroFisico -> this.copy(status = novoStatus)
    is LivroDigital -> this.copy(status = novoStatus)
}
