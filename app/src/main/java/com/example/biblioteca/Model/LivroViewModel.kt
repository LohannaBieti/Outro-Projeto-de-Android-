package com.example.biblioteca.model

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblioteca.repository.LivroRepository
import kotlinx.coroutines.launch

class LivroViewModel : ViewModel() {
    val livros = mutableStateListOf<Livro>()
    private val repository = LivroRepository()

    init {
        carregarLivros()
    }

    fun carregarLivros() {
        viewModelScope.launch {
            try {
                livros.clear()
                // se o Firebase falhar, você pode adicionar fallback local aqui
                livros.addAll(repository.getLivros())
            } catch (e: Exception) {
                println("Erro ao carregar livros: ${e.message}")
                // fallback de dev: dados locais para não quebrar UI
                if (livros.isEmpty()) {
                    livros.addAll(
                        listOf(
                            Livro("1", "Livro 1", "Autor A", "Físico", "Não Lido"),
                            Livro("2", "Livro 2", "Autor B", "Digital", "Lido")
                        )
                    )
                }
            }
        }
    }

    fun atualizarStatus(id: String, novoStatus: String) {
        viewModelScope.launch {
            try {
                repository.atualizarStatus(id, novoStatus)
                val index = livros.indexOfFirst { it.id == id }
                if (index >= 0) {
                    livros[index] = livros[index].atualizarStatus(novoStatus)
                }
            } catch (e: Exception) {
                println("Erro ao atualizar o status do livro: ${e.message}")
            }
        }
    }

    fun findLivroById(id: String): Livro? = livros.find { it.id == id }
}
