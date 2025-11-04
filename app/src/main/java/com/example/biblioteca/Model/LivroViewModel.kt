package com.example.biblioteca.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblioteca.repository.LivroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// --- UiState ATUALIZADO ---
data class UiState(
    val loading: Boolean = false,
    val isSaving: Boolean = false, // Para feedback de salvamento/deletar
    val livros: List<Livro> = emptyList(),
    val error: String? = null
)

class LivroViewModel(
    private val repository: LivroRepository = LivroRepository() // prefer Hilt in real projeto
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState(loading = true))
    val ui: StateFlow<UiState> = _ui

    init { carregarLivros() }

    fun carregarLivros() {
        viewModelScope.launch {
            // Garante que 'isSaving' seja resetado se estava carregando
            _ui.value = _ui.value.copy(loading = true, isSaving = false)
            repository.getLivros().fold(
                onSuccess = { list ->
                    _ui.value = _ui.value.copy(loading = false, livros = list)
                },
                onFailure = { e ->
                    // fallback local pequeno para não quebrar UI
                    val fallback = listOf(
                        LivroFisico(id = "1", titulo = "Livro 1", autor = "Autor A"),
                        LivroDigital(id = "2", titulo = "Livro 2", autor = "Autor B", url = "https://")
                    )
                    _ui.value = UiState(loading = false, livros = fallback, error = e.message)
                }
            )
        }
    }

    /**
     * Tenta adicionar um novo livro.
     */
    fun adicionarLivro(livro: Livro, onSucesso: () -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isSaving = true) // Inicia o feedback de 'Salvando'
            repository.addLivro(livro).fold(
                onSuccess = {
                    _ui.value = _ui.value.copy(isSaving = false)
                    onSucesso()
                    carregarLivros()
                },
                onFailure = { e ->
                    _ui.value = _ui.value.copy(isSaving = false, error = e.message ?: "Erro ao salvar")
                }
            )
        }
    }

    // --- NOVA FUNÇÃO DE DELETAR LIVRO ---
    /**
     * Tenta deletar um livro.
     * @param id O ID do livro a ser deletado.
     * @param onSucesso Callback para ser executado se o livro for deletado (ex: navegar para trás).
     */
    fun deletarLivro(id: String, onSucesso: () -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isSaving = true) // Inicia o feedback
            repository.deletarLivro(id).fold(
                onSuccess = {
                    _ui.value = _ui.value.copy(isSaving = false)
                    onSucesso()
                    carregarLivros() // Recarrega a lista para remover o livro deletado
                },
                onFailure = { e ->
                    _ui.value = _ui.value.copy(isSaving = false, error = e.message ?: "Erro ao deletar")
                }
            )
        }
    }

    fun atualizarStatus(id: String, novoStatus: String) {
        viewModelScope.launch {
            // optimistic update
            val current = _ui.value.livros.toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index >= 0) {
                current[index] = current[index].atualizarStatus(novoStatus)
                _ui.value = _ui.value.copy(livros = current)
            }
            val result = repository.atualizarStatus(id, novoStatus)
            result.fold(
                onSuccess = { /* ok */ },
                onFailure = { e ->
                    // rollback on failure
                    carregarLivros()
                    _ui.value = _ui.value.copy(error = e.message ?: "Erro ao atualizar status")
                }
            )
        }
    }

    fun marcarFavorito(id: String, favorito: Boolean) {
        viewModelScope.launch {
            // no model field for favorito; your Firestore will hold it. Here we just call repo and surface errors.
            repository.marcarFavorito(id, favorito).fold(
                onSuccess = { /* opcional: atualizar local se tiver campo */ },
                onFailure = { e -> _ui.value = _ui.value.copy(error = e.message) }
            )
        }
    }

    fun findLivroById(id: String): Livro? = _ui.value.livros.find { it.id == id }
}