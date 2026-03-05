package com.example.domus.data.repository

import com.example.domus.data.model.Family
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.*

data class MemberInfo(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val photoUrl: String? = null
)

class Repo_Familia {
    private val db = FirebaseFirestore.getInstance()
    private val familiesCollection = db.collection("families")
    private val usersCollection = db.collection("users")

    private val CODE_EXPIRATION_MILLIS = 15 * 60 * 1000 // 15 minutes

    suspend fun createFamily(name: String, adminId: String): Result<String> {
        return try {
            val familyId = familiesCollection.document().id
            val family = Family(
                id = familyId,
                name = name,
                adminId = adminId,
                joinCode = "", // No code initially
                codeCreatedAt = 0L,
                members = listOf(adminId)
            )
            familiesCollection.document(familyId).set(family).await()
            
            val userData = mapOf("familyId" to familyId)
            usersCollection.document(adminId).set(userData, SetOptions.merge()).await()
            
            Result.success(familyId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun regenerateCode(familyId: String): Result<String> {
        return try {
            val newCode = generateJoinCode()
            val timestamp = System.currentTimeMillis()
            familiesCollection.document(familyId).update(
                "joinCode", newCode,
                "codeCreatedAt", timestamp
            ).await()
            Result.success(newCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinFamilyRequest(userId: String, joinCode: String): Result<Unit> {
        return try {
            if (joinCode.isEmpty()) return Result.failure(Exception("Código no válido"))

            val query = familiesCollection.whereEqualTo("joinCode", joinCode).get().await()
            if (query.isEmpty) {
                return Result.failure(Exception("Código de familia no válido"))
            }
            
            val familyDoc = query.documents[0]
            val family = familyDoc.toObject(Family::class.java) ?: return Result.failure(Exception("Error al leer familia"))

            val now = System.currentTimeMillis()
            if (family.codeCreatedAt == 0L || now - family.codeCreatedAt > CODE_EXPIRATION_MILLIS) {
                return Result.failure(Exception("El código ha expirado o no existe. Pide uno nuevo al administrador."))
            }

            if (userId in family.pendingMembers || userId in family.members) {
                return Result.failure(Exception("Ya estás en la familia o tienes una solicitud pendiente"))
            }

            familiesCollection.document(family.id).update("pendingMembers", family.pendingMembers + userId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptMember(familyId: String, userId: String): Result<Unit> {
        return try {
            val familyDoc = familiesCollection.document(familyId).get().await()
            val members = familyDoc.get("members") as? List<String> ?: emptyList()
            val pendingMembers = familyDoc.get("pendingMembers") as? List<String> ?: emptyList()

            familiesCollection.document(familyId).update(
                "members", members + userId,
                "pendingMembers", pendingMembers - userId
            ).await()
            
            val userData = mapOf("familyId" to familyId)
            usersCollection.document(userId).set(userData, SetOptions.merge()).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFamilyByUserId(userId: String): Result<Family?> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) return Result.success(null)
            
            val familyId = userDoc.getString("familyId") ?: return Result.success(null)
            val doc = familiesCollection.document(familyId).get().await()
            Result.success(doc.toObject(Family::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMembersDetails(userIds: List<String>): List<MemberInfo> {
        if (userIds.isEmpty()) return emptyList()
        return try {
            val query = usersCollection.whereIn(FieldPath.documentId(), userIds).get().await()
            query.documents.mapNotNull { doc ->
                MemberInfo(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: doc.getString("Nombre") ?: "Usuario",
                    email = doc.getString("email") ?: doc.getString("Email") ?: "",
                    photoUrl = doc.getString("photoUrl")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}