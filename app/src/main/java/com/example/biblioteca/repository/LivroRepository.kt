package com.example.biblioteca.repository

import com.example.biblioteca.Model.Livro
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LivroRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Função para buscar livros no Firebase
    suspend fun getLivros(): List<Livro> {
        val snapshot = firestore.collection("livros").get().await()
        return snapshot.documents.map { document ->
            document.toObject(Livro::class.java)!! // Converte o documento para um objeto Livro
        }
    }

    // Função para atualizar o status de um livro no Firebase
    suspend fun atualizarStatus(id: String, novoStatus: String) {
        firestore.collection("livros")
            .document(id)
            .update("status", novoStatus)
            .await() // Aguarda a atualização no Firebase
    }
}
