package com.nexapos.retail.data.branch

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nexapos.retail.domain.branch.RemoteStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore + Auth implementation of [RemoteStore]. Builds a NAMED Firebase app
 * ("nexapos-mb") from the owner's [FirebaseConfig.Config] — never the default app
 * — so an install without multi-branch configured never initialises Firebase.
 * The app, Auth and Firestore handles are created lazily on first use; Firestore's
 * on-device cache (enabled by default) gives offline reads and queued writes.
 */
class FirestoreRemoteStore(
    private val appContext: Context,
    private val config: FirebaseConfig.Config,
) : RemoteStore {
    private val app: FirebaseApp by lazy {
        FirebaseApp.getApps(appContext).firstOrNull { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(
                appContext,
                FirebaseOptions.Builder()
                    .setProjectId(config.projectId)
                    .setApplicationId(config.appId)
                    .setApiKey(config.apiKey)
                    .build(),
                APP_NAME,
            )
    }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance(app) }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance(app) }

    @Suppress("TooGenericExceptionCaught") // any sign-in failure → fall through to create-if-new
    override suspend fun signIn(
        email: String,
        password: String,
        createIfNew: Boolean,
    ): String {
        val result =
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                if (createIfNew) auth.createUserWithEmailAndPassword(email, password).await() else throw e
            }
        return result.user?.uid ?: error("Firebase returned no uid after sign-in")
    }

    override fun signedInUid(): String? = auth.currentUser?.uid

    override fun signOut() = auth.signOut()

    override suspend fun setDoc(
        path: String,
        data: Map<String, Any?>,
    ) {
        db.document(path).set(data, SetOptions.merge()).await()
    }

    override suspend fun getDoc(path: String): Map<String, Any?>? = db.document(path).get().await().data

    override suspend fun listDocIds(collectionPath: String): List<String> =
        db.collection(collectionPath).get().await().documents.map { it.id }

    override fun observeDoc(path: String): Flow<Map<String, Any?>?> =
        callbackFlow {
            val registration =
                db.document(path).addSnapshotListener { snapshot, error ->
                    if (error != null) Log.w("FirestoreRemoteStore", "observeDoc failed: $path", error)
                    trySend(snapshot?.data)
                }
            awaitClose { registration.remove() }
        }

    override fun observeCollection(collectionPath: String): Flow<List<Map<String, Any?>>> =
        callbackFlow {
            val registration =
                db.collection(collectionPath).addSnapshotListener { snapshot, error ->
                    if (error != null) Log.w("FirestoreRemoteStore", "observeCollection failed: $collectionPath", error)
                    trySend(snapshot?.documents?.mapNotNull { it.data } ?: emptyList())
                }
            awaitClose { registration.remove() }
        }

    companion object {
        private const val APP_NAME = "nexapos-mb"
    }
}
