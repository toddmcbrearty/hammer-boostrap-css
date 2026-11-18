# Hammer Bootstrap CSS Plugin for PhpStorm & IntelliJ Platform

A JetBrains / PhpStorm plugin that provides a lightning-fast, searchable catalog and productivity toolkit for all **Bootstrap CSS custom properties (CSS variables)**.

---

## Description

**Hammer Bootstrap CSS** is a developer productivity plugin for PhpStorm and IntelliJ Platform IDEs that eliminates the friction of working with Bootstrap 5.3+ CSS custom properties (`--bs-*`). Instead of leaving your editor to browse online documentation or manually digging through SCSS sources to find variable names, default values, and color codes, Hammer Bootstrap CSS gives you an instant, searchable catalog right inside your IDE.

Whether you're customizing component themes, overriding dark mode values, or looking up typography and grid tokens, the plugin lets you search through over **446+ official Bootstrap variables** with live color swatches, resolved reference values, and component scopes. You can insert `var(--bs-*)` or bare variable names directly at your caret with a single keyboard shortcut or double-click, dramatically speeding up front-end and full-stack development.

### Quick Keyboard Shortcuts
| Action | macOS | Windows / Linux | Location / Context |
| :--- | :--- | :--- | :--- |
| **Open Quick Search Dialog** | `Shift + Option + B` | `Shift + Alt + B` | Global Editor shortcut |
| **Open Tool Window** | `Ctrl + Option + B` | `Ctrl + Alt + B` | Tools → Hammer Bootstrap CSS |
| **Insert `var(--bs-*)`** | `Enter` or `Cmd + Click` | `Enter` or `Ctrl + Click` | Inside Dialog or Tool Window |
| **Insert bare `--bs-*`** | `Shift + Enter` or `Double-Click` | `Shift + Enter` or `Double-Click` | Inside Dialog or Tool Window |
| **Insert full CSS rule** | `Cmd + Enter` | `Ctrl + Enter` | Inside Dialog or Tool Window |
| **Copy `var(...)`** | `Cmd + C` | `Ctrl + C` | Inside Dialog or Tool Window |
| **Copy bare `--bs-*`** | `Cmd + Shift + C` | `Ctrl + Shift + C` | Inside Dialog or Tool Window |

---

## Features

- **Complete Bootstrap 5.3+ CSS Variable Catalog**:
  - Over **446+ official variables** pre-indexed across all components and root scopes.
  - Includes theme colors, body typography, grid gutters, buttons, cards, modals, navbars, forms, tables, alerts, badges, dropdowns, and more.
- **Smart, Instant Search & Ranking**:
  - Live, debounced search as you type.
  - Supports variable name (e.g. `--bs-primary`), clean name without prefix (`primary`, `body-bg`), value (`#0d6efd`, `1rem`), component scope (`.btn`), category (`Buttons`), or description.
  - Multi-token search: searching `btn bg` directly matches `--bs-btn-bg`.
- **Visual Color Swatches**:
  - Crisp live color previews for 180+ color variables (hex, RGB, RGBA, and resolved references).
  - High-contrast borders ensuring visibility across both Light and Dark IDE themes.
- **Dark Mode Overrides**:
  - Side-by-side view of default light values and `[data-bs-theme=dark]` overrides.
- **Filtering Options**:
  - Filter by component category (All, Colors & Theme, Buttons, Forms, Modals, Navbar, Tables, etc.).
  - Filter by type: *All Variables*, *Colors Only*, *With Dark Mode Override*, *Root Scoped Only*, *Component Scoped Only*.
- **Project Custom Variable Detection**:
  - Automatically scans `.css` and `.scss` files in the open project for custom `--bs-*` variables and includes them with a `[Project Custom]` badge.
- **One-Click Editor Insertion & Copy**:
  - **Double-Click**: Inserts the bare variable name `--bs-...` directly at the editor cursor.
  - **Enter**: Inserts `var(--bs-...)` at the current editor cursor.
  - **Shift + Enter**: Inserts the variable name `--bs-...`.
  - **Ctrl/Cmd + Enter**: Inserts full CSS declaration (e.g. `--bs-primary: #0d6efd;`).
- **Dedicated Bottom Action Toolbar & Keyboard Shortcuts**:
  - **Insert `var(...)`** (`insertVar.svg`): Inserts `var(--bs-...)` at editor caret (`Enter`).
  - **Insert `--bs-...`** (`insertName.svg`): Inserts bare variable name at editor caret (`Shift + Enter` or `Double-Click`).
  - **Copy `var(...)`** (`copyVar.svg`): Copies `var(--bs-...)` to clipboard (`Cmd/Ctrl + C`).
  - **Copy `--bs-...`** (`copyName.svg`): Copies bare variable name `--bs-...` to clipboard (`Cmd/Ctrl + Shift + C`).
- **Dual UI: Tool Window + Quick Modal Dialog**:
  - Dockable **Bootstrap CSS** tool window on the right side.
  - Floating **Quick Search Dialog** for fast keyboard-driven insertion without leaving your editor.

---

## Keyboard Shortcuts

| Shortcut (macOS) | Shortcut (Windows/Linux) | Action |
| :--- | :--- | :--- |
| `Ctrl + Alt + B` (or `Cmd + Alt + B`) | `Ctrl + Alt + B` | Focus Bootstrap CSS Tool Window & Search |
| `Shift + Alt + B` | `Shift + Alt + B` | Open Floating Quick Search Dialog |
| `Cmd + Click` | `Ctrl + Click` | Insert `var(--bs-...)` at active editor caret |
| `Double-Click` | `Double-Click` | Insert bare variable name `--bs-...` at caret |
| `Enter` | `Enter` | Insert `var(--bs-...)` at active editor caret |
| `Shift + Enter` | `Shift + Enter` | Insert variable name `--bs-...` at caret |
| `Cmd + C` | `Ctrl + C` | Copy `var(--bs-...)` to clipboard |
| `Cmd + Shift + C` | `Ctrl + Shift + C` | Copy bare variable name `--bs-...` to clipboard |
| `Cmd/Ctrl + Enter` | `Ctrl + Enter` | Insert full CSS declaration at caret |
| `↑` / `↓` | `↑` / `↓` | Navigate variable list directly from the search field |

*(Shortcuts can be customized in **Settings → Keymap → search "Bootstrap CSS"**)*

---

## Accessing the Plugin

1. **Right Tool Window**: Click the **Bootstrap CSS** tab on the right tool window bar.
2. **Tools Menu**: Navigate to **Tools → Hammer Bootstrap CSS**.
3. **Navigate Menu**: Navigate to **Navigate → Bootstrap CSS Variable...** (or press `Ctrl+Alt+B`).
4. **Editor Context Menu**: Right-click anywhere in your HTML, CSS, Blade, PHP, or JavaScript file and select **Insert Bootstrap CSS Variable...**.

---

## Development & Building

### Prerequisites
- JDK 24+ (target runtime is JDK 25 / PhpStorm 2026.3 JBR)
- Gradle (managed via the included `./gradlew` wrapper)

### Build the Plugin Distribution ZIP
```bash
./gradlew buildPlugin
```
The packaged plugin file will be generated at:
```
build/distributions/hammer-bootstrap-css-1.0.0.zip
```

### Run Tests
```bash
./gradlew test
```

### Launch Sandboxed IDE for Testing
```bash
./gradlew runIde
```
