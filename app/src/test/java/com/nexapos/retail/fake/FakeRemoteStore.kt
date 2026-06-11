package com.nexapos.retail.fake

import com.nexapos.retail.domain.branch.RemoteStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * In-memory [RemoteStore] for tests. Records writes by path, can be told whether
 * it is signed in, and can be made to throw on writes to exercise failure handling.
 */
class FakeRemoteStore(
    private var uid: String? = "uid-1",
    private val failOnWrite: Boolean = false,
) : RemoteStore {
    val written = linkedMapOf<String, Map<String, Any?>>()

    override suspend fun signIn(
        email: String,
        password: String,
        createIfNew: Boolean,
    ): String {
        uid = "uid-1"
        return "uid-1"
    }

    override fun signedInUid(): String? = uid

    override fun signOut() {
        uid = null
    }

    override suspend fun setDoc(
        path: String,
        data: Map<String, Any?>,
    ) {
        if (failOnWrite) throw IOException("offline")
        written[path] = data
    }

    override suspend fun getDoc(path: String): Map<String, Any?>? = written[path]

    override suspend fun listDocIds(collectionPath: String): List<String> =
        written.keys
            .filter { it.startsWith("$collectionPath/") }
            .map { it.removePrefix("$collectionPath/").substringBefore("/") }
            .distinct()

    override fun observeDoc(path: String): Flow<Map<String, Any?>?> = MutableStateFlow(written[path])

    override fun observeCollection(collectionPath: String): Flow<List<Map<String, Any?>>> =
        MutableStateFlow(
            written
                .filterKeys { it.startsWith("$collectionPath/") && !it.removePrefix("$collectionPath/").contains("/") }
                .values
                .toList(),
        )
}
