package com.example.biblioteca.repository

import com.example.biblioteca.model.Livro
import com.example.biblioteca.model.LivroDigital
import com.example.biblioteca.model.LivroFisico
import com.example.biblioteca.model.toMap
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
            val favorito = doc.getBoolean("favorito") ?: false
            val categoria = doc.getString("categoria") ?: "Sem Categoria" // <-- NOVO

            when (tipo) {
                "fisico" -> LivroFisico(
                    id = id,
                    titulo = titulo,
                    autor = autor,
                    localizacaoPrateleira = doc.getString("localizacaoPrateleira") ?: "",
                    status = status,
                    favorito = favorito,
                    categoria = categoria // <-- NOVO
                )
                "digital" -> LivroDigital(
                    id = id,
                    titulo = titulo,
                    autor = autor,
                    url = doc.getString("url") ?: "",
                    status = status,
                    favorito = favorito,
                    categoria = categoria // <-- NOVO
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

    suspend fun addLivro(livro: Livro): Result<Unit> = try {
        // Usa a nova função de extensão .toMap()
        collection.add(livro.toMap()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- NOVA FUNÇÃO DE EDITAR LIVRO ---
    suspend fun editarLivro(livro: Livro): Result<Unit> = try {
        // Usa a nova função de extensão .toMap()
        // O .update() é mais seguro que o .set() pois não sobrescreve
        // campos que não estejam no mapa.
        collection.document(livro.id).update(livro.toMap()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deletarLivro(id: String): Result<Unit> = try {
        collection.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}