package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.FirestoreProvider
import com.example.data.model.UserData
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirestoreProvider.auth
    private val db = FirestoreProvider.get(context)
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    fun attemptAutoSignIn(
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        if (auth.currentUser != null) {
            onSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Web Client ID missing: ${e.message}")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onSuccess()
                } else {
                    onError("Unexpected credential format received.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("AuthRepository", "Google Sign-In cancelled: ${e.message}")
                onCancelled()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign-in failed")
            }
        }
    }

    suspend fun registerUserProfile(
        fullName: String,
        gender: String,
        dob: String,
        termsAccepted: Boolean
    ): Result<UserData> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (!termsAccepted) return Result.failure(IllegalArgumentException("You must accept Terms of Service"))

        val userDoc = db.collection("users").document(user.uid)
        val userData = UserData(
            id = user.uid,
            userId = user.uid,
            fullName = fullName.ifBlank { user.displayName ?: "SClab User" },
            email = user.email ?: "",
            gender = gender,
            dob = dob,
            role = "user",
            isEmailVerified = user.isEmailVerified,
            isSuspended = false,
            isDeleted = false,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        return try {
            userDoc.set(userData, SetOptions.merge()).await()
            Result.success(userData)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to save user profile", e)
            Result.failure(e)
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
        auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to clear credential state", e)
            } finally {
                onComplete()
            }
        }
    }
}
