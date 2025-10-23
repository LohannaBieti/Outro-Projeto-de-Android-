package com.example.biblioteca.NavegacaoApp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.biblioteca.Model.Livro
import com.example.biblioteca.ui.theme.BibliotecaTheme

// Simulação de dados dos livros (idealmente, isso seria do ViewModel)
val livros = listOf(
    Livro("1", "Livro 1", "Autor A", "Físico", "Não Lido"),
    Livro("2", "Livro 2", "Autor B", "Digital", "Lido")
)

@Composable
fun NavegacaoApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "principal") {
        composable("principal") {
            TelaPrincipal(navController)
        }
        composable("detalhes/{livroId}") { backStackEntry ->
            val livroId = backStackEntry.arguments?.getString("livroId")
            livroId?.let {
                val livro = livros.find { it.id == livroId }
                livro?.let {
                    TelaDetalhes(livro = it, onMarcarComoLido = { /* Atualizar status */ }, onMarcarComoFavorito = { /* Atualizar status */ })
                }
            }
        }
    }
}

@Composable
fun TelaPrincipal(navController: NavController) {
    Column {
        livros.forEach { livro ->
            Button(onClick = {
                navController.navigate("detalhes/${livro.id}")
            }) {
                Text(text = livro.titulo)
            }
        }
    }
}

@Composable
fun TelaDetalhes(livro: Livro, onMarcarComoLido: () -> Unit, onMarcarComoFavorito: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Usando headlineLarge para o título do livro
        Text(text = "Título: ${livro.titulo}", style = MaterialTheme.typography.headlineLarge)

        // Usando bodyMedium para o autor
        Text(text = "Autor: ${livro.autor}", style = MaterialTheme.typography.bodyMedium)

        // Usando bodySmall para a categoria
        Text(text = "Categoria: ${livro.categoria}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onMarcarComoLido) {
            Text("Marcar como Lido")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onMarcarComoFavorito) {
            Text("Marcar como Favorito")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BibliotecaTheme {
        NavegacaoApp()
    }
}
