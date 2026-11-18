package com.github.todd.hammerbootstrap

import com.github.todd.hammerbootstrap.matcher.TypeFilter
import com.github.todd.hammerbootstrap.matcher.VariableMatcher
import com.github.todd.hammerbootstrap.model.BootstrapVariable
import com.github.todd.hammerbootstrap.service.BootstrapVariableService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VariableMatcherTest {

    private val service = BootstrapVariableService()
    private val allVars = service.getAllVariables()

    @Test
    fun `test exact match ranks first`() {
        val results = VariableMatcher.filterAndRank(allVars, "--bs-primary")
        assertTrue(results.isNotEmpty())
        assertEquals("--bs-primary", results[0].name)
    }

    @Test
    fun `test clean name search`() {
        val results = VariableMatcher.filterAndRank(allVars, "primary")
        assertTrue(results.isNotEmpty())
        assertEquals("--bs-primary", results[0].name)
    }

    @Test
    fun `test token matching for compound variable names`() {
        val results = VariableMatcher.filterAndRank(allVars, "btn bg")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "--bs-btn-bg" })
    }

    @Test
    fun `test search by hex value`() {
        val results = VariableMatcher.filterAndRank(allVars, "#0d6efd")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "--bs-primary" || it.name == "--bs-blue" })
    }

    @Test
    fun `test category filtering`() {
        val results = VariableMatcher.filterAndRank(allVars, "", categoryFilter = "Buttons")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == "Buttons" })
    }

    @Test
    fun `test colors only filter`() {
        val results = VariableMatcher.filterAndRank(allVars, "", typeFilter = TypeFilter.COLORS_ONLY)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.isColor || it.colorHex != null })
    }

    @Test
    fun `test dark mode filter`() {
        val results = VariableMatcher.filterAndRank(allVars, "", typeFilter = TypeFilter.HAS_DARK_MODE)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.darkValue != null })
    }

    @Test
    fun `test root scoped filter`() {
        val results = VariableMatcher.filterAndRank(allVars, "", typeFilter = TypeFilter.ROOT_ONLY)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.scope == ":root" })
    }

    @Test
    fun `test component scoped filter`() {
        val results = VariableMatcher.filterAndRank(allVars, "", typeFilter = TypeFilter.COMPONENT_ONLY)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.scope != ":root" })
    }

    @Test
    fun `test non-matching query returns empty list`() {
        val results = VariableMatcher.filterAndRank(allVars, "xyznonexistentvariable999")
        assertTrue(results.isEmpty())
    }
}
