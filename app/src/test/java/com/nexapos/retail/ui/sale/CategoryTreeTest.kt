package com.nexapos.retail.ui.sale

import com.nexapos.retail.data.entity.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTreeTest {
    private val plumbing = Category(id = 1, name = "Plumbing", parentId = null)
    private val pipes = Category(id = 2, name = "Pipes", parentId = 1)
    private val fittings = Category(id = 3, name = "Fittings", parentId = 1)
    private val electrical = Category(id = 4, name = "Electrical", parentId = null)

    @Test
    fun `tree groups mains with their subs`() {
        val tree = buildCategoryTree(listOf(plumbing, pipes, fittings, electrical))
        assertEquals(listOf("Electrical", "Plumbing"), tree.map { it.name })
        val plumb = tree.first { it.name == "Plumbing" }
        assertEquals(listOf("Fittings", "Pipes"), plumb.subs.map { it.name })
    }

    @Test
    fun `mainIdOf returns parent for a sub and self for a main`() {
        assertEquals(1L, mainIdOf(pipes))
        assertEquals(1L, mainIdOf(plumbing))
    }

    @Test
    fun `predicate matches all when no main selected`() {
        assertTrue(matchesCategory("Plumbing", "Pipes", main = null, sub = null))
    }

    @Test
    fun `predicate narrows by main then sub`() {
        assertTrue(matchesCategory("Plumbing", "Pipes", main = "Plumbing", sub = null))
        assertFalse(matchesCategory("Electrical", "Wire", main = "Plumbing", sub = null))
        assertTrue(matchesCategory("Plumbing", "Pipes", main = "Plumbing", sub = "Pipes"))
        assertFalse(matchesCategory("Plumbing", "Fittings", main = "Plumbing", sub = "Pipes"))
    }

    @Test
    fun `label shows main and sub only when distinct`() {
        assertEquals("Plumbing · Pipes", categoryLabel("Plumbing", "Pipes"))
        assertEquals("Plumbing", categoryLabel("Plumbing", "Plumbing"))
    }
}
