package com.nexapos.retail.domain.branch

import com.nexapos.retail.data.branch.BranchRef
import com.nexapos.retail.data.branch.BranchSummary
import com.nexapos.retail.data.branch.DayDoc
import com.nexapos.retail.data.branch.StockItemDto
import com.nexapos.retail.data.branch.VisibilityMatrix
import kotlinx.coroutines.flow.Flow

/** A remote branch's live state: its latest summary and when it last synced. */
data class RemoteBranchState(
    val summary: BranchSummary?,
    val lastSyncAt: Long?,
)

/**
 * Read access to OTHER branches' synced data (plus the head-office visibility
 * config) through the shared business account. The sync engine writes; this only
 * reads — except [saveVisibility], which head office uses to grant viewing rights.
 */
interface RemoteBranchRepository {
    /** True when the business account is signed in (cross-branch reads are possible). */
    fun isAvailable(): Boolean

    /** Every branch that has registered itself — the directory. */
    fun observeBranches(): Flow<List<BranchRef>>

    /** The head-office-managed visibility matrix. */
    fun observeVisibility(): Flow<VisibilityMatrix>

    /** A branch's latest summary plus its last-sync time. */
    fun observeState(code: String): Flow<RemoteBranchState>

    /** A branch's stock list (first chunk; covers a normal catalogue). */
    fun observeStock(code: String): Flow<List<StockItemDto>>

    /** A branch's recorded data for one calendar day (yyyy-MM-dd), or null. */
    suspend fun day(
        code: String,
        date: String,
    ): DayDoc?

    /** Head office writes the visibility matrix. */
    suspend fun saveVisibility(matrix: VisibilityMatrix)
}
