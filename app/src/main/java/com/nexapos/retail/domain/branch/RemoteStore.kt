package com.nexapos.retail.domain.branch

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the cloud document store the multi-branch add-on syncs through.
 * The real implementation is Firestore; unit tests use an in-memory fake, so the
 * sync engine ([com.nexapos.retail.data.branch.BranchSync]) is fully testable
 * without Firebase.
 *
 * [path] arguments are complete Firestore document paths (an even number of
 * segments), e.g. "businesses/UID/branches/A/state/summary". Collection paths
 * (odd segment count) are used by [listDocIds].
 */
interface RemoteStore {
    /**
     * Signs the business account in, creating it when [createIfNew] is true and it
     * doesn't exist yet. Returns the account uid. Throws on failure (bad password,
     * no network, project unreachable) — callers treat any throw as "sync offline".
     */
    suspend fun signIn(
        email: String,
        password: String,
        createIfNew: Boolean,
    ): String

    /** The signed-in account uid, or null when not signed in. */
    fun signedInUid(): String?

    fun signOut()

    /** Creates or merges a document at [path]. */
    suspend fun setDoc(
        path: String,
        data: Map<String, Any?>,
    )

    /** Reads a document's fields, or null if it doesn't exist. */
    suspend fun getDoc(path: String): Map<String, Any?>?

    /** Document ids directly under the collection at [collectionPath]. */
    suspend fun listDocIds(collectionPath: String): List<String>

    /** Live updates of the document at [path] (emits null while it doesn't exist). */
    fun observeDoc(path: String): Flow<Map<String, Any?>?>
}
