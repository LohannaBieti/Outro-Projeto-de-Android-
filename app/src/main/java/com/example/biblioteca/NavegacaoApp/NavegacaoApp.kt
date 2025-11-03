package com.example.biblioteca.navegacao

// --- IMPORTS (Completos) ---
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // <-- Import principal
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.biblioteca.model.Livro
import com.example.biblioteca.model.LivroDigital
import com.example.biblioteca.model.LivroFisico
import com.example.biblioteca.model.LivroViewModel
import com.example.biblioteca.ui.theme.BibliotecaTheme


// --- NavegacaoApp ATUALIZADA ---
@Composable
fun NavegacaoApp() {
    val navController = rememberNavController()

    // --- MUDANÇA ---
    // O ViewModel é inicializado AQUI, uma única vez, no escopo do NavHost.
    // Todas as telas que o usarem receberão esta mesma instância.
    val viewModel: LivroViewModel = viewModel()

    NavHost(navController = navController, startDestination = "principal") {

        // --- TELA PRINCIPAL (MODIFICADA) ---
        composable("principal") {
            // --- MUDANÇA ---
            // Não criamos um novo VM. Apenas lemos o state do VM compartilhado.
            // val viewModel: LivroViewModel = viewModel() // <-- LINHA ANTIGA REMOVIDA
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
                    loading = state.loading,
                    livros = state.livros,
                    error = state.error,
                    onItemClick = { id -> navController.navigate("detalhes/$id") },
                    onRefresh = { viewModel.carregarLivros() } // Passa a função do VM compartilhado
                )
            }
        }

        // --- TELA DE DETALHES (MODIFICADA) ---
        composable(
            route = "detalhes/{livroId}",
            arguments = listOf(navArgument("livroId") { type = NavType.StringType })
        ) { backStackEntry ->
            val livroId = backStackEntry.arguments?.getString("livroId")

            // --- MUDANÇA ---
            // Não criamos um novo VM. Usamos o VM compartilhado.
            // val viewModel: LivroViewModel = viewModel() // <-- LINHA ANTIGA REMOVIDA

            if (livroId == null) {
                TelaErro("Livro inválido")
            } else {
                // Busca o livro direto do estado atual do VM compartilhado
                val livro = viewModel.findLivroById(livroId)
                if (livro == null) {
                    TelaErro("Livro não encontrado")
                } else {
                    TelaDetalhes(
                        livro = livro,
                        onMarcarComoLido = { viewModel.atualizarStatus(livro.id, "Lido") },
                        onMarcarComoFavorito = { isFavorito ->
                            viewModel.marcarFavorito(livro.id, isFavorito)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // --- NOVA TELA (ADICIONAR LIVRO) (MODIFICADA) ---
        composable("adicionar_livro") {
            // --- MUDANÇA ---
            // Não criamos um novo VM. Usamos o VM compartilhado.
            // val viewModel: LivroViewModel = viewModel() // <-- LINHA ANTIGA REMOVIDA
            val state by viewModel.ui.collectAsState()

            TelaAdicionarLivro(
                isSaving = state.isSaving,
                onSalvar = { novoLivro ->
                    // Chama a função no VM compartilhado
                    viewModel.adicionarLivro(novoLivro) {
                        // onSucesso: Navega de volta
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// --- TelaPrincipal (Sem mudanças internas) ---
@Composable
fun TelaPrincipal(
    modifier: Modifier = Modifier,
    loading: Boolean,
    livros: List<Livro>,
    error: String?,
    onItemClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Erro ao carregar livros: $error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Text("Tentar Novamente")
                    }
                }
            } else if (livros.isEmpty()) {
                Text("Nenhum livro encontrado.")
            } else {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    items(livros) { livro ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { onItemClick(livro.id) },
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = livro.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(text = "Autor: ${livro.autor}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(text = "Status: ${livro.status}", style = MaterialTheme.typography.bodySmall)
                                val detalheEspecifico = when (livro) {
                                    is LivroFisico -> "Prateleira: ${livro.localizacaoPrateleira}"
                                    is LivroDigital -> "Formato: Digital"
                                }
                                Text(text = detalheEspecifico, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TelaDetalhes (Sem mudanças internas) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDetalhes(
    livro: Livro,
    onMarcarComoLido: () -> Unit,
    onMarcarComoFavorito: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var isFavorito by remember { mutableStateOf(false) } // Lembre-se de corrigir isso (Bug #2)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = livro.titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(text = "Título: ${livro.titulo}", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(text = "Autor: ${livro.autor}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = "Status: ${livro.status}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            when (livro) {
                is LivroFisico -> {
                    Text("Tipo: Livro Físico", style = MaterialTheme.typography.titleMedium)
                    Text("Localização: ${livro.localizacaoPrateleira}", style = MaterialTheme.typography.bodySmall)
                }
                is LivroDigital -> {
                    Text("Tipo: Livro Digital", style = MaterialTheme.typography.titleMedium)
                    Text("URL: ${livro.url}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onMarcarComoLido,
                modifier = Modifier.fillMaxWidth(),
                enabled = livro.status != "Lido"
            ) {
                Text(if (livro.status == "Lido") "Já lido" else "Marcar como Lido")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    isFavorito = !isFavorito
                    onMarcarComoFavorito(isFavorito)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isFavorito) "Remover dos Favoritos" else "Marcar como Favorito")
            }
        }
    }
}

// --- TelaErro (Sem mudanças) ---
@Composable
fun TelaErro(mensagem: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = mensagem, color = MaterialTheme.colorScheme.error)
    }
}


// --- TelaAdicionarLivro (Sem mudanças internas) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAdicionarLivro(
    isSaving: Boolean,
    onSalvar: (Livro) -> Unit,
    onBack: () -> Unit
) {
    // Estados locais para os campos de texto
    var titulo by remember { mutableStateOf("") }
    var autor by remember { mutableStateOf("") }
    var detalhe by remember { mutableStateOf("") } // Localização ou URL

    // Estado para o seletor de tipo
    val tipos = listOf("Físico", "Digital")
    var tipoSelecionado by remember { mutableStateOf(tipos[0]) } // Padrão é "Físico"

    // Validação simples para habilitar o botão Salvar
    val isFormularioValido = titulo.isNotBlank() && autor.isNotBlank() && detalhe.isNotBlank()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Adicionar Novo Livro") },
                navigationIcon = {
                    // Botão de voltar (só funciona se não estiver salvando)
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
                    .padding(16.dp)
            ) {
                // Campo Título
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Spacer(Modifier.height(8.dp))

                // Campo Autor
                OutlinedTextField(
                    value = autor,
                    onValueChange = { autor = it },
                    label = { Text("Autor") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Spacer(Modifier.height(16.dp))

                // Seletor de Tipo (Radio Buttons)
                Text("Tipo de Livro:", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.selectableGroup()) {
                    tipos.forEach { tipo ->
                        Row(
                            Modifier
                                .selectable(
                                    selected = (tipo == tipoSelecionado),
                                    onClick = { tipoSelecionado = tipo },
                                    role = Role.RadioButton,
                                    enabled = !isSaving
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (tipo == tipoSelecionado),
                                onClick = null, // null pq o 'selectable' já cuida disso
                                enabled = !isSaving
                            )
                            Text(text = tipo, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Campo de Detalhe (muda o label)
                val labelDetalhe = if (tipoSelecionado == "Físico") "Localização (Prateleira)" else "URL"
                OutlinedTextField(
                    value = detalhe,
                    onValueChange = { detalhe = it },
                    label = { Text(labelDetalhe) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                Spacer(Modifier.height(32.dp))

                // Botão Salvar
                Button(
                    onClick = {
                        // Cria o objeto Livro correto e chama o onSalvar
                        val novoLivro = if (tipoSelecionado == "Físico") {
                            LivroFisico(titulo = titulo, autor = autor, localizacaoPrateleira = detalhe)
                        } else {
                            LivroDigital(titulo = titulo, autor = autor, url = detalhe)
                        }
                        onSalvar(novoLivro)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    // Desabilitado se formulário for inválido OU se estiver salvando
                    enabled = isFormularioValido && !isSaving
                ) {
                    Text("Salvar Livro")
                }
            }

            // Overlay de Loading
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}


// --- Previews (Sem mudanças) ---

@Preview(showBackground = true, name = "Tela Principal (Lista)")
@Composable
fun PreviewTelaPrincipalComLista() {
    val livrosMock = listOf(
        LivroFisico(id = "1", titulo = "O Senhor dos Anéis", autor = "J.R.R. Tolkien", localizacaoPrateleira = "A-12", status = "Lido"),
        LivroDigital(id = "2", titulo = "Duna", autor = "Frank Herbert", url = "http://...", status = "Lendo"),
        LivroFisico(id = "3", titulo = "A Fundação", autor = "Isaac Asimov", localizacaoPrateleira = "B-03", status = "Não lido")
    )
    BibliotecaTheme {
        // Envolve no Scaffold para ver o FAB
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Livro")
                }
            }
        ) { padding ->
            TelaPrincipal(
                modifier = Modifier.padding(padding),
                loading = false,
                livros = livrosMock,
                error = null,
                onItemClick = {},
                onRefresh = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Tela Adicionar Livro")
@Composable
fun PreviewTelaAdicionarLivro() {
    BibliotecaTheme {
        TelaAdicionarLivro(
            isSaving = false,
            onSalvar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Tela Adicionar Livro (Salvando)")
@Composable
fun PreviewTelaAdicionarLivroSalvando() {
    BibliotecaTheme {
        TelaAdicionarLivro(
            isSaving = true, // <-- Mostra o loading
            onSalvar = {},
            onBack = {}
        )
    }
}