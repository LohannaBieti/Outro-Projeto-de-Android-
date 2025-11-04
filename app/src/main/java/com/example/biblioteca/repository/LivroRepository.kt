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

            // Lógica para ler o favorito que será salvo
            val favorito = doc.getBoolean("favorito") ?: false

            when (tipo) {
                "fisico" -> LivroFisico(
                    id = id,
                    titulo = titulo,
                    autor = autor,
                    localizacaoPrateleira = doc.getString("localizacaoPrateleira") ?: "",
                    status = status,
                    // favorito = favorito // <-- Descomente QUANDO adicionar 'favorito' ao seu Model
                )
                "digital" -> LivroDigital(
                    id = id,
                    titulo = titulo,
                    autor = autor,
                    url = doc.getString("url") ?: "",
                    status = status,
                    // favorito = favorito // <-- Descomente QUANDO adicionar 'favorito' ao seu Model
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
        // Constrói o mapa de dados a ser salvo
        val dadosDoLivro: Map<String, Any> = when (livro) {
            is LivroFisico -> mapOf(
                "tipo" to "fisico", // Essencial para a leitura depois
                "titulo" to livro.titulo,
                "autor" to livro.autor,
                "status" to livro.status, // "Não lido" por padrão
                "localizacaoPrateleira" to livro.localizacaoPrateleira,
                "favorito" to false // Valor padrão
            )
            is LivroDigital -> mapOf(
                "tipo" to "digital", // Essencial para a leitura depois
                "titulo" to livro.titulo,
                "autor" to livro.autor,
                "status" to livro.status, // "Não lido" por padrão
                "url" to livro.url,
                "favorito" to false // Valor padrão
            )
        }

        // Adiciona os dados como um novo documento na coleção 'livros'
        collection.add(dadosDoLivro).await()
        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- NOVA FUNÇÃO DE DELETAR LIVRO ---
    /**
     * Deleta um livro do Firestore usando seu ID.
     */
    suspend fun deletarLivro(id: String): Result<Unit> = try {
        collection.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}