# Project Rules: CodeReview Buddy
- **Language & Build:** Java 21 (LTS) + Apache Maven (`pom.xml`).
- **Libraries:**
  - `com.github.javaparser:javaparser-core` (3.28.2)
  - `org.openjfx:javafx-controls` & `javafx-fxml` (21.0.13)
  - `com.github.librepdf:openpdf` (2.2.5)
  - `org.junit.jupiter:junit-jupiter` (5.11.4)
- **Architecture:** Clean MVC/Service separation under `com.codereview.{model,analyzer,report,ui}`.
- **UI Rule:** Build the JavaFX UI programmatically in pure Java (no `.fxml` files) to avoid classpath resource loading bugs.
- **Verification:** Always run `mvn test` after making changes to verify compilation and tests pass.