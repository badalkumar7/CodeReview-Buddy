package com.codereview.ui;
import com.codereview.analyzer.AstAnalyzerService;
import com.codereview.model.*;
import com.codereview.report.PdfReportExporter;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.nio.file.*;
import java.util.*;

/** Programmatic JavaFX dashboard for scanning projects and reviewing findings. */
public class MainDashboardView extends BorderPane {
    private final Stage stage; private final AstAnalyzerService analyzer=new AstAnalyzerService(); private ProjectReport report;
    private final ObservableList<CodeIssue> issues=FXCollections.observableArrayList(); private final TableView<CodeIssue> table=new TableView<>();
    private final Label health=new Label("—"),files=new Label("0"),complexity=new Label("0.00"),issueCount=new Label("0"),status=new Label("Choose a project folder to begin");
    /** Creates a dashboard attached to the supplied application stage. */
    public MainDashboardView(Stage stage){this.stage=stage;setPadding(new Insets(18));setStyle("-fx-background-color:#f4f7fb;");build();}
    private void build(){
        Button select=button("Select Project Folder"),self=button("Scan Self (src/)"),export=button("Export PDF Report");select.setOnAction(e->choose());self.setOnAction(e->{Path p=Path.of("src");if(Files.isDirectory(p))scan(p);else status.setText("No src/ folder found in the current directory.");});export.setOnAction(e->export());
        HBox top=new HBox(12,select,self,export,status);top.setAlignment(Pos.CENTER_LEFT);top.setPadding(new Insets(4,0,18,0));setTop(top);
        HBox cards=new HBox(12,card("HEALTH SCORE",health),card("FILES SCANNED",files),card("AVG COMPLEXITY",complexity),card("TOTAL ISSUES",issueCount));
        PieChart pie=new PieChart();pie.setTitle("Issues by severity");pie.setPrefHeight(260);
        ListView<FileReport> topFiles=new ListView<>();topFiles.setPrefHeight(220);topFiles.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(FileReport f,boolean empty){super.updateItem(f,empty);setText(empty||f==null?null:String.format("%.2f  %s",f.getAverageComplexity(),Path.of(f.getFilePath()).getFileName()));}});
        VBox left=new VBox(14,new Label("Severity distribution"),pie,new Label("Most complex files"),topFiles);left.setPrefWidth(340);left.setPadding(new Insets(0,16,0,0));
        TextField search=new TextField();search.setPromptText("Search issues...");ComboBox<String> severity=new ComboBox<>(FXCollections.observableArrayList("All severities","HIGH","MEDIUM","LOW"));severity.getSelectionModel().selectFirst();
        FilteredList<CodeIssue> filtered=new FilteredList<>(issues,x->true);search.textProperty().addListener((o,a,b)->filter(filtered,search.getText(),severity.getValue()));severity.valueProperty().addListener((o,a,b)->filter(filtered,search.getText(),b));table.setItems(filtered);table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        addColumn("Severity",90,i->i.getSeverity().name());addColumn("Rule",145,CodeIssue::getRuleName);addColumn("File",180,CodeIssue::getFileName);addColumn("Line",60,i->String.valueOf(i.getLineNumber()));addColumn("Finding",260,CodeIssue::getMessage);addColumn("Suggestion",260,CodeIssue::getSuggestion);
        HBox filters=new HBox(10,search,severity);VBox right=new VBox(10,new Label("Code issues"),filters,table);VBox.setVgrow(table,Priority.ALWAYS);right.setPadding(new Insets(0,0,0,12));
        SplitPane lower=new SplitPane(left,right);lower.setDividerPositions(.28);VBox content=new VBox(16,cards,lower);VBox.setVgrow(lower,Priority.ALWAYS);setCenter(content);
        getStylesheets().add("data:text/css,"+java.net.URLEncoder.encode(".label{-fx-text-fill:#203047;-fx-font-family:'Segoe UI';}.button{-fx-background-color:#2864dc;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:10 14;}.table-view,.list-view,.chart{-fx-background-radius:10;}",java.nio.charset.StandardCharsets.UTF_8));
    }
    private void addColumn(String title,int width,java.util.function.Function<CodeIssue,String> f){TableColumn<CodeIssue,String> c=new TableColumn<>(title);c.setPrefWidth(width);c.setCellValueFactory(x->new javafx.beans.property.SimpleStringProperty(f.apply(x.getValue())));table.getColumns().add(c);}
    private void filter(FilteredList<CodeIssue> f,String query,String sev){String q=query==null?"":query.toLowerCase(Locale.ROOT);f.setPredicate(i->(sev==null||sev.startsWith("All")||i.getSeverity().name().equals(sev))&&(q.isBlank()||(i.getMessage()+i.getSuggestion()+i.getFileName()+i.getRuleName()).toLowerCase(Locale.ROOT).contains(q)));}
    private Button button(String s){return new Button(s);} private VBox card(String title,Label value){value.setStyle("-fx-font-size:25px;-fx-font-weight:bold;");Label t=new Label(title);t.setStyle("-fx-font-size:11px;-fx-text-fill:#718096;");VBox box=new VBox(8,t,value);box.setPadding(new Insets(16));box.setStyle("-fx-background-color:white;-fx-background-radius:12;");HBox.setHgrow(box,Priority.ALWAYS);return box;}
    private void choose(){DirectoryChooser d=new DirectoryChooser();d.setTitle("Select a Java project");java.io.File f=d.showDialog(stage);if(f!=null)scan(f.toPath());}
    private void scan(Path path){status.setText("Scanning…");Task<ProjectReport> task=new Task<>(){@Override protected ProjectReport call()throws Exception{return analyzer.analyze(path);}};task.setOnSucceeded(e->{report=task.getValue();issues.setAll(report.getAllIssues());health.setText(report.getHealthScore()+" / 100 · "+report.getGrade());files.setText(String.valueOf(report.getTotalFiles()));complexity.setText(String.format("%.2f",report.getAverageComplexity()));issueCount.setText(String.valueOf(issues.size()));status.setText("Scanned "+report.getProjectPath());refreshCharts(report);});task.setOnFailed(e->{status.setText("Scan failed: "+task.getException().getMessage());new Alert(Alert.AlertType.ERROR,"Could not scan folder: "+task.getException().getMessage()).show();});Thread t=new Thread(task,"code-review-scan");t.setDaemon(true);t.start();}
    private void refreshCharts(ProjectReport r){PieChart pie=findPie();Map<CodeIssue.Severity,Long> counts=new EnumMap<>(CodeIssue.Severity.class);for(CodeIssue.Severity s:CodeIssue.Severity.values())counts.put(s,r.getAllIssues().stream().filter(i->i.getSeverity()==s).count());pie.getData().setAll(counts.entrySet().stream().filter(e->e.getValue()>0).map(e->new PieChart.Data(e.getKey()+" ("+e.getValue()+")",e.getValue())).toList());ListView<FileReport> lv=findList();lv.getItems().setAll(r.getFileReports().stream().sorted(Comparator.comparingDouble(FileReport::getAverageComplexity).reversed()).limit(10).toList());}
    private PieChart findPie(){return (PieChart)((VBox)((SplitPane)((VBox)getCenter()).getChildren().get(1)).getItems().get(0)).getChildren().get(1);} private ListView<FileReport> findList(){return (ListView<FileReport>)((VBox)((SplitPane)((VBox)getCenter()).getChildren().get(1)).getItems().get(0)).getChildren().get(3);}
    private void export(){if(report==null){new Alert(Alert.AlertType.INFORMATION,"Run a scan before exporting a report.").show();return;}FileChooser fc=new FileChooser();fc.setTitle("Export PDF report");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files","*.pdf"));fc.setInitialFileName("code-review-report.pdf");java.io.File f=fc.showSaveDialog(stage);if(f!=null)try{new PdfReportExporter().export(report,f.toPath());status.setText("Report saved: "+f.getAbsolutePath());}catch(Exception ex){new Alert(Alert.AlertType.ERROR,"PDF export failed: "+ex.getMessage()).show();}}
}
