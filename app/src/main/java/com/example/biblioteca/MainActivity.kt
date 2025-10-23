package com.example.biblioteca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.example.biblioteca.Model.Livro
import com.example.biblioteca.ui.theme.BibliotecaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BibliotecaTheme {
                // Criando uma instância de Livro
                val livro = Livro("1", "Livro Exemplo", "Autor Exemplo", "Ficção", "Não lido")
                val livroAtualizado = livro.atualizarStatus("Lido")

                // Mostrar status atualizado
                Text(text = "Status do Livro: ${livroAtualizado.status}")
            }
        }
    }
}
