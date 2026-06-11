package com.nexapos.retail.data.branch

/** A branch's directory entry — each branch self-registers this when it syncs. */
data class BranchRef(
    val code: String,
    val name: String,
    val isHq: Boolean,
) {
    fun toMap(): Map<String, Any?> = mapOf("code" to code, "name" to name, "isHq" to isHq)

    companion object {
        fun fromMap(m: Map<String, Any?>) =
            BranchRef(
                code = m["code"] as? String ?: "",
                name = m["name"] as? String ?: "",
                isHq = m["isHq"] as? Boolean ?: false,
            )
    }
}

/** Head-office-managed map of viewer branch code → the codes it is allowed to view. */
data class VisibilityMatrix(val canView: Map<String, List<String>>) {
    fun toMap(): Map<String, Any?> = canView

    companion object {
        val EMPTY = VisibilityMatrix(emptyMap())

        fun fromMap(m: Map<String, Any?>) =
            VisibilityMatrix(
                m.mapValues { (_, v) -> (v as? List<*>)?.mapNotNull { it as? String } ?: emptyList() },
            )
    }
}

/** Consolidated head-office totals rolled up across several branches' summaries. */
data class ConsolidatedSummary(
    val branchCount: Int,
    val salesTodayCents: Long,
    val ticketsToday: Int,
    val stockValueCents: Long,
    val lowStockCount: Int,
)

/** Pure rules for who can view whom, and the head-office roll-up. */
object BranchDirectory {
    /**
     * The branches [viewerCode] is allowed to view (never itself). Head office sees
     * every other branch; a plain branch sees only the codes the [matrix] grants it.
     */
    fun viewable(
        viewerCode: String,
        isHq: Boolean,
        all: List<BranchRef>,
        matrix: VisibilityMatrix,
    ): List<BranchRef> {
        val others = all.filter { it.code != viewerCode }
        if (isHq) return others
        val allowed = matrix.canView[viewerCode].orEmpty().toSet()
        return others.filter { it.code in allowed }
    }

    fun consolidate(summaries: List<BranchSummary>) =
        ConsolidatedSummary(
            branchCount = summaries.size,
            salesTodayCents = summaries.sumOf { it.salesTodayCents },
            ticketsToday = summaries.sumOf { it.ticketsToday },
            stockValueCents = summaries.sumOf { it.stockValueCents },
            lowStockCount = summaries.sumOf { it.lowStockCount },
        )
}
