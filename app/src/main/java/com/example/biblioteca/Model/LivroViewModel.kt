package com.example.biblioteca.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblioteca.repository.LivroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// --- UiState (Sem mudanças) ---
data class UiState(
    val loading: Boolean = false,
    val isSaving: Boolean = false,
    val livros: List<Livro> = emptyList(), // Lista filtrada para a UI
    val error: String? = null,
    // Novos campos para Busca e Filtro
    val searchQuery: String = "",
    val selectedAutor: String = "Todos",
    val selectedCategoria: String = "Todas",
    val autoresDisponiveis: List<String> = listOf("Todos"),
    val categoriasDisponiveis: List<String> = listOf("Todas")
)

class LivroViewModel(
    private val repository: LivroRepository = LivroRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState(loading = true))
    val ui: StateFlow<UiState> = _ui

    // Lista mestra que guarda TODOS os livros do Firestore
    private var listaMestraDeLivros: List<Livro> = emptyList()

    init { carregarLivros() }

    fun carregarLivros() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, isSaving = false)
            repository.getLivros().fold(
                onSuccess = { list ->
                    listaMestraDeLivros = list.sortedBy { it.titulo }
                    // Atualiza a UI com a lista mestra e os filtros
                    _atualizarListasDaUi()
                },
                onFailure = { e ->
                    listaMestraDeLivros = emptyList()
                    _ui.value = _ui.value.copy(loading = false, error = e.message, livros = emptyList())
                }
            )
        }
    }

    // --- LÓGICA DE BUSCA E FILTRO ---

    /**
     * Função central que aplica filtros e busca na lista mestra
     * e atualiza o UiState.
     */
    private fun _atualizarListasDaUi() {
        val state = _ui.value // Pega o estado atual

        // 1. Gera as listas de filtros (dinâmicas e únicas)

        // --- CORREÇÃO: Usa groupBy para de-duplicar sem diferenciar maiúsculas/minúsculas
        // Isso agrupa "Autor" e "autor" juntos e pega apenas o primeiro ("Autor")
        val autores = listOf("Todos") + listaMestraDeLivros.map { it.autor }
            .groupBy { it.lowercase() } // Agrupa por "frank herbert"
            .map { it.value.first() }     // Pega o primeiro original (ex: "Frank Herbert")
            .sorted()

        val categorias = listOf("Todas") + listaMestraDeLivros.map { it.categoria }
            .groupBy { it.lowercase() }
            .map { it.value.first() }
            .sorted()

        // 2. Aplica a busca e os filtros
        val livrosFiltrados = listaMestraDeLivros.filter { livro ->
            // Lógica da Busca (título E autor)
            val buscaOk = (livro.titulo.contains(state.searchQuery, ignoreCase = true) ||
                    livro.autor.contains(state.searchQuery, ignoreCase = true))

            // --- CORREÇÃO: Compara ignorando maiúsculas/minúsculas
            // Lógica do Filtro de Autor
            val autorOk = (state.selectedAutor == "Todos" ||
                    livro.autor.equals(state.selectedAutor, ignoreCase = true))

            // Lógica do Filtro de Categoria
            val categoriaOk = (state.selectedCategoria == "Todas" ||
                    livro.categoria.equals(state.selectedCategoria, ignoreCase = true))

            buscaOk && autorOk && categoriaOk
        }

        // 3. Atualiza o UiState
        _ui.value = _ui.value.copy(
            loading = false,
            livros = livrosFiltrados,
            autoresDisponiveis = autores,
            categoriasDisponiveis = categorias
        )
    }

    fun onSearchQueryChanged(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        _atualizarListasDaUi()
    }

    fun onAutorFilterChanged(autor: String) {
        _ui.value = _ui.value.copy(selectedAutor = autor)
        _atualizarListasDaUi()
    }

    fun onCategoriaFilterChanged(categoria: String) {
        _ui.value = _ui.value.copy(selectedCategoria = categoria)
        _atualizarListasDaUi()
    }

    // --- LÓGICA DE CRUD (Adicionar, Editar, Deletar) ---

    fun adicionarLivro(livro: Livro, onSucesso: () -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isSaving = true)
            repository.addLivro(livro).fold(
                onSuccess = {
                    // Recarrega tudo do Firestore para pegar o novo ID
                    // e atualizar a lista mestra.
                    carregarLivros()
                    onSucesso()
                },
                onFailure = { e ->
                    _ui.value = _ui.value.copy(isSaving = false, error = e.message ?: "Erro ao salvar")
                }
            )
        }
    }

    // --- NOVA FUNÇÃO DE EDITAR ---
    fun editarLivro(livroEditado: Livro, onSucesso: () -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isSaving = true)
            repository.editarLivro(livroEditado).fold(
                onSuccess = {
                    // Atualiza a lista mestra localmente (otimizado)
                    val index = listaMestraDeLivros.indexOfFirst { it.id == livroEditado.id }
                    if (index != -1) {
                        listaMestraDeLivros = listaMestraDeLivros.toMutableList().apply {
                            this[index] = livroEditado
                        }.sortedBy { it.titulo }
                    }
                    _atualizarListasDaUi() // Atualiza a UI com filtros
                    _ui.value = _ui.value.copy(isSaving = false)
                    onSucesso()
                },
                onFailure = { e ->
                    _ui.value = _ui.value.copy(isSaving = false, error = e.message ?: "Erro ao editar")
                }
            )
        }
    }

    fun deletarLivro(id: String, onSucesso: () -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isSaving = true)
            repository.deletarLivro(id).fold(
                onSuccess = {
                    // Remove da lista mestra localmente (otimizado)
                    listaMestraDeLivros = listaMestraDeLivros.filterNot { it.id == id }
                    _atualizarListasDaUi() // Atualiza a UI com filtros
                    _ui.value = _ui.value.copy(isSaving = false)
                    onSucesso()
                },
                onFailure = { e ->
                    _ui.value = _ui.value.copy(isSaving = false, error = e.message ?: "Erro ao deletar")
                }
            )
        }
    }

    // --- LÓGICA DE ATUALIZAÇÃO (Status, Favorito) ---

    fun atualizarStatus(id: String, novoStatus: String) {
        viewModelScope.launch {
            // Atualização otimista (primeiro local, depois remoto)
            val index = listaMestraDeLivros.indexOfFirst { it.id == id }
            if (index != -1) {
                listaMestraDeLivros = listaMestraDeLivros.toMutableList().apply {
                    this[index] = this[index].atualizarStatus(novoStatus)
                }
                _atualizarListasDaUi()
            }

            repository.atualizarStatus(id, novoStatus).fold(
                onSuccess = { /* ok */ },
                onFailure = { e ->
                    // Falhou, reverte
                    carregarLivros()
                    _ui.value = _ui.value.copy(error = e.message ?: "Erro ao atualizar status")
                }
            )
        }
    }

    fun marcarFavorito(id: String, favorito: Boolean) {
        viewModelScope.launch {
            // Atualização otimista
            val index = listaMestraDeLivros.indexOfFirst { it.id == id }
            if (index != -1) {
                listaMestraDeLivros = listaMestraDeLivros.toMutableList().apply {
                    this[index] = this[index].atualizarFavorito(favorito)
                }
                _atualizarListasDaUi()
            }

            repository.marcarFavorito(id, favorito).fold(
                onSuccess = { /* ok */ },
                onFailure = { e ->
                    // Falhou, reverte
                    carregarLivros()
                    _ui.value = _ui.value.copy(error = e.message)
                }
            )
        }
    }

    // Função de busca local, usada para a tela de Edição
    fun findLivroById(id: String): Livro? = listaMestraDeLivros.find { it.id == id }
}