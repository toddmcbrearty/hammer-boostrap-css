package com.github.todd.hammerbootstrap.matcher

import com.github.todd.hammerbootstrap.model.BootstrapVariable

enum class TypeFilter(val displayName: String) {
    ALL("All Variables"),
    COLORS_ONLY("Colors Only"),
    HAS_DARK_MODE("With Dark Override"),
    ROOT_ONLY("Root (:root) Scoped"),
    COMPONENT_ONLY("Component Scoped")
}

object VariableMatcher {

    data class MatchResult(
        val item: BootstrapVariable,
        val score: Int
    )

    fun matches(
        item: BootstrapVariable,
        query: String,
        categoryFilter: String = "All Categories",
        typeFilter: TypeFilter = TypeFilter.ALL
    ): MatchResult? {
        // Category check
        if (categoryFilter != "All Categories" && !item.category.equals(categoryFilter, ignoreCase = true)) {
            return null
        }

        // Type filter check
        when (typeFilter) {
            TypeFilter.ALL -> Unit
            TypeFilter.COLORS_ONLY -> if (!item.isColor && item.colorHex == null) return null
            TypeFilter.HAS_DARK_MODE -> if (item.darkValue.isNullOrBlank()) return null
            TypeFilter.ROOT_ONLY -> if (item.scope != ":root") return null
            TypeFilter.COMPONENT_ONLY -> if (item.scope == ":root") return null
        }

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return MatchResult(item, 0)
        }

        val q = trimmed.lowercase()
        val nameLower = item.name.lowercase()
        val cleanNameLower = item.cleanName.lowercase()
        val valLower = item.defaultValue.lowercase()
        val resolvedValLower = (item.resolvedValue ?: "").lowercase()
        val darkValLower = (item.darkValue ?: "").lowercase()
        val descLower = item.description.lowercase()
        val catLower = item.category.lowercase()
        val scopeLower = item.scope.lowercase()

        var score = 0

        // 1. Exact match on name
        if (nameLower == q || cleanNameLower == q) {
            score += 2000
        }
        // 2. Prefix match on name or clean name
        else if (nameLower.startsWith(q)) {
            score += 1500
        } else if (cleanNameLower.startsWith(q)) {
            score += 1400
        }
        // 3. Substring match on name
        else if (nameLower.contains(q)) {
            score += 1000
        }
        // 4. Exact match on default, resolved, or dark value
        else if (valLower == q || resolvedValLower == q || darkValLower == q) {
            score += 900
        }
        // 5. Value prefix or contains
        else if (valLower.startsWith(q) || resolvedValLower.startsWith(q) || darkValLower.startsWith(q)) {
            score += 800
        } else if (valLower.contains(q) || resolvedValLower.contains(q) || darkValLower.contains(q)) {
            score += 600
        }
        // 6. Category match
        else if (catLower.startsWith(q)) {
            score += 500
        } else if (catLower.contains(q)) {
            score += 400
        }
        // 7. Scope match
        else if (scopeLower.contains(q)) {
            score += 350
        }
        // 8. Description match
        else if (descLower.contains(q)) {
            score += 300
        }

        // 9. Token matching: split query into space/hyphen tokens e.g. "btn bg" -> matches "--bs-btn-bg"
        val tokens = q.split(Regex("[\\s-]+")).filter { it.isNotBlank() }
        if (tokens.size > 1) {
            val allTokensMatchName = tokens.all { nameLower.contains(it) }
            if (allTokensMatchName) {
                score += 1100
            } else {
                val allTokensMatchAny = tokens.all { token ->
                    nameLower.contains(token) ||
                            valLower.contains(token) ||
                            descLower.contains(token) ||
                            catLower.contains(token)
                }
                if (allTokensMatchAny) {
                    score += 500
                }
            }
        }

        if (score == 0) {
            return null
        }

        // Slight bonus for shorter names (more specific / root variables)
        val lengthBonus = (100 - item.name.length).coerceAtLeast(0)
        score += lengthBonus

        return MatchResult(item, score)
    }

    fun filterAndRank(
        items: List<BootstrapVariable>,
        query: String,
        categoryFilter: String = "All Categories",
        typeFilter: TypeFilter = TypeFilter.ALL
    ): List<BootstrapVariable> {
        val matches = ArrayList<MatchResult>(items.size)
        for (item in items) {
            val res = matches(item, query, categoryFilter, typeFilter)
            if (res != null) {
                matches.add(res)
            }
        }

        if (query.isBlank()) {
            return matches.map { it.item }
        }

        matches.sortWith(
            compareByDescending<MatchResult> { it.score }
                .thenBy { it.item.name }
        )

        return matches.map { it.item }
    }
}
