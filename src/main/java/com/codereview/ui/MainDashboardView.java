package com.codereview.ui;

import com.codereview.analyzer.AstAnalyzerService;
import com.codereview.model.CodeIssue;
import com.codereview.model.FileReport;
import com.codereview.model.ProjectReport;
import com.codereview.report.PdfReportExporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Programmatic JavaFX dashboard for scanning projects and reviewing findings. */
public class MainDashboardView extends BorderPane {
    private final Stage stage;
    private final AstAnalyzerService analyzer = new AstAnalyzerService();
    private final ObservableList<CodeIssue> issues = FXCollections.observableArrayList();
    private final TableView<CodeIssue> table = new TableView<>();
    private final PieChart pie = new PieChart();
    private final ListView<FileReport> topFiles = new ListView<>();
    private final Label health = new Label("—");
    private final Label files = new Label("0");
    private final Label complexity = new Label("0.00");
    private final Label issueCount = new Label("0");
    private final Label status = new Label("Choose a project folder to begin");
    private ProjectReport report;

    public MainDashboardView(Stage stage) {
        this.stage = stage;
        setPadding(new Insets(18));
        setStyle("-fx-background-color:#f4f7fb;");
        build();
    }

    private void build() {
        Button select = new Button("Select Project Folder");
        Button selfScan = new Button("Scan Self (src/)");
        Button export = new Button("Export PDF Report");
        select.setOnAction(event -> choose());
        selfScan.setOnAction(event -> {
            Path sourcePath = Path.of("src");
            if (Files.isDirectory(sourcePath)) {
                scan(sourcePath);
            } else {
                status.setText("No src/ folder found in the current directory.");
            }
        });
        export.setOnAction(event -> export());

        HBox top = new HBox(12, select, selfScan, export, status);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(4, 0, 18, 0));
        setTop(top);

        HBox cards = new HBox(
                12,
                card("HEALTH SCORE", health),
                card("FILES SCANNED", files),
                card("AVG COMPLEXITY", complexity),
                card("TOTAL ISSUES", issueCount));

        pie.setTitle("Issues by severity");
        pie.setPrefHeight(260);
        topFiles.setPrefHeight(220);
        topFiles.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(FileReport fileReport, boolean empty) {
                super.updateItem(fileReport, empty);
                if (empty || fileReport == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f  %s", fileReport.getAverageComplexity(),
                            Path.of(fileReport.getFilePath()).getFileName()));
                }
            }
        });

        VBox left = new VBox(
                14,
                new Label("Severity distribution"),
                pie,
                new Label("Most complex files"),
                topFiles);
        left.setPrefWidth(340);
        left.setPadding(new Insets(0, 16, 0, 0));

        TextField search = new TextField();
        search.setPromptText("Search issues...");
        ComboBox<String> severity = new ComboBox<>(FXCollections.observableArrayList(
                "All severities", "HIGH", "MEDIUM", "LOW"));
        severity.getSelectionModel().selectFirst();

        FilteredList<CodeIssue> filtered = new FilteredList<>(issues, issue -> true);
        search.textProperty().addListener((observable, oldValue, newValue) ->
                filter(filtered, search.getText(), severity.getValue()));
        severity.valueProperty().addListener((observable, oldValue, newValue) ->
                filter(filtered, search.getText(), newValue));
        table.setItems(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        addColumn("Severity", 90, issue -> issue.getSeverity().name());
        addColumn("Rule", 145, CodeIssue::getRuleName);
        addColumn("File", 180, CodeIssue::getFileName);
        addColumn("Line", 60, issue -> String.valueOf(issue.getLineNumber()));
        addColumn("Finding", 260, CodeIssue::getMessage);
        addColumn("Suggestion", 260, CodeIssue::getSuggestion);

        HBox filters = new HBox(10, search, severity);
        VBox right = new VBox(10, new Label("Code issues"), filters, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        right.setPadding(new Insets(0, 0, 0, 12));

        SplitPane lower = new SplitPane(left, right);
        lower.setDividerPositions(.28);
        VBox content = new VBox(16, cards, lower);
        VBox.setVgrow(lower, Priority.ALWAYS);
        setCenter(content);

        String style = ".label{-fx-text-fill:#203047;-fx-font-family:'Segoe UI';}"
                + ".button{-fx-background-color:#2864dc;-fx-text-fill:white;"
                + "-fx-background-radius:8;-fx-padding:10 14;}"
                + ".table-view,.list-view,.chart{-fx-background-radius:10;}";
        getStylesheets().add("data:text/css,"
                + java.net.URLEncoder.encode(style, StandardCharsets.UTF_8));
    }

    private void addColumn(String title, int width, Function<CodeIssue, String> value) {
        TableColumn<CodeIssue, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private void filter(FilteredList<CodeIssue> filtered, String query, String severity) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        filtered.setPredicate(issue ->
                (severity == null || severity.startsWith("All")
                        || issue.getSeverity().name().equals(severity))
                        && (normalizedQuery.isBlank()
                        || (issue.getMessage() + issue.getSuggestion() + issue.getFileName()
                        + issue.getRuleName()).toLowerCase(Locale.ROOT).contains(normalizedQuery)));
    }

    private VBox card(String title, Label value) {
        value.setStyle("-fx-font-size:25px;-fx-font-weight:bold;");
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size:11px;-fx-text-fill:#718096;");

        VBox box = new VBox(8, heading, value);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void choose() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a Java project");
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            scan(selectedDirectory.toPath());
        }
    }

    private void scan(Path path) {
        status.setText("Scanning…");
        Task<ProjectReport> task = new Task<>() {
            @Override
            protected ProjectReport call() throws Exception {
                return analyzer.analyze(path);
            }
        };

        task.setOnSucceeded(event -> {
            report = task.getValue();
            issues.setAll(report.getAllIssues());
            health.setText(report.getHealthScore() + " / 100 · " + report.getGrade());
            files.setText(String.valueOf(report.getTotalFiles()));
            complexity.setText(String.format("%.2f", report.getAverageComplexity()));
            issueCount.setText(String.valueOf(issues.size()));
            status.setText("Scanned " + report.getProjectPath());
            refreshCharts(report);
        });
        task.setOnFailed(event -> {
            status.setText("Scan failed: " + task.getException().getMessage());
            new Alert(Alert.AlertType.ERROR,
                    "Could not scan folder: " + task.getException().getMessage()).show();
        });

        Thread thread = new Thread(task, "code-review-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshCharts(ProjectReport projectReport) {
        Map<CodeIssue.Severity, Long> counts = new EnumMap<>(CodeIssue.Severity.class);
        for (CodeIssue.Severity severity : CodeIssue.Severity.values()) {
            long count = projectReport.getAllIssues().stream()
                    .filter(issue -> issue.getSeverity() == severity)
                    .count();
            counts.put(severity, count);
        }

        pie.getData().setAll(counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new PieChart.Data(
                        entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()))
                .toList());

        topFiles.getItems().setAll(projectReport.getFileReports().stream()
                .sorted(Comparator.comparingDouble(FileReport::getAverageComplexity).reversed())
                .limit(10)
                .toList());
    }

    private void export() {
        if (report == null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Run a scan before exporting a report.").show();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export PDF report");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("code-review-report.pdf");
        File destination = chooser.showSaveDialog(stage);
        if (destination != null) {
            try {
                new PdfReportExporter().export(report, destination.toPath());
                status.setText("Report saved: " + destination.getAbsolutePath());
            } catch (Exception exception) {
                new Alert(Alert.AlertType.ERROR,
                        "PDF export failed: " + exception.getMessage()).show();
            }
        }
    }
}
