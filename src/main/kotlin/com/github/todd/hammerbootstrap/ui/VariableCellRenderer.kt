package com.github.todd.hammerbootstrap.ui

import com.github.todd.hammerbootstrap.model.BootstrapVariable
import com.github.todd.hammerbootstrap.service.BootstrapVariableService
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

/**
 * Clean, high-performance, 2-row cell renderer for Bootstrap CSS variables.
 * Keeps rows 100% clean and free of inline buttons so variable names and
 * default values never overlap or clip outside the viewport.
 */
class VariableCellRenderer : ListCellRenderer<BootstrapVariable> {

    companion object {
        fun getCodeFont(size: Float, bold: Boolean): Font {
            val fontName = try {
                EditorColorsManager.getInstance().globalScheme.editorFontName
            } catch (_: Exception) {
                "JetBrains Mono"
            }
            val style = if (bold) Font.BOLD else Font.PLAIN
            return Font(fontName, style, JBUI.scale(size.toInt())).deriveFont(size)
        }
    }

    private val panel = object : JPanel(BorderLayout(JBUI.scale(10), JBUI.scale(6))) {
        override fun paintComponent(g: Graphics) {
            val g2 = g as? Graphics2D
            if (g2 != null) {
                UISettings.setupAntialiasing(g2)
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            }
            super.paintComponent(g)
        }
    }

    private val topRow = JPanel(BorderLayout(JBUI.scale(8), 0))
    private val leftTop = JPanel(BorderLayout(JBUI.scale(8), 0))
    private val badgeContainer = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0))
    private val scopeLabelTop = JLabel()

    private val bottomRow = JPanel(BorderLayout(JBUI.scale(8), 0))

    private val categoryBadge = CategoryBadgeLabel()
    private val colorSwatch = ColorSwatchLabel()

    private val nameComponent = object : SimpleColoredComponent() {
        override fun paintComponent(g: Graphics) {
            val g2 = g as? Graphics2D
            if (g2 != null) {
                UISettings.setupAntialiasing(g2)
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            }
            super.paintComponent(g)
        }
    }

    private val valueComponent = object : SimpleColoredComponent() {
        override fun paintComponent(g: Graphics) {
            val g2 = g as? Graphics2D
            if (g2 != null) {
                UISettings.setupAntialiasing(g2)
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            }
            super.paintComponent(g)
        }
    }

    private val content = JPanel()

    init {
        panel.isOpaque = true
        topRow.isOpaque = false
        leftTop.isOpaque = false
        badgeContainer.isOpaque = false
        bottomRow.isOpaque = false
        nameComponent.isOpaque = false
        valueComponent.isOpaque = false
        scopeLabelTop.isOpaque = false

        badgeContainer.add(categoryBadge)
        badgeContainer.add(colorSwatch)

        leftTop.add(badgeContainer, BorderLayout.WEST)
        leftTop.add(nameComponent, BorderLayout.CENTER)

        topRow.add(leftTop, BorderLayout.CENTER)
        topRow.add(scopeLabelTop, BorderLayout.EAST)

        bottomRow.add(valueComponent, BorderLayout.CENTER)

        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.isOpaque = false
        content.add(topRow)
        content.add(Box.createVerticalStrut(JBUI.scale(6)))
        content.add(bottomRow)

        panel.add(content, BorderLayout.CENTER)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(Color(225, 228, 233), Color(50, 53, 58))),
            JBUI.Borders.empty(10, 14)
        )
    }

    override fun getListCellRendererComponent(
        list: JList<out BootstrapVariable>,
        value: BootstrapVariable?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value == null) return panel

        if (isSelected) {
            panel.background = UIUtil.getListSelectionBackground(true)
            scopeLabelTop.foreground = UIUtil.getListSelectionForeground(true)
        } else {
            panel.background = if (index % 2 == 0) UIUtil.getListBackground() else UIUtil.getDecoratedRowColor()
            scopeLabelTop.foreground = JBColor(Color(120, 120, 120), Color(150, 150, 150))
        }

        // Category Badge
        categoryBadge.setCategory(value.category, value.isCustom)

        // Color Swatch
        val parsedColor = value.colorHex?.let { BootstrapVariableService.parseColor(it) }
            ?: if (value.isColor) BootstrapVariableService.parseColor(value.defaultValue) else null

        if (parsedColor != null) {
            colorSwatch.setColor(parsedColor)
            colorSwatch.isVisible = true
        } else {
            colorSwatch.isVisible = false
        }

        // Variable Name in bold antialiased code font
        renderName(value.name, isSelected)

        // Value Component (Left Bottom: "default: <value>")
        renderValue(value, isSelected)

        // Scope Label (Top Right)
        scopeLabelTop.text = "[${value.scope}]"
        scopeLabelTop.font = getCodeFont(11f, false)

        return panel
    }

    private fun renderName(name: String, isSelected: Boolean) {
        nameComponent.clear()
        nameComponent.font = getCodeFont(13.5f, true)
        val attr = if (isSelected) {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, UIUtil.getListSelectionForeground(true))
        } else {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, UIUtil.getListForeground())
        }
        nameComponent.append(name, attr)
    }

    private fun renderValue(value: BootstrapVariable, isSelected: Boolean) {
        valueComponent.clear()
        valueComponent.font = getCodeFont(12f, false)

        val labelAttr = if (isSelected) {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color(225, 230, 240))
        } else {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(Color(110, 115, 125), Color(150, 155, 165)))
        }

        val valAttr = if (isSelected) {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, Color(255, 255, 255))
        } else {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor(Color(35, 38, 45), Color(220, 225, 230)))
        }

        val aliasAttr = if (isSelected) {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Color(205, 215, 230))
        } else {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor(Color(130, 135, 145), Color(140, 145, 155)))
        }

        val darkAttr = if (isSelected) {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color(255, 238, 185))
        } else {
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(Color(115, 68, 180), Color(185, 150, 250)))
        }

        valueComponent.append("default: ", labelAttr)

        val resolved = value.resolvedValue
        if (!resolved.isNullOrBlank() && resolved != value.defaultValue) {
            valueComponent.append(resolved, valAttr)
            valueComponent.append("  (${value.defaultValue})", aliasAttr)
        } else {
            valueComponent.append(value.defaultValue, valAttr)
        }

        if (!value.darkValue.isNullOrBlank()) {
            valueComponent.append("  [dark: ${value.darkValue}]", darkAttr)
        }
    }

    private class CategoryBadgeLabel : JLabel() {
        private var badgeBg: Color = Color.GRAY
        private var badgeFg: Color = Color.WHITE

        init {
            isOpaque = false
            font = getCodeFont(10.5f, true)
            border = JBUI.Borders.empty(3, 7)
            horizontalAlignment = CENTER
        }

        fun setCategory(category: String, isCustom: Boolean) {
            val text = when {
                isCustom -> "CUSTOM"
                category == "Colors & Theme" -> "THEME"
                category == "Typography & Body" -> "BODY"
                category == "Layout & Borders" -> "LAYOUT"
                category == "Grid & Layout" -> "GRID"
                category == "Buttons" -> "BTN"
                category == "Cards" -> "CARD"
                category == "Modals" -> "MODAL"
                category == "Alerts" -> "ALERT"
                category == "Badges" -> "BADGE"
                category == "Navbar" -> "NAVBAR"
                category == "Navs & Tabs" -> "NAV"
                category == "Tables" -> "TABLE"
                category == "Forms" -> "FORM"
                category == "Accordion" -> "ACCORD"
                category == "Dropdowns" -> "DROPDOWN"
                category == "List Group" -> "LIST"
                category == "Offcanvas" -> "OFFCANVAS"
                category == "Tooltips & Popovers" -> "POPOVER"
                category == "Pagination" -> "PAGE"
                category == "Progress" -> "PROGRESS"
                category == "Spinners" -> "SPINNER"
                category == "Toasts" -> "TOAST"
                category == "Breadcrumbs" -> "CRUMB"
                category == "Carousel" -> "CAROUSEL"
                else -> category.uppercase().take(6)
            }
            this.text = text

            badgeBg = when (text) {
                "THEME" -> JBColor(Color(102, 16, 242), Color(111, 66, 193))
                "BTN" -> JBColor(Color(13, 110, 253), Color(25, 118, 210))
                "MODAL" -> JBColor(Color(214, 51, 132), Color(233, 30, 99))
                "ALERT" -> JBColor(Color(220, 53, 69), Color(211, 47, 47))
                "TABLE" -> JBColor(Color(32, 201, 151), Color(0, 150, 136))
                "NAVBAR", "NAV" -> JBColor(Color(13, 202, 240), Color(3, 169, 244))
                "CARD" -> JBColor(Color(108, 117, 125), Color(97, 97, 97))
                "FORM" -> JBColor(Color(253, 126, 20), Color(245, 124, 0))
                "CUSTOM" -> JBColor(Color(25, 135, 84), Color(46, 125, 50))
                else -> JBColor(Color(108, 117, 125), Color(117, 117, 117))
            }
            badgeFg = Color.WHITE
            foreground = badgeFg
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            UISettings.setupAntialiasing(g2)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.color = badgeBg
            g2.fillRoundRect(0, 0, width, height, JBUI.scale(6), JBUI.scale(6))
            super.paintComponent(g2)
            g2.dispose()
        }
    }

    private class ColorSwatchLabel : JLabel() {
        private var swatchColor: Color = Color.WHITE

        init {
            isOpaque = false
            preferredSize = Dimension(JBUI.scale(18), JBUI.scale(18))
        }

        fun setColor(color: Color) {
            swatchColor = color
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val px = 0
            val py = (height - JBUI.scale(15)) / 2
            val size = JBUI.scale(15)

            if (swatchColor.alpha < 255) {
                g2.color = Color.LIGHT_GRAY
                g2.fillRect(px, py, size, size)
                g2.color = Color.WHITE
                g2.fillRect(px, py, size / 2, size / 2)
                g2.fillRect(px + size / 2, py + size / 2, size / 2, size / 2)
            }

            g2.color = swatchColor
            g2.fillRoundRect(px, py, size, size, JBUI.scale(4), JBUI.scale(4))

            g2.color = JBColor(Color(0, 0, 0, 70), Color(255, 255, 255, 70))
            g2.drawRoundRect(px, py, size, size, JBUI.scale(4), JBUI.scale(4))

            g2.dispose()
        }
    }
}
