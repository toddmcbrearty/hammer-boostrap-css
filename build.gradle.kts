plugins {
    id("java")
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "com.github.todd"
version = "1.0.0"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        providers.gradleProperty("platformBundledPlugins").orNull?.takeIf { it.isNotBlank() }?.let {
            bundledPlugins(it.split(','))
        }
        pluginVerifier()
        zipSigner()
    }

    implementation("com.google.code.gson:gson:2.12.1")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.todd.hammerbootstrapcss"
        name = "Hammer Bootstrap CSS"
        version = project.version.toString()
        vendor {
            name = "Todd"
        }
        description = """
            <h3>Hammer Bootstrap CSS</h3>
            <p>
                A developer productivity plugin for PhpStorm and IntelliJ Platform IDEs that eliminates the friction of working with <b>Bootstrap 5.3+ CSS custom properties (<code>--bs-*</code>)</b>.
                Instead of leaving your editor to browse online documentation or manually digging through SCSS sources to find variable names, default values, and color codes, Hammer Bootstrap CSS gives you an instant, searchable catalog right inside your IDE.
            </p>
            <p>
                Whether you're customizing component themes, overriding dark mode values, or looking up typography and grid tokens, the plugin lets you search through over <b>446+ official Bootstrap variables</b> with live color swatches, resolved reference values, and component scopes. You can insert <code>var(--bs-*)</code> or bare variable names directly at your caret with a single keyboard shortcut or double-click.
            </p>
            <h4>Key Features:</h4>
            <ul>
                <li><b>Complete 446+ Variable Catalog:</b> Pre-indexed variables for colors, typography, buttons, forms, modals, tables, navbars, and root scopes.</li>
                <li><b>Visual Color Swatches:</b> High-contrast live color previews for 180+ hex, RGB, and resolved color variables across Light and Dark themes.</li>
                <li><b>Dark Mode Overrides:</b> Side-by-side view of default light values and <code>[data-bs-theme=dark]</code> overrides.</li>
                <li><b>Project Custom Variable Detection:</b> Automatically scans project <code>.css</code>/<code>.scss</code> files for custom <code>--bs-*</code> definitions.</li>
                <li><b>One-Click Insertion:</b> Double-click or <kbd>Shift+Enter</kbd> to insert variable name; <kbd>Enter</kbd> to insert <code>var(--bs-*)</code>.</li>
                <li><b>Tool Window & Quick Search Dialog:</b> Dockable right sidebar panel and floating modal dialog (<kbd>Shift+Alt+B</kbd>).</li>
            </ul>
            <h4>Keyboard Shortcuts:</h4>
            <ul>
                <li><b>Open Quick Search Dialog:</b> <kbd>Shift+Option+B</kbd> (macOS) / <kbd>Shift+Alt+B</kbd> (Win/Linux)</li>
                <li><b>Open Tool Window:</b> <kbd>Ctrl+Option+B</kbd> (macOS) / <kbd>Ctrl+Alt+B</kbd> (Win/Linux)</li>
                <li><b>Insert <code>var(--bs-*)</code>:</b> <kbd>Enter</kbd> or <kbd>Cmd/Ctrl+Click</kbd> on any list item</li>
                <li><b>Insert <code>--bs-*</code>:</b> <kbd>Shift+Enter</kbd> or <kbd>Double-Click</kbd> on any list item</li>
                <li><b>Insert Full CSS Rule:</b> <kbd>Cmd/Ctrl+Enter</kbd> (e.g. <code>--bs-primary: #0d6efd;</code>)</li>
                <li><b>Copy <code>var(...)</code>:</b> <kbd>Cmd/Ctrl+C</kbd> | <b>Copy Name:</b> <kbd>Cmd/Ctrl+Shift+C</kbd></li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
