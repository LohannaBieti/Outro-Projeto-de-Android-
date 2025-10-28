package com.example.biblioteca.navegacao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblioteca.model.Livro
import com.example.biblioteca.model.LivroViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.navArgument

import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons

@Composable
fun NavegacaoApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "principal") {
        composable("principal") {
            val viewModel: LivroViewModel = viewModel()
            TelaPrincipal(
                livros = viewModel.livros,
                onItemClick = { id -> navController.navigate("detalhes/$id") }
            )
        }
        composable(
            route = "detalhes/{livroId}",
            arguments = listOf(navArgument("livroId") { type = NavType.StringType })
        ) { backStackEntry ->
            val livroId = backStackEntry.arguments?.getString("livroId")
            val viewModel: LivroViewModel = viewModel()
            if (livroId == null) {
                // rota inválida
                TelaErro("Livro inválido")
            } else {
                val livro = viewModel.findLivroById(livroId)
                if (livro == null) {
                    TelaErro("Livro não encontrado")
                } else {
                    TelaDetalhes(
                        livro = livro,
                        onMarcarComoLido = { viewModel.atualizarStatus(livro.id, "Lido") },
                        onMarcarComoFavorito = { /* implemente favorito */ },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun TelaPrincipal(livros: List<Livro>, onItemClick: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        if (livros.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Carregando livros...")
            }
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
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDetalhes(
    livro: Livro,
    onMarcarComoLido: () -> Unit,
    onMarcarComoFavorito: () -> Unit,
    onBack: () -> Unit
) {
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
            Text(text = "Categoria: ${livro.categoria}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onMarcarComoLido, modifier = Modifier.fillMaxWidth()) {
                Text("Marcar como Lido")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onMarcarComoFavorito, modifier = Modifier.fillMaxWidth()) {
                Text("Marcar como Favorito")
            }
        }
    }
}


@Composable
fun TelaErro(mensagem: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = mensagem)
    }
}
