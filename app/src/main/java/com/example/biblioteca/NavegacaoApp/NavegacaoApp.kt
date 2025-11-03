package com.example.biblioteca.navegacao

// Imports atualizados
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.biblioteca.model.Livro // <<< MUDANÇA (Importa a sealed class)
import com.example.biblioteca.model.LivroDigital // <<< MUDANÇA
import com.example.biblioteca.model.LivroFisico // <<< MUDANÇA
import com.example.biblioteca.model.LivroViewModel

@Composable
fun NavegacaoApp() {
    val navController = rememberNavController()
    // O ViewModel pode ser pego aqui se for compartilhado,
    // mas pegar dentro da rota ("principal" e "detalhes") garante o escopo correto.

    NavHost(navController = navController, startDestination = "principal") {

        // --- TELA PRINCIPAL ---
        composable("principal") {
            val viewModel: LivroViewModel = viewModel()
            // <<< MUDANÇA: Coleta o UiState do StateFlow
            val state by viewModel.ui.collectAsState()

            TelaPrincipal(
                loading = state.loading,
                livros = state.livros,
                error = state.error,
                onItemClick = { id -> navController.navigate("detalhes/$id") },
                onRefresh = { viewModel.carregarLivros() } // Passa a função de recarregar
            )
        }

        // --- TELA DE DETALHES ---
        composable(
            route = "detalhes/{livroId}",
            arguments = listOf(navArgument("livroId") { type = NavType.StringType })
        ) { backStackEntry ->
            val livroId = backStackEntry.arguments?.getString("livroId")

            // <<< MUDANÇA: Pega o mesmo ViewModel que a TelaPrincipal
            val viewModel: LivroViewModel = viewModel()

            if (livroId == null) {
                TelaErro("Livro inválido")
            } else {
                // Busca o livro direto do estado atual do ViewModel
                val livro = viewModel.findLivroById(livroId)
                if (livro == null) {
                    // Isso pode acontecer se o VM estiver carregando ou o ID for ruim
                    TelaErro("Livro não encontrado")
                } else {
                    TelaDetalhes(
                        livro = livro,
                        onMarcarComoLido = { viewModel.atualizarStatus(livro.id, "Lido") },
                        // <<< MUDANÇA: Ação de favorito agora passa um booleano
                        onMarcarComoFavorito = { isFavorito ->
                            viewModel.marcarFavorito(livro.id, isFavorito)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// --- COMPOSABLE TelaPrincipal ATUALIZADO ---
@Composable
fun TelaPrincipal(
    loading: Boolean,
    livros: List<Livro>,
    error: String?,
    onItemClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // <<< MUDANÇA: Lógica para exibir Loading, Erro ou Conteúdo

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
                // A lista de livros (mesma lógica de antes)
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

                                // <<< MUDANÇA: Polimorfismo no Card da lista
                                val detalheEspecifico = when (livro) {
                                    is LivroFisico -> "Prateleira: ${livro.localizacaoPrateleira}"
                                    is LivroDigital -> "Formato: Digital" // ou livro.url se preferir
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

// --- COMPOSABLE TelaDetalhes ATUALIZADO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDetalhes(
    livro: Livro,
    onMarcarComoLido: () -> Unit,
    onMarcarComoFavorito: (Boolean) -> Unit, // <<< MUDANÇA
    onBack: () -> Unit
) {
    // Estado local simples para controlar o botão de favorito
    var isFavorito by remember { mutableStateOf(false) }

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

            // <<< MUDANÇA: Polimorfismo para exibir detalhes específicos
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
            // --- Fim da mudança de polimorfismo ---

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onMarcarComoLido,
                modifier = Modifier.fillMaxWidth(),
                // Desabilita o botão se já estiver lido
                enabled = livro.status != "Lido"
            ) {
                Text(if (livro.status == "Lido") "Já lido" else "Marcar como Lido")
            }

            Spacer(Modifier.height(8.dp))

            // <<< MUDANÇA: Botão de favorito com estado
            OutlinedButton(
                onClick = {
                    isFavorito = !isFavorito // Inverte o estado local
                    onMarcarComoFavorito(isFavorito) // Envia a ação para o ViewModel
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isFavorito) "Remover dos Favoritos" else "Marcar como Favorito")
            }
        }
    }
}

// --- COMPOSABLE TelaErro (Sem mudanças) ---
@Composable
fun TelaErro(mensagem: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = mensagem, color = MaterialTheme.colorScheme.error)
    }
}