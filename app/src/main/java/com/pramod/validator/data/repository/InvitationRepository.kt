package com.pramod.validator.data.repository

import com.pramod.validator.data.models.Invitation
import com.pramod.validator.data.FirestoreCollections
import com.pramod.validator.services.EmailService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class InvitationRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val emailService = EmailService()

    suspend fun createInvitation(invitation: Invitation): Result<Invitation> {
        return try {
            android.util.Log.d("InvitationRepository", "Creating invitation for: ${invitation.email}")
            
            val invitationWithToken = invitation.copy(
                id = UUID.randomUUID().toString(),
                token = UUID.randomUUID().toString()
            )
            
            android.util.Log.d("InvitationRepository", "Generated token: ${invitationWithToken.token}")
            
            // Save invitation to Firestore
            android.util.Log.d("InvitationRepository", "Saving to Firestore...")
            firestore.collection(FirestoreCollections.INVITATIONS)
                .document(invitationWithToken.id)
                .set(invitationWithToken)
                .await()
            
            android.util.Log.d("InvitationRepository", "Saved to Firestore successfully")
            
            // Send invitation email
            android.util.Log.d("InvitationRepository", "Sending email...")
            val invitationLink = getInvitationLink(invitationWithToken.token)
            android.util.Log.d("InvitationRepository", "Invitation link: $invitationLink")
            
            try {
                val emailResult = emailService.sendInvitationEmail(
                    toEmail = invitation.email,
                    recipientName = invitation.displayName,
                    enterpriseName = "Your Enterprise", // You might want to get this from the enterprise data
                    invitationLink = invitationLink,
                    invitedBy = "Enterprise Admin" // You might want to get this from the current user
                )
                
                if (emailResult.isSuccess) {
                    android.util.Log.d("InvitationRepository", "Invitation created and email sent successfully")
                } else {
                    android.util.Log.e("InvitationRepository", "Invitation created but email failed: ${emailResult.exceptionOrNull()?.message}")
                    // Still continue - invitation is created even if email fails
                }
            } catch (e: Exception) {
                android.util.Log.e("InvitationRepository", "Email service error: ${e.message}")
                // Continue anyway - invitation is created
            }
            
            android.util.Log.d("InvitationRepository", "Invitation created successfully")
            Result.success(invitationWithToken)
        } catch (e: Exception) {
            android.util.Log.e("InvitationRepository", "Error creating invitation", e)
            Result.failure(e)
        }
    }
    
    private fun getInvitationLink(token: String): String {
        // Use custom scheme for better mobile experience
        return "validator://invite?token=$token"
    }

    suspend fun getInvitationByToken(token: String): Result<Invitation?> {
        return try {
            val snapshot = firestore.collection("invitations")
                .whereEqualTo("token", token)
                .whereEqualTo("isUsed", false)
                .get()
                .await()
            
            val invitation = snapshot.documents.firstOrNull()?.toObject(Invitation::class.java)
            Result.success(invitation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markInvitationAsUsed(token: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("invitations")
                .whereEqualTo("token", token)
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents.first()
                val invitation = doc.toObject(Invitation::class.java)
                
                doc.reference.update(
                    "isUsed", true,
                    "usedAt", System.currentTimeMillis()
                ).await()
                
                // Send welcome email
                if (invitation != null) {
                    emailService.sendWelcomeEmail(
                        toEmail = invitation.email,
                        recipientName = invitation.displayName,
                        enterpriseName = "Your Enterprise" // You might want to get this from the enterprise data
                    )
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvitationsByEnterprise(enterpriseId: String): Result<List<Invitation>> {
        return try {
            val snapshot = firestore.collection("invitations")
                .whereEqualTo("enterpriseId", enterpriseId)
                .get()
                .await()
            
            val invitations = snapshot.documents.mapNotNull { it.toObject(Invitation::class.java) }
            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInvitation(invitationId: String): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.INVITATIONS)
                .document(invitationId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
