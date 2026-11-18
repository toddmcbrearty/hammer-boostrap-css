package com.github.todd.hammerbootstrap.service

import com.github.todd.hammerbootstrap.matcher.TypeFilter
import com.github.todd.hammerbootstrap.matcher.VariableMatcher
import com.github.todd.hammerbootstrap.model.BootstrapVariable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import java.awt.Color
import java.io.InputStreamReader

@Service(Service.Level.APP)
class BootstrapVariableService {

    private val LOG = Logger.getInstance(BootstrapVariableService::class.java)
    private val gson = Gson()

    private val coreVariables: List<BootstrapVariable> by lazy {
        loadBundledVariables()
    }

    private var projectCustomVariables: List<BootstrapVariable> = emptyList()

    private fun loadBundledVariables(): List<BootstrapVariable> {
        val stream = javaClass.getResourceAsStream("/bootstrap/bootstrap-variables.json")
            ?: return emptyList()

        return try {
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<BootstrapVariable>>() {}.type
                val list: List<BootstrapVariable> = gson.fromJson(reader, type)
                list
            }
        } catch (e: Exception) {
            LOG.error("Failed to load bundled Bootstrap CSS variables", e)
            emptyList()
        }
    }

    fun getAllVariables(): List<BootstrapVariable> {
        return if (projectCustomVariables.isEmpty()) {
            coreVariables
        } else {
            projectCustomVariables + coreVariables
        }
    }

    fun getCategories(): List<String> {
        val allCats = LinkedHashSet<String>()
        allCats.add("All Categories")
        if (projectCustomVariables.isNotEmpty()) {
            allCats.add("Project Custom")
        }
        val catsFromVars = coreVariables.map { it.category }.distinct().sorted()
        allCats.addAll(catsFromVars)
        return allCats.toList()
    }

    fun search(
        query: String,
        categoryFilter: String = "All Categories",
        typeFilter: TypeFilter = TypeFilter.ALL
    ): List<BootstrapVariable> {
        return VariableMatcher.filterAndRank(
            items = getAllVariables(),
            query = query,
            categoryFilter = categoryFilter,
            typeFilter = typeFilter
        )
    }

    fun scanProjectVariables(project: Project) {
        val custom = mutableListOf<BootstrapVariable>()
        try {
            val projectScope = GlobalSearchScope.projectScope(project)
            val files = mutableListOf<VirtualFile>()

            // We can scan files under the project
            fun scanDir(file: VirtualFile) {
                if (file.isDirectory) {
                    if (file.name.startsWith(".") || file.name == "node_modules" || file.name == "vendor") return
                    for (child in file.children) {
                        scanDir(child)
                    }
                } else if (file.extension in listOf("css", "scss", "sass", "less")) {
                    files.add(file)
                }
            }

            val baseDir = project.guessProjectDir()
            if (baseDir != null) {
                scanDir(baseDir)
            }

            val varRegex = Regex("""(--bs-[a-zA-Z0-9_-]+)\s*:\s*([^;]+);""")
            val coreNames = coreVariables.map { it.name }.toSet()

            for (f in files) {
                try {
                    val content = String(f.contentsToByteArray(), Charsets.UTF_8)
                    for (match in varRegex.findAll(content)) {
                        val name = match.groupValues[1]
                        val rawValue = match.groupValues[2].trim()
                        if (name !in coreNames && custom.none { it.name == name }) {
                            val color = parseColor(rawValue)
                            custom.add(
                                BootstrapVariable(
                                    name = name,
                                    category = "Project Custom",
                                    defaultValue = rawValue,
                                    darkValue = null,
                                    scope = f.name,
                                    description = "Custom variable defined in ${f.name}",
                                    colorHex = color?.let { toHex(it) },
                                    isColor = color != null || rawValue.startsWith("#") || rawValue.startsWith("rgb"),
                                    isCustom = true
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error scanning project variables", e)
        }
        projectCustomVariables = custom
    }

    companion object {
        fun getInstance(): BootstrapVariableService {
            return ApplicationManager.getApplication().getService(BootstrapVariableService::class.java)
        }

        fun parseColor(str: String?): Color? {
            if (str.isNullOrBlank()) return null
            val s = str.trim().lowercase()

            // Hex format #rgb, #rrggbb, #rrggbbaa
            if (s.startsWith("#")) {
                val hex = s.removePrefix("#")
                try {
                    when (hex.length) {
                        3 -> {
                            val r = hex.substring(0, 1).repeat(2).toInt(16)
                            val g = hex.substring(1, 2).repeat(2).toInt(16)
                            val b = hex.substring(2, 3).repeat(2).toInt(16)
                            return Color(r, g, b)
                        }
                        6 -> {
                            val r = hex.substring(0, 2).toInt(16)
                            val g = hex.substring(2, 4).toInt(16)
                            val b = hex.substring(4, 6).toInt(16)
                            return Color(r, g, b)
                        }
                        8 -> {
                            val r = hex.substring(0, 2).toInt(16)
                            val g = hex.substring(2, 4).toInt(16)
                            val b = hex.substring(4, 6).toInt(16)
                            val a = hex.substring(6, 8).toInt(16)
                            return Color(r, g, b, a)
                        }
                    }
                } catch (_: Exception) {
                    return null
                }
            }

            // rgb(r, g, b) or rgba(r, g, b, a)
            if (s.startsWith("rgb")) {
                val regex = Regex("""rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([0-9.]+))?\s*\)""")
                val match = regex.find(s)
                if (match != null) {
                    val r = match.groupValues[1].toIntOrNull() ?: return null
                    val g = match.groupValues[2].toIntOrNull() ?: return null
                    val b = match.groupValues[3].toIntOrNull() ?: return null
                    val aStr = match.groupValues[4]
                    val a = if (aStr.isNotBlank()) {
                        val alphaFloat = aStr.toFloatOrNull() ?: 1.0f
                        (alphaFloat * 255).toInt().coerceIn(0, 255)
                    } else 255
                    return Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a)
                }
            }

            // Named colors
            return when (s) {
                "black" -> Color.BLACK
                "white" -> Color.WHITE
                "transparent" -> Color(0, 0, 0, 0)
                else -> null
            }
        }

        fun toHex(color: Color): String {
            return String.format("#%02x%02x%02x", color.red, color.green, color.blue)
        }
    }
}
