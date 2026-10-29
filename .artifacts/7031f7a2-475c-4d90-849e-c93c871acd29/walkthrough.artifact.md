# AnaliZer Application - Feature Walkthrough & Verification

AnaliZer is a modern Android data analytics application built with Jetpack Compose, Material 3, Jetpack Navigation 3, Apache POI (for spreadsheet processing), and Vico (for interactive data visualization).

## Key Features Implemented

### 1. Spreadsheet Import (Excel & Google Sheets)
- **Excel Support**: Full parsing of `.xlsx` and `.xls` files via Apache POI. Handles formula evaluation, text, numeric, date, and boolean cell types robustly.
- **Google Sheets Integration**: Seamless import from public Google Sheets export links (CSV export format & direct URL parsing).
- **Sample Data**: Built-in sample datasets (e.g., Financial Performance, Sales Analytics) for instant offline exploration.

### 2. Sheet, Row Range & Column Configuration
- **Sheet Selection**: Dynamic dropdown / sheet selector for multi-sheet workbooks.
- **Row Range Filtering**: Configurable header row and data row bounds (Start row & End row).
- **Column Customization & Renaming**: Toggle columns to include/exclude and override header names with custom aliases.

### 3. Descriptive Statistics Engine
Computes comprehensive statistical metrics across numerical columns:
- **Count**: Total valid numerical data points.
- **Sum**: Cumulative total.
- **Mean (Average)**: Arithmetic mean.
- **Median**: Middle value for skewed distributions.
- **Min / Max**: Range boundaries.
- **Standard Deviation**: Measure of data dispersion / variability.

### 4. Interactive Charting with Vico
- **Bar Charts**: Visualizing categorical comparisons and totals.
- **Line Charts**: Trend analysis over rows or sequential time series.
- **Dynamic Scaling & Styling**: Material 3 color-adapted charts with interactive tooltips and axis labels.

### 5. Jetpack Compose UI & Navigation 3
- **Edge-to-Edge Material 3 Design**: Dynamic color schemes, expressive layout components, and polished dark/light themes.
- **Navigation 3**: Type-safe navigation (`NavDisplay` and serializable `NavKey` objects) linking Import, Configuration, and Analytics screens.
- **State Management**: Robust `ViewModel` Architecture holding UI state (`ImportState`, `ConfigState`, `AnalyticsState`) with clean unidirectional data flow.

---

## Verification & Test Results

- **Unit Tests**:
  - `StatisticsEngineTest`: Validates calculations for mean, median, min, max, sum, count, and standard deviation against edge cases (empty lists, single values, negative numbers).
  - `SpreadsheetRepositoryTest`: Verifies robust parsing of workbook streams and headers.
  - `ChartDataMapperTest`: Tests data transformation into Vico model structures.
- **Build & Test Status**:
  - `./gradlew :app:testDebugUnitTest` executed successfully with **ALL TESTS PASSED**.
  - `./gradlew :app:assembleDebug` completed successfully with zero warnings or errors.
