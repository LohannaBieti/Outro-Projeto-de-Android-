package com.example.biblioteca.navegacao

// --- IMPORTS (TODOS OS NECESSÁRIOS) ---
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.biblioteca.model.Livro
import com.example.biblioteca.model.LivroDigital
import com.example.biblioteca.model.LivroFisico
import com.example.biblioteca.model.LivroViewModel
// Importe o UiState do seu arquivo LivroViewModel
import com.example.biblioteca.model.UiState
import com.example.biblioteca.ui.theme.BibliotecaTheme


// --- NavegacaoApp ATUALIZADA ---
@Composable
fun NavegacaoApp() {
    val navController = rememberNavController()
    // O ViewModel é compartilhado por todas as telas do NavHost
    val viewModel: LivroViewModel = viewModel()

    NavHost(navController = navController, startDestination = "principal") {

        // --- TELA PRINCIPAL ---
        composable("principal") {
            val state by viewModel.ui.collectAsState()

            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate("adicionar_livro") }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Livro")
                    }
                }
            ) { paddingValues ->
                TelaPrincipal(
                    modifier = Modifier.padding(paddingValues),
                    state = state, // Passa o UiState inteiro
                    onItemClick = { id -> navController.navigate("detalhes/$id") },
                    onRefresh = { viewModel.carregarLivros() },
                    // Passa os novos eventos do ViewModel
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onAutorFilterChanged = { viewModel.onAutorFilterChanged(it) },
                    onCategoriaFilterChanged = { viewModel.onCategoriaFilterChanged(it) }
                )
            }
        }

        // --- TELA DE DETALHES (ATUALIZADA) ---
        composable(
            route = "detalhes/{livroId}",
            arguments = listOf(navArgument("livroId") { type = NavType.StringType })
        ) { backStackEntry ->
            val livroId = backStackEntry.arguments?.getString("livroId")
            val state by viewModel.ui.collectAsState()

            if (livroId == null) {
                TelaErro("Livro inválido")
            } else {
                val livro = viewModel.findLivroById(livroId)
                if (livro == null) {
                    TelaErro("Livro não encontrado")
                } else {
                    TelaDetalhes(
                        livro = livro,
                        isProcessing = state.isSaving,
                        onMarcarComoLido = { viewModel.atualizarStatus(livro.id, "Lido") },
                        onMarcarComoFavorito = { isFavorito ->
                            viewModel.marcarFavorito(livro.id, isFavorito)
                        },
                        onDeletar = {
                            viewModel.deletarLivro(livro.id) {
                                navController.popBackStack()
                            }
                        },
                        // --- NOVO EVENTO DE EDITAR ---
                        onEditar = {
                            navController.navigate("editar_livro/${livro.id}")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // --- TELA ADICIONAR LIVRO (USA O FORMULÁRIO) ---
        composable("adicionar_livro") {
            val state by viewModel.ui.collectAsState()

            TelaFormularioLivro(
                tituloTela = "Adicionar Novo Livro",
                livroInicial = null, // Começa em branco
                isSaving = state.isSaving,
                onSalvar = { novoLivro ->
                    viewModel.adicionarLivro(novoLivro) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // --- NOVA TELA DE EDITAR LIVRO ---
        composable(
            route = "editar_livro/{livroId}",
            arguments = listOf(navArgument("livroId") { type = NavType.StringType })
        ) { backStackEntry ->
            val livroId = backStackEntry.arguments?.getString("livroId")
            val state by viewModel.ui.collectAsState()

            val livroParaEditar = livroId?.let { viewModel.findLivroById(it) }

            if (livroParaEditar == null) {
                TelaErro(mensagem = "Livro não encontrado para edição.")
            } else {
                TelaFormularioLivro(
                    tituloTela = "Editar Livro",
                    livroInicial = livroParaEditar, // Pré-preenche o formulário
                    isSaving = state.isSaving,
                    onSalvar = { livroEditado ->
                        viewModel.editarLivro(livroEditado) {
                            navController.popBackStack() // Volta para a tela de detalhes
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// --- TelaPrincipal (COM BUSCA E FILTROS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(
    modifier: Modifier = Modifier,
    state: UiState, // Recebe o UiState completo
    onItemClick: (String) -> Unit,
    onRefresh: () -> Unit,
    // Novos callbacks
    onSearchQueryChanged: (String) -> Unit,
    onAutorFilterChanged: (String) -> Unit,
    onCategoriaFilterChanged: (String) -> Unit
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.loading) {
                CircularProgressIndicator()
            } else if (state.error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Erro ao carregar livros: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Text("Tentar Novamente")
                    }
                }
            } else {
                // --- NOVO: Coluna para Busca, Filtros e Lista ---
                Column(modifier = Modifier.fillMaxSize()) {

                    // --- BARRA DE BUSCA E FILTROS ---
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // Campo de Busca
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            label = { Text("Buscar por Título ou Autor") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        // Filtros Lado a Lado
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Filtro de Autor
                            FiltroDropDown(
                                label = "Autor",
                                selectedOption = state.selectedAutor,
                                options = state.autoresDisponiveis,
                                onOptionSelected = onAutorFilterChanged,
                                modifier = Modifier.weight(1f)
                            )
                            // Filtro de Categoria
                            FiltroDropDown(
                                label = "Categoria",
                                selectedOption = state.selectedCategoria,
                                options = state.categoriasDisponiveis,
                                onOptionSelected = onCategoriaFilterChanged,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // --- LISTA DE LIVROS ---
                    if (state.livros.isEmpty()) {
                        // Estado Vazio (considerando busca/filtro)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(24.dp)
                        ) {
                            Text(
                                "Nenhum livro encontrado",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tente ajustar sua busca ou filtros.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 12.dp, end = 12.dp, bottom = 80.dp // Padding para o FAB
                            )
                        ) {
                            items(state.livros) { livro ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable { onItemClick(livro.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = livro.titulo,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Autor: ${livro.autor}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        // --- NOVO: Mostra a Categoria ---
                                        Text(
                                            text = "Categoria: ${livro.categoria}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Status: ${livro.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- NOVO COMPOSABLE REUTILIZÁVEL PARA FILTROS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltroDropDown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}


// --- TelaDetalhes (COM BOTÃO EDITAR) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDetalhes(
    livro: Livro,
    isProcessing: Boolean,
    onMarcarComoLido: () -> Unit,
    onMarcarComoFavorito: (Boolean) -> Unit,
    onDeletar: () -> Unit,
    onEditar: () -> Unit, // <-- NOVO
    onBack: () -> Unit
) {
    val isFavorito = livro.favorito
    var mostrarDialogoDeletar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = livro.titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // --- Conteúdo dos Detalhes ---
            Column(
                modifier = Modifier.weight(1.0f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoLinha(label = "Autor", value = livro.autor)
                InfoLinha(label = "Status", value = livro.status)
                InfoLinha(label = "Categoria", value = livro.categoria) // <-- NOVO

                Spacer(Modifier.height(8.dp))

                when (livro) {
                    is LivroFisico -> {
                        InfoLinha(label = "Tipo", value = "Livro Físico")
                        InfoLinha(label = "Localização", value = livro.localizacaoPrateleira)
                    }
                    is LivroDigital -> {
                        InfoLinha(label = "Tipo", value = "Livro Digital")
                        InfoLinha(label = "URL", value = livro.url)
                    }
                }
            }

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // --- Botões de Ação ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMarcarComoLido,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = livro.status != "Lido" && !isProcessing
                ) {
                    Text(if (livro.status == "Lido") "Já lido" else "Marcar como Lido")
                }
                OutlinedButton(
                    onClick = { onMarcarComoFavorito(!isFavorito) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Text(if (isFavorito) "Remover dos Favoritos" else "Marcar como Favorito")
                }

                // --- NOVO: BOTÃO DE EDITAR ---
                OutlinedButton(
                    onClick = onEditar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Text("Editar Livro")
                }

                OutlinedButton(
                    onClick = { mostrarDialogoDeletar = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Text("Deletar Livro", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // --- Diálogo de Confirmação ---
    if (mostrarDialogoDeletar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDeletar = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza que deseja deletar permanentemente o livro '${livro.titulo}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoDeletar = false
                        onDeletar()
                    }
                ) {
                    Text("Deletar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoDeletar = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// --- Composable auxiliar InfoLinha ---
@Composable
private fun InfoLinha(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- Composable TelaErro ---
@Composable
fun TelaErro(mensagem: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = mensagem, color = MaterialTheme.colorScheme.error)
    }
}


// --- TelaAdicionarLivro REATORADA PARA TelaFormularioLivro ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFormularioLivro(
    tituloTela: String,
    livroInicial: Livro?, // null para "Adicionar", não-null para "Editar"
    isSaving: Boolean,
    onSalvar: (Livro) -> Unit,
    onBack: () -> Unit
) {
    // Estados do formulário, pré-preenchidos se 'livroInicial' não for null
    var titulo by remember { mutableStateOf(livroInicial?.titulo ?: "") }
    var autor by remember { mutableStateOf(livroInicial?.autor ?: "") }
    var categoria by remember { mutableStateOf(livroInicial?.categoria ?: "") }

    val tipoInicial = if (livroInicial is LivroFisico) "Físico" else "Digital"
    var tipoSelecionado by remember { mutableStateOf(tipoInicial) }

    val detalheInicial = when (livroInicial) {
        is LivroFisico -> livroInicial.localizacaoPrateleira
        is LivroDigital -> livroInicial.url
        else -> ""
    }
    var detalhe by remember { mutableStateOf(detalheInicial) }

    val tipos = listOf("Físico", "Digital")

    // A edição não permite mudar o tipo do livro (Físico/Digital)
    val isModoEdicao = livroInicial != null

    val isFormularioValido = titulo.isNotBlank() && autor.isNotBlank() && detalhe.isNotBlank() && categoria.isNotBlank()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tituloTela) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = autor,
                    onValueChange = { autor = it },
                    label = { Text("Autor") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoria (Ex: Ficção, Técnico)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                Spacer(Modifier.height(8.dp))

                Text("Tipo de Livro:", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.selectableGroup()) {
                    tipos.forEach { tipo ->
                        Row(
                            Modifier
                                .selectable(
                                    selected = (tipo == tipoSelecionado),
                                    onClick = { if (!isModoEdicao) tipoSelecionado = tipo },
                                    role = Role.RadioButton,
                                    enabled = !isSaving && !isModoEdicao // Desabilita se estiver salvando OU editando
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (tipo == tipoSelecionado),
                                onClick = null,
                                enabled = !isSaving && !isModoEdicao
                            )
                            Text(text = tipo, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                val labelDetalhe = if (tipoSelecionado == "Físico") "Localização (Prateleira)" else "URL"
                OutlinedTextField(
                    value = detalhe,
                    onValueChange = { detalhe = it },
                    label = { Text(labelDetalhe) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                Spacer(Modifier.weight(1.0f))

                Button(
                    onClick = {
                        // Constrói o livro novo ou editado
                        val livroParaSalvar = if (tipoSelecionado == "Físico") {
                            (livroInicial as? LivroFisico ?: LivroFisico()).copy(
                                id = livroInicial?.id ?: "", // Mantém o ID original se estiver editando
                                titulo = titulo,
                                autor = autor,
                                categoria = categoria,
                                localizacaoPrateleira = detalhe
                            )
                        } else {
                            (livroInicial as? LivroDigital ?: LivroDigital()).copy(
                                id = livroInicial?.id ?: "", // Mantém o ID original se estiver editando
                                titulo = titulo,
                                autor = autor,
                                categoria = categoria,
                                url = detalhe
                            )
                        }
                        onSalvar(livroParaSalvar)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormularioValido && !isSaving
                ) {
                    Text(if (isModoEdicao) "Salvar Alterações" else "Adicionar Livro")
                }
            }

            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}


// --- Previews (Atualizados) ---

@Preview(showBackground = true, name = "Tela Principal (Lista)")
@Composable
fun PreviewTelaPrincipalComLista() {
    val livrosMock = listOf(
        LivroFisico(id = "1", titulo = "O Senhor dos Anéis", autor = "J.R.R. Tolkien", categoria = "Fantasia", localizacaoPrateleira = "A-12", status = "Lido"),
        LivroDigital(id = "2", titulo = "Duna", autor = "Frank Herbert", categoria = "Ficção Científica", url = "http://...", status = "Lendo"),
        LivroFisico(id = "3", titulo = "A Fundação", autor = "Isaac Asimov", categoria = "Ficção Científica", localizacaoPrateleira = "B-03", status = "Não lido")
    )
    val stateMock = UiState(
        livros = livrosMock,
        autoresDisponiveis = listOf("Todos", "J.R.R. Tolkien", "Frank Herbert", "Isaac Asimov"),
        categoriasDisponiveis = listOf("Todas", "Fantasia", "Ficção Científica")
    )

    BibliotecaTheme {
        TelaPrincipal(
            state = stateMock,
            onItemClick = {},
            onRefresh = {},
            onSearchQueryChanged = {},
            onAutorFilterChanged = {},
            onCategoriaFilterChanged = {}
        )
    }
}

@Preview(showBackground = true, name = "Tela Detalhes")
@Composable
fun PreviewTelaDetalhes() {
    BibliotecaTheme {
        val livroMock = LivroFisico(id = "1", titulo = "O Poder do Agora", autor = "Eckhart Tolle", categoria = "Autoajuda", localizacaoPrateleira = "M-22", status = "Não lido")
        TelaDetalhes(
            livro = livroMock,
            isProcessing = false,
            onMarcarComoLido = {},
            onMarcarComoFavorito = {},
            onDeletar = {},
            onEditar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Tela Formulário (Adicionar)")
@Composable
fun PreviewTelaFormularioAdicionar() {
    BibliotecaTheme {
        TelaFormularioLivro(
            tituloTela = "Adicionar Livro",
            livroInicial = null,
            isSaving = false,
            onSalvar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Tela Formulário (Editar)")
@Composable
fun PreviewTelaFormularioEditar() {
    BibliotecaTheme {
        TelaFormularioLivro(
            tituloTela = "Editar Livro",
            livroInicial = LivroFisico(id = "1", titulo = "O Poder do Agora", autor = "Eckhart Tolle", categoria = "Autoajuda", localizacaoPrateleira = "M-22"),
            isSaving = false,
            onSalvar = {},
            onBack = {}
        )
    }
}