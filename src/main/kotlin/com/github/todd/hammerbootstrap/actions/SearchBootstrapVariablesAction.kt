package com.github.todd.hammerbootstrap.actions

import com.github.todd.hammerbootstrap.ui.BootstrapSearchDialog
import com.github.todd.hammerbootstrap.ui.BootstrapVariablesPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class SearchBootstrapVariablesAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Bootstrap CSS")

        val editor = e.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText?.trim()

        if (toolWindow != null && toolWindow.isAvailable) {
            toolWindow.activate {
                val content = toolWindow.contentManager.selectedContent ?: return@activate
                val panel = content.component as? BootstrapVariablesPanel ?: return@activate
                panel.focusSearch(selectedText)
            }
        } else {
            BootstrapSearchDialog(project).show()
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && !project.isDisposed
    }
}
