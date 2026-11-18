package com.github.todd.hammerbootstrap.ui

import com.github.todd.hammerbootstrap.matcher.TypeFilter
import com.github.todd.hammerbootstrap.model.BootstrapVariable
import com.github.todd.hammerbootstrap.service.BootstrapVariableService
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.Timer

class BootstrapVariablesPanel(private val project: Project) : JPanel(BorderLayout()) {

    companion object {
        val ICON_INSERT_VAR = IconLoader.getIcon("/icons/insertVar.svg", BootstrapVariablesPanel::class.java.classLoader)
        val ICON_INSERT_NAME = IconLoader.getIcon("/icons/insertName.svg", BootstrapVariablesPanel::class.java.classLoader)
        val ICON_COPY_VAR = IconLoader.getIcon("/icons/copyVar.svg", BootstrapVariablesPanel::class.java.classLoader)
        val ICON_COPY_NAME = IconLoader.getIcon("/icons/copyName.svg", BootstrapVariablesPanel::class.java.classLoader)
    }

    private val service = BootstrapVariableService.getInstance()

    private val searchField = SearchTextField(true)
    private val categoryCombo = ComboBox<String>()
    private val typeFilterCombo = ComboBox(TypeFilter.values().map { it.displayName }.toTypedArray())
    private val refreshButton = JButton(AllIcons.Actions.Refresh).apply {
        toolTipText = "Re-scan project CSS variables"
        isFocusable = false
        preferredSize = Dimension(JBUI.scale(28), JBUI.scale(24))
    }
    private val countLabel = JBLabel("0 variables").apply {
        foreground = JBColor.GRAY
        font = font.deriveFont(Font.PLAIN, JBUI.scale(11).toFloat())
    }

    private val listModel = DefaultListModel<BootstrapVariable>()
    private val cellRenderer = VariableCellRenderer()
    private val variablesList = object : JBList<BootstrapVariable>(listModel) {
        override fun getScrollableTracksViewportWidth(): Boolean = true
    }

    // Bottom dedicated action toolbar components
    private val bottomBar = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(4))).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor(Color(220, 224, 230), Color(50, 54, 60))),
            JBUI.Borders.empty(8, 12, 10, 12)
        )
        isOpaque = true
    }

    private val selectedVarTitleLabel = JBLabel("No variable selected").apply {
        font = VariableCellRenderer.getCodeFont(12.5f, true)
    }

    private val selectedVarDetailLabel = JBLabel("Select a variable to insert or copy").apply {
        font = VariableCellRenderer.getCodeFont(11f, false)
        foreground = JBColor(Color(110, 115, 125), Color(150, 155, 165))
    }

    private val insertVarBtn = JButton("Insert var(...)", AllIcons.Actions.Execute).apply {
        toolTipText = "Insert var(--bs-...) at current caret [Enter]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val insertNameBtn = JButton("Insert Name", AllIcons.Nodes.Variable).apply {
        toolTipText = "Insert bare variable name at current caret [Shift+Enter]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val copyVarBtn = JButton("Copy var(...)", AllIcons.Actions.Copy).apply {
        toolTipText = "Copy var(--bs-...) to clipboard [Ctrl/Cmd+C]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val copyNameBtn = JButton("Copy Name", AllIcons.Actions.MoveTo2).apply {
        toolTipText = "Copy bare variable name to clipboard [Ctrl/Cmd+Shift+C]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val debounceTimer: Timer

    init {
        debounceTimer = Timer(100) {
            performSearch()
        }.apply { isRepeats = false }

        setupHeader()
        setupList()
        setupBottomBar()

        val topContainer = JPanel(BorderLayout())
        topContainer.add(createSearchToolbar(), BorderLayout.NORTH)
        topContainer.add(createFiltersToolbar(), BorderLayout.SOUTH)

        add(topContainer, BorderLayout.NORTH)
        val scrollPane = JBScrollPane(variablesList).apply {
            horizontalScrollBarPolicy = javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        add(scrollPane, BorderLayout.CENTER)
        add(bottomBar, BorderLayout.SOUTH)

        // Initial scan on background thread
        ApplicationManager.getApplication().executeOnPooledThread {
            service.scanProjectVariables(project)
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed) {
                    refreshCategories()
                    performSearch()
                }
            }
        }
    }

    private fun createSearchToolbar(): JPanel {
        val panel = JPanel(BorderLayout(JBUI.scale(6), 0))
        panel.border = JBUI.Borders.empty(8, 8, 4, 8)

        searchField.textEditor.emptyText.text = "Search CSS variables (e.g. primary, modal-border, #0d6efd)..."

        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                debounceTimer.restart()
            }
        })

        searchField.textEditor.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val size = listModel.size()
                if (size == 0) return

                val currentIdx = variablesList.selectedIndex
                when (e.keyCode) {
                    KeyEvent.VK_DOWN -> {
                        val next = (currentIdx + 1).coerceAtMost(size - 1)
                        variablesList.selectedIndex = next
                        variablesList.ensureIndexIsVisible(next)
                        e.consume()
                    }
                    KeyEvent.VK_UP -> {
                        val prev = (currentIdx - 1).coerceAtLeast(0)
                        variablesList.selectedIndex = prev
                        variablesList.ensureIndexIsVisible(prev)
                        e.consume()
                    }
                    KeyEvent.VK_ENTER -> {
                        val selected = variablesList.selectedValue
                        if (selected != null) {
                            if (e.isShiftDown) {
                                insertIntoEditor(selected.name)
                            } else if (e.isMetaDown || e.isControlDown) {
                                insertIntoEditor(selected.declaration)
                            } else {
                                insertIntoEditor(selected.varExpression)
                            }
                            e.consume()
                        }
                    }
                }
            }
        })

        panel.add(searchField, BorderLayout.CENTER)
        return panel
    }

    private fun createFiltersToolbar(): JPanel {
        val panel = JPanel(BorderLayout(JBUI.scale(6), 0))
        panel.border = JBUI.Borders.empty(0, 8, 6, 8)

        val leftFilters = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0))
        leftFilters.isOpaque = false

        categoryCombo.isFocusable = false
        categoryCombo.preferredSize = Dimension(JBUI.scale(130), JBUI.scale(24))
        categoryCombo.addActionListener { performSearch() }

        typeFilterCombo.isFocusable = false
        typeFilterCombo.preferredSize = Dimension(JBUI.scale(130), JBUI.scale(24))
        typeFilterCombo.addActionListener { performSearch() }

        refreshButton.addActionListener {
            refreshButton.isEnabled = false
            ApplicationManager.getApplication().executeOnPooledThread {
                service.scanProjectVariables(project)
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) {
                        refreshCategories()
                        performSearch()
                        refreshButton.isEnabled = true
                    }
                }
            }
        }

        leftFilters.add(categoryCombo)
        leftFilters.add(typeFilterCombo)
        leftFilters.add(refreshButton)

        val rightFilters = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        rightFilters.isOpaque = false
        rightFilters.add(countLabel)

        panel.add(leftFilters, BorderLayout.WEST)
        panel.add(rightFilters, BorderLayout.EAST)
        return panel
    }

    private fun setupHeader() {
        categoryCombo.isFocusable = false
        typeFilterCombo.isFocusable = false
        refreshCategories()
    }

    private fun refreshCategories() {
        val prevSelected = categoryCombo.selectedItem as? String
        val categories = service.getCategories()
        categoryCombo.model = DefaultComboBoxModel(categories.toTypedArray())
        if (prevSelected != null && prevSelected in categories) {
            categoryCombo.selectedItem = prevSelected
        } else {
            categoryCombo.selectedIndex = 0
        }
    }

    private fun setupList() {
        variablesList.cellRenderer = cellRenderer
        variablesList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        variablesList.emptyText.text = "No Bootstrap CSS variables found matching query"

        variablesList.addListSelectionListener {
            val selected = variablesList.selectedValue
            updateBottomBar(selected)
        }

        variablesList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.isPopupTrigger || javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    val index = variablesList.locationToIndex(e.point)
                    if (index != -1) {
                        variablesList.selectedIndex = index
                        showContextMenu(e)
                    }
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    val index = variablesList.locationToIndex(e.point)
                    if (index != -1 && (e.isMetaDown || e.isControlDown)) {
                        variablesList.selectedIndex = index
                        val selected = variablesList.model.getElementAt(index)
                        insertIntoEditor(selected.varExpression)
                    } else if (e.clickCount == 2) {
                        val selected = variablesList.selectedValue ?: return
                        insertIntoEditor(selected.name)
                    }
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val index = variablesList.locationToIndex(e.point)
                    if (index != -1) {
                        variablesList.selectedIndex = index
                        showContextMenu(e)
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val index = variablesList.locationToIndex(e.point)
                    if (index != -1) {
                        variablesList.selectedIndex = index
                        showContextMenu(e)
                    }
                }
            }
        })

        variablesList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val selected = variablesList.selectedValue ?: return
                when {
                    e.keyCode == KeyEvent.VK_ENTER && e.isShiftDown -> {
                        insertIntoEditor(selected.name)
                        e.consume()
                    }
                    e.keyCode == KeyEvent.VK_ENTER -> {
                        insertIntoEditor(selected.varExpression)
                        e.consume()
                    }
                    (e.keyCode == KeyEvent.VK_C && (e.isMetaDown || e.isControlDown) && e.isShiftDown) -> {
                        copyToClipboard(selected.name)
                        e.consume()
                    }
                    (e.keyCode == KeyEvent.VK_C && (e.isMetaDown || e.isControlDown)) -> {
                        copyToClipboard(selected.varExpression)
                        e.consume()
                    }
                }
            }
        })
    }

    private fun setupBottomBar() {
        val infoBox = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }
        infoBox.add(selectedVarTitleLabel)
        infoBox.add(Box.createVerticalStrut(JBUI.scale(2)))
        infoBox.add(selectedVarDetailLabel)

        val buttonsRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(6)
        }

        insertVarBtn.addActionListener {
            val selected = variablesList.selectedValue ?: return@addActionListener
            insertIntoEditor(selected.varExpression)
        }
        insertNameBtn.addActionListener {
            val selected = variablesList.selectedValue ?: return@addActionListener
            insertIntoEditor(selected.name)
        }
        copyVarBtn.addActionListener {
            val selected = variablesList.selectedValue ?: return@addActionListener
            copyToClipboard(selected.varExpression)
        }
        copyNameBtn.addActionListener {
            val selected = variablesList.selectedValue ?: return@addActionListener
            copyToClipboard(selected.name)
        }

        buttonsRow.add(insertVarBtn)
        buttonsRow.add(insertNameBtn)
        buttonsRow.add(copyVarBtn)
        buttonsRow.add(copyNameBtn)

        bottomBar.add(infoBox, BorderLayout.NORTH)
        bottomBar.add(buttonsRow, BorderLayout.SOUTH)
    }

    private fun updateBottomBar(variable: BootstrapVariable?) {
        if (variable == null) {
            selectedVarTitleLabel.text = "No variable selected"
            selectedVarDetailLabel.text = "Select a variable to insert or copy"
            insertVarBtn.isEnabled = false
            insertNameBtn.isEnabled = false
            copyVarBtn.isEnabled = false
            copyNameBtn.isEnabled = false
            return
        }

        selectedVarTitleLabel.text = "${variable.name}  [${variable.category}]"
        val resolved = if (!variable.resolvedValue.isNullOrBlank() && variable.resolvedValue != variable.defaultValue) {
            "default: ${variable.resolvedValue} (${variable.defaultValue})"
        } else {
            "default: ${variable.defaultValue}"
        }
        val dark = if (!variable.darkValue.isNullOrBlank()) "  [dark: ${variable.darkValue}]" else ""
        selectedVarDetailLabel.text = "$resolved$dark  [${variable.scope}]"

        insertVarBtn.isEnabled = true
        insertNameBtn.isEnabled = true
        copyVarBtn.isEnabled = true
        copyNameBtn.isEnabled = true
    }

    private fun showContextMenu(e: MouseEvent) {
        val selected = variablesList.selectedValue ?: return
        val menu = JPopupMenu()

        val insertVarItem = JMenuItem("Insert '${selected.varExpression}' at Caret", AllIcons.Actions.Execute).apply {
            addActionListener { insertIntoEditor(selected.varExpression) }
        }
        val insertNameItem = JMenuItem("Insert '${selected.name}' at Caret", AllIcons.Nodes.Variable).apply {
            addActionListener { insertIntoEditor(selected.name) }
        }
        val insertDeclItem = JMenuItem("Insert '${selected.declaration}' at Caret", AllIcons.Actions.EditSource).apply {
            addActionListener { insertIntoEditor(selected.declaration) }
        }

        val copyVarItem = JMenuItem("Copy '${selected.varExpression}'", AllIcons.Actions.Copy).apply {
            addActionListener { copyToClipboard(selected.varExpression) }
        }
        val copyNameItem = JMenuItem("Copy Variable Name '${selected.name}'", AllIcons.Actions.MoveTo2).apply {
            addActionListener { copyToClipboard(selected.name) }
        }
        val copyValItem = JMenuItem("Copy Default Value: ${selected.effectiveDefaultValue}").apply {
            addActionListener { copyToClipboard(selected.effectiveDefaultValue) }
        }
        val copyRawValItem = if (selected.resolvedValue != null && selected.resolvedValue != selected.defaultValue) {
            JMenuItem("Copy Raw Alias: ${selected.defaultValue}").apply {
                addActionListener { copyToClipboard(selected.defaultValue) }
            }
        } else null
        val copyDarkItem = JMenuItem("Copy Dark Value: ${selected.darkValue ?: ""}").apply {
            isEnabled = selected.darkValue != null
            addActionListener { selected.darkValue?.let { copyToClipboard(it) } }
        }
        val copyDeclItem = JMenuItem("Copy Declaration").apply {
            addActionListener { copyToClipboard(selected.declaration) }
        }

        menu.add(insertVarItem)
        menu.add(insertNameItem)
        menu.add(insertDeclItem)
        menu.addSeparator()
        menu.add(copyVarItem)
        menu.add(copyNameItem)
        menu.add(copyValItem)
        if (copyRawValItem != null) {
            menu.add(copyRawValItem)
        }
        if (selected.darkValue != null) {
            menu.add(copyDarkItem)
        }
        menu.add(copyDeclItem)

        menu.show(variablesList, e.x, e.y)
    }

    fun performSearch() {
        val query = searchField.text.trim()
        val cat = categoryCombo.selectedItem as? String ?: "All Categories"
        val typeIdx = typeFilterCombo.selectedIndex.coerceAtLeast(0)
        val type = TypeFilter.values().getOrNull(typeIdx) ?: TypeFilter.ALL

        val results = service.search(query, cat, type)

        listModel.clear()
        for (r in results) {
            listModel.addElement(r)
        }

        val totalAll = service.getAllVariables().size
        if (results.size == totalAll && query.isEmpty() && cat == "All Categories" && type == TypeFilter.ALL) {
            countLabel.text = "$totalAll variables"
        } else {
            countLabel.text = "${results.size} matches"
        }

        if (listModel.size() > 0) {
            variablesList.selectedIndex = 0
            updateBottomBar(listModel.getElementAt(0))
        } else {
            updateBottomBar(null)
        }
    }

    fun focusSearch(initialText: String? = null) {
        if (!initialText.isNullOrBlank()) {
            searchField.text = initialText
        }
        searchField.requestFocus()
        searchField.textEditor.selectAll()
    }

    private fun insertIntoEditor(text: String) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val doc = editor.document
            val caretModel = editor.caretModel
            val offset = caretModel.offset

            WriteCommandAction.runWriteCommandAction(project) {
                doc.insertString(offset, text)
                caretModel.moveToOffset(offset + text.length)
            }
        } else {
            copyToClipboard(text)
        }
    }

    private fun copyToClipboard(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }
}
