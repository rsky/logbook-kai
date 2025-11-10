# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Logbook-kai is a JavaFX-based desktop application for KanColle (Kantai Collection) ship-girls management. It's a proxy server tool that captures and analyzes game communication to help players manage their fleets, equipment, expeditions, and battles.

## Development Commands

### Build Commands
- `./gradlew shadowJar` - Build executable JAR file (output: `build/libs/logbook-kai-<version>-all.jar`)
- `./gradlew package` - Build JAR with launch scripts in ZIP archive (output: `build/distributions/`)
- `./gradlew clean` - Clean build artifacts

### Testing
- `./gradlew test` - Run unit tests
- Tests are located in `src/test/java/`

### Platform-Specific Builds
- `./gradlew macApp` - Build macOS application (requires Xcode Command Line Tools)
- `./gradlew macDmg` - Build macOS disk image
- `gradlew.bat winApp` - Build Windows application (requires WiX Toolset v3.x)
- `gradlew.bat winMsi` - Build Windows installer

## Architecture

### Core Components

**Main Application Entry Point:**
- `src/main/java/logbook/internal/Launcher.java` - Application launcher
- `src/main/java/logbook/internal/gui/Main.java` - JavaFX main class

**Proxy System:**
- `src/main/java/logbook/internal/proxy/` - Proxy server implementation using Jetty
- `src/main/java/logbook/proxy/` - Proxy server interfaces for plugin extension

**API Processing:**
- `src/main/java/logbook/api/` - API listener implementations for game data capture
- Each API endpoint has its own handler class (e.g., `ApiGetMemberShip2.java`)

**Data Models:**
- `src/main/java/logbook/bean/` - Data beans for ships, equipment, battles, etc.
- Uses JSON for data serialization with Glassfish JSON library

**GUI Controllers:**
- `src/main/java/logbook/internal/gui/` - JavaFX controllers for all UI components
- FXML files in `src/main/resources/logbook/gui/`

**Plugin System:**
- `src/main/java/logbook/plugin/` - Plugin interfaces using ServiceLoader pattern
- Supports lifecycle hooks, GUI extensions, and data processing plugins

### Key Dependencies
- **JavaFX 21** - GUI framework (controls, FXML, media, swing, web)
- **Jetty 11** - Embedded HTTP server/proxy
- **Jackson 2** - JSON processing
- **ControlsFX** - Additional JavaFX controls
- **Lombok** - Code generation (annotations)
- **Logback** - Logging framework

### Data Flow
1. Game traffic is captured via proxy server (`logbook.internal.proxy`)
2. API responses are parsed by API listeners (`logbook.api.*`)
3. Data is stored in bean objects (`logbook.bean.*`)
4. GUI controllers (`logbook.internal.gui.*`) display and manipulate data
5. Configuration and persistent data uses XDG Base Directory Specification

### Configuration
- Uses XDG Base Directory Specification
- Config files: `~/.config/logbook-kai/` (or `$XDG_CONFIG_HOME`)
- Data files: `~/.local/share/logbook-kai/` (or `$XDG_DATA_HOME`)
- Can be overridden with `LOGBOOK_KAI_CONFIG_DIR` and `LOGBOOK_KAI_DATA_DIR`

### Plugin Development
The application supports plugin extensions through ServiceLoader interfaces:
- `logbook.plugin.lifecycle.StartUp` - Startup hooks
- `logbook.plugin.gui.*` - GUI extensions (menu items, fleet tab remarks)
- `logbook.api.APIListenerSpi` - API data processing
- `logbook.proxy.ContentListenerSpi` - HTTP traffic capture

### Special Features
- **Passive Mode API** - Accepts HTTP POST data from external tools
- **Audio Notifications** - For expeditions and repairs
- **Screenshot Capture** - Battle and fleet screenshots
- **Battle Log Analysis** - Detailed battle statistics and reports