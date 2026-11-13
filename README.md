# CodeReview Buddy 🔍☕

**CodeReview Buddy** is an offline, desktop-based **Java Static Code & Complexity Analyzer** built with **Java 21**, **JavaParser**, **JavaFX**, and **OpenPDF**. It parses Java source files (`.java`) directly into Abstract Syntax Trees (AST) without compiling or executing target codebases—detecting structural code smells, calculating complexity metrics, computing a Repository Health Score, and generating downloadable PDF audit reports.

---

## ✨ Key Features

- **Offline AST Parsing Engine:** Recursively scans arbitrary Java project folders without needing external compilers or network access.
- **Automated Code Smell Detection:**
  - **Cyclomatic Complexity (`HIGH`):** Flags methods exceeding McCabe Cyclomatic Complexity thresholds (`> 10`).
  - **Excessive Nesting Depth (`HIGH`):** Detects deeply nested conditional and loop blocks (`> 3`).
  - **Long Method (`MEDIUM`):** Identifies oversized methods (`> 30` lines) that need decomposition.
  - **Unused Imports (`LOW`):** Catches unreferenced `import` statements to keep source files clean.
- **Interactive JavaFX Analytics Dashboard:**
  - Real-time KPI summary cards (**Health Score**, **Files Scanned**, **Avg Complexity**, **Total Issues**).
  - Severity distribution pie chart (`HIGH`, `MEDIUM`, `LOW`).
  - Ranked list of most complex files.
  - Searchable and severity-filterable issue table with line numbers and refactoring suggestions.
- **One-Click Self-Scan (`Scan Self (src/)`):** Built-in self-diagnostic mode to benchmark the analyzer against its own source code.
- **PDF Audit Export (`Export PDF Report`):** Generates structured multi-page PDF reports powered by **OpenPDF**.

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
| :--- | :--- | :--- |
| **Language & Runtime** | Java OpenJDK | 21 (LTS) |
| **Build Tool** | Apache Maven | 3.9+ |
| **AST Parsing** | JavaParser | 3.28.2 |
| **Desktop UI & Charts** | JavaFX (OpenJFX) | 21.0.13 |
| **PDF Generation** | OpenPDF | 2.2.5 |
| **Unit Testing** | JUnit 5 (Jupiter) | 5.10.x |

---

## 📂 Project Architecture

```text
src/
├── main/java/com/codereview/
│   ├── analyzer/       # AstAnalyzerService & ComplexityVisitor (AST traversal)
│   ├── engine/         # RuleEngine (threshold evaluation & severity mapping)
│   ├── model/          # CodeIssue, FileReport, ProjectReport (metrics & scoring)
│   ├── report/         # PdfReportExporter (OpenPDF report generation)
│   └── ui/             # MainDashboardView & JavaFX entry point
└── test/java/          # JUnit 5 unit tests for AST analysis & rule validation
```

---

## 🚀 Getting Started

### Prerequisites
- **JDK 21** or higher installed and configured in `JAVA_HOME`
- **Apache Maven 3.9+**

### Run the Application
1. **Clone the repository:**
   ```bash
   git clone https://github.com/badalkumar7/CodeReview-Buddy.git
   cd CodeReview-Buddy
   ```

2. **Run unit tests:**
   ```bash
   mvn test
   ```

3. **Launch the JavaFX desktop application:**
   ```bash
   mvn clean javafx:run
   ```

---

## 📊 Self-Scan Benchmark

Running **Scan Self (`src/`)** on CodeReview Buddy's own codebase produces:
- **Health Score:** `98 / 100 • A`
- **Files Scanned:** `10`
- **Average Complexity:** `1.89`
- **Total Issues Detected:** `5` (`2 HIGH`, `1 MEDIUM`, `2 LOW`)

---

## 👤 Author

**Badal Kumar**  
GitHub: [@badalkumar7](https://github.com/badalkumar7)