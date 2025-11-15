package com.example.biblioteca.model

/**
 * Cria um Mapa de dados a partir de um objeto Livro,
 * pronto para ser salvo no Firestore.
 */
fun Livro.toMap(): Map<String, Any> {
    val dadosComuns = mutableMapOf<String, Any>(
        "titulo" to this.titulo,
        "autor" to this.autor,
        "status" to this.status,
        "favorito" to this.favorito,
        "categoria" to this.categoria
    )

    when (this) {
        is LivroFisico -> {
            dadosComuns["tipo"] = "fisico"
            dadosComuns["localizacaoPrateleira"] = this.localizacaoPrateleira
        }
        is LivroDigital -> {
            dadosComuns["tipo"] = "digital"
            dadosComuns["url"] = this.url
        }
    }
    return dadosComuns
}

sealed class Livro {
    abstract val id: String
    abstract val titulo: String
    abstract val autor: String
    abstract val status: String
    abstract val favorito: Boolean
    abstract val categoria: String // <-- NOVO
}

data class LivroFisico(
    override val id: String = "",
    override val titulo: String = "",
    override val autor: String = "",
    val localizacaoPrateleira: String = "",
    override val status: String = "Não lido",
    override val favorito: Boolean = false,
    override val categoria: String = "Sem Categoria" // <-- NOVO
) : Livro()

data class LivroDigital(
    override val id: String = "",
    override val titulo: String = "",
    override val autor: String = "",
    val url: String = "",
    override val status: String = "Não lido",
    override val favorito: Boolean = false,
    override val categoria: String = "Sem Categoria" // <-- NOVO
) : Livro()

// helper extension to update status immutably for both types
fun Livro.atualizarStatus(novoStatus: String): Livro = when (this) {
    is LivroFisico -> this.copy(status = novoStatus)
    is LivroDigital -> this.copy(status = novoStatus)
}

// helper extension to update favorito immutably for both types
fun Livro.atualizarFavorito(novoFavorito: Boolean): Livro = when (this) {
    is LivroFisico -> this.copy(favorito = novoFavorito)
    is LivroDigital -> this.copy(favorito = novoFavorito)
}