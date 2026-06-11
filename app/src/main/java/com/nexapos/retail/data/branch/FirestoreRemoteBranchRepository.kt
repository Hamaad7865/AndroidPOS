package com.nexapos.retail.data.branch

import com.nexapos.retail.domain.branch.RemoteBranchRepository
import com.nexapos.retail.domain.branch.RemoteBranchState
import com.nexapos.retail.domain.branch.RemoteStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Firestore-backed [RemoteBranchRepository]. Every read is scoped to the signed-in
 * business account's uid; when no one is signed in, the flows emit empty so the UI
 * shows a "not connected" state instead of failing.
 */
class FirestoreRemoteBranchRepository(private val remote: RemoteStore) : RemoteBranchRepository {
    override fun isAvailable(): Boolean = remote.signedInUid() != null

    override fun observeBranches(): Flow<List<BranchRef>> {
        val uid = remote.signedInUid() ?: return flowOf(emptyList())
        return remote.observeCollection(BranchPaths.branchesCollection(uid))
            .map { docs -> docs.map { BranchRef.fromMap(it) }.filter { it.code.isNotBlank() }.sortedBy { it.code } }
    }

    override fun observeVisibility(): Flow<VisibilityMatrix> {
        val uid = remote.signedInUid() ?: return flowOf(VisibilityMatrix.EMPTY)
        return remote.observeDoc(BranchPaths.visibility(uid))
            .map { m -> if (m != null) VisibilityMatrix.fromMap(m) else VisibilityMatrix.EMPTY }
    }

    override fun observeState(code: String): Flow<RemoteBranchState> {
        val uid = remote.signedInUid() ?: return flowOf(RemoteBranchState(null, null))
        return remote.observeDoc(BranchPaths.summary(uid, code))
            .map { m ->
                RemoteBranchState(
                    summary = m?.let { BranchSummary.fromMap(it) },
                    lastSyncAt = (m?.get("lastSyncAt") as? Number)?.toLong(),
                )
            }
    }

    override fun observeStock(code: String): Flow<List<StockItemDto>> {
        val uid = remote.signedInUid() ?: return flowOf(emptyList())
        return remote.observeDoc(BranchPaths.stockChunk(uid, code, 0))
            .map { m -> if (m != null) StockChunk.fromMap(m).items else emptyList() }
    }

    override suspend fun day(
        code: String,
        date: String,
    ): DayDoc? {
        val uid = remote.signedInUid() ?: return null
        return remote.getDoc(BranchPaths.day(uid, code, date))?.let { DayDoc.fromMap(it) }
    }

    override suspend fun saveVisibility(matrix: VisibilityMatrix) {
        val uid = remote.signedInUid() ?: return
        remote.setDoc(BranchPaths.visibility(uid), matrix.toMap())
    }
}
