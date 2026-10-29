# Project Plan

Crie um aplicativo Android que analisa planilhas Excel (.xlsx/.xls) e Google Sheets (via link publico/compartilhado). O aplicativo deve permitir configurar abas, colunas e intervalo de linhas a serem analisadas, permitir definir/renomear titulos de colunas (caso nao existam na planilha), calcular estatisticas descritivas (maximo, minimo, media, mediana, soma, contagem, etc.) e exibir graficos interativos (graficos de barras, linhas, pizza).

## Project Brief

# Project Brief: Spreadsheet Analyzer App

## Features
1. **Excel & Google Sheets Import**: Import local Excel files (`.xlsx` / `.xls`) using the Storage Access Framework and fetch public Google Sheets documents via shared links.
2. **Data Range & Column Configuration**: Select specific worksheet tabs, define row/column ranges, and define or rename column headers.
3. **Descriptive Statistics Engine**: Compute core descriptive metrics including minimum, maximum, mean, median, sum, and count for configured columns.
4. **Interactive Data Visualization**: Render interactive charts (bar, line, and pie charts) based on analyzed numerical data.

## High-Level Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation & Adaptive Strategy**: **Jetpack Navigation 3** (state-driven) and **Compose Material Adaptive** library
- **Architecture & Asynchronous Engine**: ViewModel, Kotlin Coroutines, and Flow
- **Spreadsheet Parsing & Networking**: Apache POI (Excel file parsing) and OkHttp (fetching Google Sheets data)
- **Charting Library**: Vico (Compose-native charting)

## Implementation Steps
**Total Duration:** 21m 44s

### Task_1_DependenciesAndCoreModels: Add dependencies for Apache POI, OkHttp, Vico chart library, Navigation 3, and setup core data models for Spreadsheet, DataRange, and ColumnStats.
- **Status:** COMPLETED
- **Updates:** Dependencies for Apache POI, OkHttp, and Vico charts added. Core data models (SpreadsheetData, AnalysisConfig, ColumnStats) created. Build passes successfully.
- **Acceptance Criteria:**
  - build pass
  - dependencies and core data models configured successfully
- **Duration:** 11m 36s

### Task_2_SpreadsheetImportAndParsingEngine: Implement file import via Storage Access Framework (Excel .xlsx/.xls parsing using Apache POI) and URL import (Google Sheets CSV fetching via OkHttp), supporting sheet and range extraction.
- **Status:** COMPLETED
- **Updates:** SpreadsheetRepository implemented with local Excel parsing (Apache POI), Google Sheets URL fetching/CSV export via OkHttp, and content resolver loading. Verified with build and unit tests.
- **Acceptance Criteria:**
  - build pass
  - Excel parsing and Google Sheets fetch support working
- **Duration:** 1m 28s

### Task_3_StatsAndChartEngine: Build Descriptive Statistics Engine (minimum, maximum, mean, median, sum, count calculation) and chart data transformation logic for Bar, Line, and Pie charts.
- **Status:** COMPLETED
- **Updates:** StatisticsEngine and ChartDataMapper implemented with full statistical calculations (min, max, mean, median, sum, count, stdDev), flexible number parsing, and Vico chart model mapping. Verified with unit tests and build.
- **Acceptance Criteria:**
  - build pass
  - descriptive statistics engine and chart data processor calculated accurately
- **Duration:** 58s

### Task_4_ComposeUIAndNavigation: Build Jetpack Compose UI screens (Spreadsheet Import Screen, Range & Column Config Screen, Stats & Charts Analytics Screen) wired with Navigation 3.
- **Status:** COMPLETED
- **Updates:** Jetpack Compose UI screens (ImportScreen, ConfigScreen, AnalyticsScreen), AnalyzerViewModel, and Navigation integration implemented with Material 3 and Vico interactive charts. Build and unit tests pass successfully.
- **Acceptance Criteria:**
  - build pass
  - app UI allows spreadsheet loading, range selection, stats display, and interactive chart rendering
- **Duration:** 3m 21s

### Task_5_RunAndVerify: Run and verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Critic agent successfully verified app stability, import screen, configuration screen, statistics engine, and Vico charts on the Android emulator. Zero crashes or critical UI issues found.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - app functionality and stability verified
- **Duration:** 4m 21s

