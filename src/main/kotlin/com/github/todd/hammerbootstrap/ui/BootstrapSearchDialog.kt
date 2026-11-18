package com.github.todd.hammerbootstrap.ui

import com.github.todd.hammerbootstrap.matcher.TypeFilter
import com.github.todd.hammerbootstrap.model.BootstrapVariable
import com.github.todd.hammerbootstrap.service.BootstrapVariableService
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
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
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.Timer

class BootstrapSearchDialog(private val project: Project) : DialogWrapper(project, true) {

    companion object {
        val ICON_INSERT_VAR = IconLoader.getIcon("/icons/insertVar.svg", BootstrapSearchDialog::class.java.classLoader)
        val ICON_INSERT_NAME = IconLoader.getIcon("/icons/insertName.svg", BootstrapSearchDialog::class.java.classLoader)
        val ICON_COPY_VAR = IconLoader.getIcon("/icons/copyVar.svg", BootstrapSearchDialog::class.java.classLoader)
        val ICON_COPY_NAME = IconLoader.getIcon("/icons/copyName.svg", BootstrapSearchDialog::class.java.classLoader)
    }

    private val service = BootstrapVariableService.getInstance()
    private val searchField = SearchTextField(true)
    private val categoryCombo = ComboBox<String>()
    private val typeFilterCombo = ComboBox(TypeFilter.values().map { it.displayName }.toTypedArray())
    private val countLabel = JBLabel("0 variables").apply {
        font = JBUI.Fonts.miniFont()
        foreground = JBColor.GRAY
    }

    private val listModel = DefaultListModel<BootstrapVariable>()
    private val cellRenderer = VariableCellRenderer()
    private val variablesList = object : JBList<BootstrapVariable>(listModel) {
        override fun getScrollableTracksViewportWidth(): Boolean = true
    }.apply {
        cellRenderer = this@BootstrapSearchDialog.cellRenderer
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    // Bottom dedicated action toolbar for modal dialog
    private val bottomBar = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(4))).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor(Color(220, 224, 230), Color(50, 54, 60))),
            JBUI.Borders.empty(8, 8, 4, 8)
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

    private val insertVarBtn = JButton("Insert var(...)", com.intellij.icons.AllIcons.Actions.Execute).apply {
        toolTipText = "Insert var(--bs-...) at caret [Enter]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val insertNameBtn = JButton("Insert Name", com.intellij.icons.AllIcons.Nodes.Variable).apply {
        toolTipText = "Insert bare variable name at caret [Shift+Enter]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val copyVarBtn = JButton("Copy var(...)", com.intellij.icons.AllIcons.Actions.Copy).apply {
        toolTipText = "Copy var(--bs-...) to clipboard [Ctrl/Cmd+C]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val copyNameBtn = JButton("Copy Name", com.intellij.icons.AllIcons.Actions.MoveTo2).apply {
        toolTipText = "Copy bare variable name to clipboard [Ctrl/Cmd+Shift+C]"
        isFocusable = false
        isEnabled = false
        iconTextGap = JBUI.scale(8)
    }

    private val debounceTimer: Timer

    init {
        title = "Search Bootstrap CSS Variables"
        setOKButtonText("Insert var(...)")
        init()

        debounceTimer = Timer(80) {
            performSearch()
        }.apply { isRepeats = false }

        val categories = service.getCategories()
        categoryCombo.model = DefaultComboBoxModel(categories.toTypedArray())
        categoryCombo.addActionListener { performSearch() }
        typeFilterCombo.addActionListener { performSearch() }

        setupBottomBar()
        performSearch()
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
            insertAndClose(selected.varExpression)
        }
        insertNameBtn.addActionListener {
            val selected = variablesList.selectedValue ?: return@addActionListener
            insertAndClose(selected.name)
        }
        copyVarBtn.addActionListener {
            val selected = variablesList.selectedValue ?: return@addActionListener
            CopyPasteManager.getInstance().setContents(StringSelection(selected.varExpression))
            close(OK_EXIT_CODE)
        }
        copyNameBtn.addActionListener {
            val selected = variablesList.selectedValue ?: return@addActionListener
            CopyPasteManager.getInstance().setContents(StringSelection(selected.name))
            close(OK_EXIT_CODE)
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

    override fun createCenterPanel(): JComponent {
        val rootPanel = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            preferredSize = Dimension(JBUI.scale(680), JBUI.scale(460))
            border = JBUI.Borders.empty(4)
        }

        searchField.textEditor.emptyText.text = "Search by variable name, value, category, or description..."
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
                                insertAndClose(selected.name)
                            } else if (e.isMetaDown || e.isControlDown) {
                                insertAndClose(selected.declaration)
                            } else {
                                insertAndClose(selected.varExpression)
                            }
                            e.consume()
                        }
                    }
                }
            }
        })

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
                        insertAndClose(selected.varExpression)
                    } else if (e.clickCount == 2) {
                        val selected = variablesList.selectedValue ?: return
                        insertAndClose(selected.name)
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

        variablesList.addListSelectionListener {
            val selected = variablesList.selectedValue
            updateBottomBar(selected)
        }

        val topPanel = JPanel(BorderLayout(0, JBUI.scale(4)))
        topPanel.add(searchField, BorderLayout.NORTH)

        val filterRow = JPanel(BorderLayout())
        val combos = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0))
        categoryCombo.preferredSize = Dimension(JBUI.scale(140), categoryCombo.preferredSize.height)
        typeFilterCombo.preferredSize = Dimension(JBUI.scale(130), typeFilterCombo.preferredSize.height)
        combos.add(categoryCombo)
        combos.add(typeFilterCombo)

        val rightCount = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 4))
        rightCount.add(countLabel)

        filterRow.add(combos, BorderLayout.WEST)
        filterRow.add(rightCount, BorderLayout.EAST)
        topPanel.add(filterRow, BorderLayout.SOUTH)

        rootPanel.add(topPanel, BorderLayout.NORTH)
        rootPanel.add(JBScrollPane(variablesList), BorderLayout.CENTER)
        rootPanel.add(bottomBar, BorderLayout.SOUTH)

        return rootPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return searchField.textEditor
    }

    override fun doOKAction() {
        val selected = variablesList.selectedValue
        if (selected != null) {
            insertAndClose(selected.varExpression)
        } else {
            super.doOKAction()
        }
    }

    private fun showContextMenu(e: MouseEvent) {
        val selected = variablesList.selectedValue ?: return
        val menu = JPopupMenu()

        val insertVarItem = JMenuItem("Insert '${selected.varExpression}' at Caret", com.intellij.icons.AllIcons.Actions.Execute).apply {
            addActionListener { insertAndClose(selected.varExpression) }
        }
        val insertNameItem = JMenuItem("Insert '${selected.name}' at Caret", com.intellij.icons.AllIcons.Nodes.Variable).apply {
            addActionListener { insertAndClose(selected.name) }
        }
        val insertDeclItem = JMenuItem("Insert '${selected.declaration}' at Caret", com.intellij.icons.AllIcons.Actions.EditSource).apply {
            addActionListener { insertAndClose(selected.declaration) }
        }

        val copyVarItem = JMenuItem("Copy '${selected.varExpression}'", com.intellij.icons.AllIcons.Actions.Copy).apply {
            addActionListener {
                CopyPasteManager.getInstance().setContents(StringSelection(selected.varExpression))
                close(OK_EXIT_CODE)
            }
        }
        val copyNameItem = JMenuItem("Copy Variable Name '${selected.name}'", com.intellij.icons.AllIcons.Actions.MoveTo2).apply {
            addActionListener {
                CopyPasteManager.getInstance().setContents(StringSelection(selected.name))
                close(OK_EXIT_CODE)
            }
        }
        val copyValItem = JMenuItem("Copy Default Value: ${selected.effectiveDefaultValue}").apply {
            addActionListener {
                CopyPasteManager.getInstance().setContents(StringSelection(selected.effectiveDefaultValue))
                close(OK_EXIT_CODE)
            }
        }

        menu.add(insertVarItem)
        menu.add(insertNameItem)
        menu.add(insertDeclItem)
        menu.addSeparator()
        menu.add(copyVarItem)
        menu.add(copyNameItem)
        menu.add(copyValItem)

        menu.show(variablesList, e.x, e.y)
    }

    private fun insertAndClose(text: String) {
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
            CopyPasteManager.getInstance().setContents(StringSelection(text))
        }
        close(OK_EXIT_CODE)
    }

    private fun performSearch() {
        val query = searchField.text.trim()
        val cat = categoryCombo.selectedItem as? String ?: "All Categories"
        val typeIdx = typeFilterCombo.selectedIndex.coerceAtLeast(0)
        val type = TypeFilter.values().getOrNull(typeIdx) ?: TypeFilter.ALL

        val results = service.search(query, cat, type)

        listModel.clear()
        for (r in results) {
            listModel.addElement(r)
        }

        val total = service.getAllVariables().size
        countLabel.text = "${results.size} / $total"

        if (listModel.size() > 0) {
            variablesList.selectedIndex = 0
            updateBottomBar(listModel.getElementAt(0))
        } else {
            updateBottomBar(null)
        }
    }
}
