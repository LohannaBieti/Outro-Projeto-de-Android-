package com.example.biblioteca.Model

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblioteca.repository.LivroRepository
import kotlinx.coroutines.launch

class LivroViewModel : ViewModel() {
    val livros = mutableStateListOf<Livro>()
    private val repository = LivroRepository()

    // Função para carregar livros
    fun carregarLivros() {
        viewModelScope.launch {
            try {
                // Limpa a lista e adiciona os livros carregados
                livros.clear()
                livros.addAll(repository.getLivros()) // Busca livros no Firebase
            } catch (e: Exception) {
                // Trate erros aqui (ex: erro de rede)
                println("Erro ao carregar livros: ${e.message}")
            }
        }
    }

    fun atualizarStatus(id: String, novoStatus: String) {
        viewModelScope.launch {
            try {
                // Atualiza o status no Firebase
                repository.atualizarStatus(id, novoStatus)

                // Encontra o livro na lista e atualiza seu status
                val livro = livros.find { it.id == id }
                livro?.let {
                    // Substitui o livro na lista com o novo status
                    val livroAtualizado =
                        it.atualizarStatus(novoStatus) // Usa o método atualizarStatus
                    val index = livros.indexOf(it)
                    livros[index] = livroAtualizado
                }
            } catch (e: Exception) {
                // Trate erros de atualização de status
                println("Erro ao atualizar o status do livro: ${e.message}")
            }
        }
    }
}