package com.example.biblioteca.Model // Mesmo pacote

// Definição da classe Livro
class Livro(
    val id: String,
    val titulo: String,
    val autor: String,
    val categoria: String,
    var status: String
) {
    // Método para atualizar o status
    fun atualizarStatus(novoStatus: String): Livro {
        return Livro(id, titulo, autor, categoria, novoStatus)
    }
}
