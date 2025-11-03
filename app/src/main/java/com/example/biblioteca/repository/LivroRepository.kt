package com.example.biblioteca.repository

import com.example.biblioteca.model.Livro
import com.example.biblioteca.model.LivroDigital
import com.example.biblioteca.model.LivroFisico
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LivroRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("livros")

    suspend fun getLivros(): Result<List<Livro>> = try {
        val snapshot = collection.get().await()
        val list = snapshot.documents.mapNotNull { doc ->
            val tipo = doc.getString("tipo") ?: return@mapNotNull null
            val id = doc.id
            val titulo = doc.getString("titulo") ?: ""
            val autor = doc.getString("autor") ?: ""
            val status = doc.getString("status") ?: "Não lido"
            when (tipo) {
                "fisico" -> LivroFisico(
                    id = id,
                    titulo = titulo,
                    autor = autor,
                    localizacaoPrateleira = doc.getString("localizacaoPrateleira") ?: "",
                    status = status
                )
                "digital" -> LivroDigital(
                    id = id,
                    titulo = titulo,
                    autor = autor,
                    url = doc.getString("url") ?: "",
                    status = status
                )
                else -> null
            }
        }
        Result.success(list)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun atualizarStatus(id: String, novoStatus: String): Result<Unit> = try {
        collection.document(id).update("status", novoStatus).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun marcarFavorito(id: String, favorito: Boolean): Result<Unit> = try {
        collection.document(id).update("favorito", favorito).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
