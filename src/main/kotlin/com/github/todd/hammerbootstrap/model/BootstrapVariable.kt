package com.github.todd.hammerbootstrap.model

data class BootstrapVariable(
    val name: String,
    val category: String,
    val defaultValue: String,
    val resolvedValue: String? = null,
    val darkValue: String? = null,
    val scope: String = ":root",
    val description: String = "",
    val colorHex: String? = null,
    val isColor: Boolean = false,
    val isCustom: Boolean = false
) {
    val varExpression: String
        get() = "var($name)"

    val effectiveDefaultValue: String
        get() = resolvedValue?.takeIf { it.isNotBlank() } ?: defaultValue

    val declaration: String
        get() = "$name: $defaultValue;"

    val resolvedDeclaration: String
        get() = "$name: $effectiveDefaultValue;"

    val darkDeclaration: String?
        get() = darkValue?.let { "$name: $it;" }

    val cleanName: String
        get() = if (name.startsWith("--bs-")) name.removePrefix("--bs-") else name.removePrefix("--")
}
