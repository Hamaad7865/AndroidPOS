package com.nexapos.retail.data.branch

import org.junit.Assert.assertEquals
import org.junit.Test

class BranchIdentityTest {
    @Test
    fun `normalizeCode upper-cases, strips non-alphanumerics, and caps at 4`() {
        assertEquals("A", BranchIdentity.normalizeCode("a"))
        assertEquals("HQ", BranchIdentity.normalizeCode("  hq "))
        assertEquals("CUR1", BranchIdentity.normalizeCode("cur1"))
        assertEquals("BRAN", BranchIdentity.normalizeCode("branch-1")) // alnum-only then capped
        assertEquals("ABCD", BranchIdentity.normalizeCode("a-b-c-d-e"))
        assertEquals("", BranchIdentity.normalizeCode("   "))
    }
}
