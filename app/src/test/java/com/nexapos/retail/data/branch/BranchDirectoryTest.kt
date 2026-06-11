package com.nexapos.retail.data.branch

import org.junit.Assert.assertEquals
import org.junit.Test

class BranchDirectoryTest {
    private val a = BranchRef("A", "Curepipe", isHq = false)
    private val b = BranchRef("B", "Rose Hill", isHq = false)
    private val hq = BranchRef("HQ", "Head Office", isHq = true)
    private val all = listOf(a, b, hq)

    @Test
    fun `branch ref round-trips`() {
        assertEquals(a, BranchRef.fromMap(a.toMap()))
        assertEquals(hq, BranchRef.fromMap(hq.toMap()))
    }

    @Test
    fun `visibility matrix round-trips with nested lists`() {
        val m = VisibilityMatrix(mapOf("A" to listOf("B"), "B" to emptyList()))
        assertEquals(m, VisibilityMatrix.fromMap(m.toMap()))
    }

    @Test
    fun `head office sees every other branch`() {
        val seen = BranchDirectory.viewable("HQ", isHq = true, all = all, matrix = VisibilityMatrix.EMPTY)
        assertEquals(listOf(a, b), seen)
    }

    @Test
    fun `a branch sees only the codes the matrix grants and never itself`() {
        val matrix = VisibilityMatrix(mapOf("A" to listOf("B")))
        assertEquals(listOf(b), BranchDirectory.viewable("A", isHq = false, all = all, matrix = matrix))
        // B has no entry → sees nothing
        assertEquals(emptyList<BranchRef>(), BranchDirectory.viewable("B", isHq = false, all = all, matrix = matrix))
    }

    @Test
    fun `consolidate sums all summaries`() {
        val s1 = BranchSummary(50_000L, 4, 1_000_000L, 20, 2, null, null)
        val s2 = BranchSummary(30_050L, 3, 500_000L, 10, 1, "Priya", 123L)
        val c = BranchDirectory.consolidate(listOf(s1, s2))
        assertEquals(2, c.branchCount)
        assertEquals(80_050L, c.salesTodayCents)
        assertEquals(7, c.ticketsToday)
        assertEquals(1_500_000L, c.stockValueCents)
        assertEquals(3, c.lowStockCount)
    }
}
