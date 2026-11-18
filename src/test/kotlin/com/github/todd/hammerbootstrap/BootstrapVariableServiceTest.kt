package com.github.todd.hammerbootstrap

import com.github.todd.hammerbootstrap.matcher.TypeFilter
import com.github.todd.hammerbootstrap.service.BootstrapVariableService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color

class BootstrapVariableServiceTest {

    private val service = BootstrapVariableService()

    @Test
    fun `test bundled variables load correctly`() {
        val vars = service.getAllVariables()
        assertEquals(446, vars.size, "Should load all 446 Bootstrap 5.3 CSS variables")

        val primary = vars.find { it.name == "--bs-primary" }
        assertNotNull(primary)
        assertEquals("#0d6efd", primary?.defaultValue)
        assertEquals("Colors & Theme", primary?.category)
        assertTrue(primary?.isColor == true)
        assertEquals("#0d6efd", primary?.colorHex)
    }

    @Test
    fun `test dark mode overrides are present`() {
        val vars = service.getAllVariables()
        val withDark = vars.filter { it.darkValue != null }
        assertTrue(withDark.isNotEmpty(), "Should have variables with dark mode overrides")
        assertTrue(withDark.size >= 100, "Should have 100+ variables with dark mode overrides in Bootstrap 5.3")

        val bodyBg = vars.find { it.name == "--bs-body-bg" }
        assertNotNull(bodyBg)
        assertEquals("#fff", bodyBg?.defaultValue)
        assertEquals("#212529", bodyBg?.darkValue)
    }

    @Test
    fun `test categories are populated`() {
        val categories = service.getCategories()
        assertTrue(categories.contains("All Categories"))
        assertTrue(categories.contains("Colors & Theme"))
        assertTrue(categories.contains("Buttons"))
        assertTrue(categories.contains("Cards"))
        assertTrue(categories.contains("Modals"))
        assertTrue(categories.contains("Tables"))
        assertTrue(categories.contains("Navbar"))
    }

    @Test
    fun `test color parsing`() {
        val hex3 = BootstrapVariableService.parseColor("#fff")
        assertEquals(Color.WHITE, hex3)

        val hex6 = BootstrapVariableService.parseColor("#0d6efd")
        assertEquals(Color(13, 110, 253), hex6)

        val rgb = BootstrapVariableService.parseColor("rgb(13, 110, 253)")
        assertEquals(Color(13, 110, 253), rgb)

        val rgba = BootstrapVariableService.parseColor("rgba(33, 37, 41, 0.75)")
        assertNotNull(rgba)
        assertEquals(33, rgba?.red)
        assertEquals(37, rgba?.green)
        assertEquals(41, rgba?.blue)
        assertEquals((0.75 * 255).toInt(), rgba?.alpha)

        val namedBlack = BootstrapVariableService.parseColor("black")
        assertEquals(Color.BLACK, namedBlack)
    }

    @Test
    fun `test expressions and declarations`() {
        val item = service.getAllVariables().first { it.name == "--bs-primary" }
        assertEquals("var(--bs-primary)", item.varExpression)
        assertEquals("--bs-primary: #0d6efd;", item.declaration)
        assertEquals("primary", item.cleanName)
    }

    @Test
    fun `test resolved concrete default values`() {
        val vars = service.getAllVariables()
        val modalRadius = vars.find { it.name == "--bs-modal-border-radius" }
        assertNotNull(modalRadius)
        assertEquals("var(--bs-border-radius-lg)", modalRadius?.defaultValue)
        assertEquals("0.5rem", modalRadius?.resolvedValue)
        assertEquals("0.5rem", modalRadius?.effectiveDefaultValue)

        val modalWidth = vars.find { it.name == "--bs-modal-border-width" }
        assertNotNull(modalWidth)
        assertEquals("1px", modalWidth?.effectiveDefaultValue)

        val modalColor = vars.find { it.name == "--bs-modal-border-color" }
        assertNotNull(modalColor)
        assertEquals("rgba(0, 0, 0, 0.175)", modalColor?.effectiveDefaultValue)
    }
}
