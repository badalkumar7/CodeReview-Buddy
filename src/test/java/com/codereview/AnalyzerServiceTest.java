package com.codereview;

import com.codereview.analyzer.AstAnalyzerService;
import com.codereview.analyzer.ComplexityVisitor;
import com.codereview.analyzer.RuleEngine;
import com.codereview.model.CodeIssue;
import com.codereview.model.ProjectReport;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerServiceTest {
    @TempDir
    Path temp;

    private CompilationUnit parse(String source) {
        return new JavaParser().parse(source).getResult().orElseThrow();
    }

    @Test
    void cyclomaticComplexityCountsBranches() {
        CompilationUnit compilationUnit = parse(
                "class X { int f(boolean a, boolean b){ if(a && b) return 1; else return 0; } }");
        MethodDeclaration method = compilationUnit.findAll(MethodDeclaration.class).get(0);

        assertEquals(3, ComplexityVisitor.calculate(method));
    }

    @Test
    void detectsExcessiveNesting() {
        String source = "class X { void f(){ if(true){while(true){for(;;){if(false){} }}}} }";
        var issues = new RuleEngine().analyze(parse(source), "X.java");

        assertTrue(issues.stream().anyMatch(issue ->
                issue.getRuleName().equals("Nesting Depth")
                        && issue.getSeverity() == CodeIssue.Severity.HIGH));
    }

    @Test
    void detectsLongMethods() {
        StringBuilder source = new StringBuilder("class X { void f(){\n");
        for (int i = 0; i < 32; i++) {
            source.append("int x").append(i).append("=0;\n");
        }
        source.append("} }");

        var issues = new RuleEngine().analyze(parse(source.toString()), "X.java");
        assertTrue(issues.stream().anyMatch(issue -> issue.getRuleName().equals("Long Method")));
    }

    @Test
    void detectsMissingPublicJavadocs() {
        var issues = new RuleEngine().analyze(
                parse("public class X { public void f(){} }"), "X.java");

        assertEquals(2, issues.stream()
                .filter(issue -> issue.getRuleName().equals("Missing Javadoc"))
                .count());
    }

    @Test
    void detectsUnusedImports() {
        var issues = new RuleEngine().analyze(
                parse("import java.util.List; class X {}"), "X.java");

        assertTrue(issues.stream().anyMatch(issue -> issue.getRuleName().equals("Unused Import")));
    }

    @Test
    void recursivelyScansDirectoryAndHandlesParseErrors() throws Exception {
        Path nested = Files.createDirectories(temp.resolve("src/nested"));
        Files.writeString(nested.resolve("Good.java"), "class Good { void f(){} }");
        Files.writeString(temp.resolve("Broken.java"), "class Broken { this is invalid");

        ProjectReport report = new AstAnalyzerService().analyze(temp);
        assertEquals(2, report.getTotalFiles());
        assertTrue(report.getAllIssues().stream()
                .anyMatch(issue -> issue.getRuleName().equals("Parse Error")));
        assertTrue(report.getTotalLines() > 0);
    }

    @Test
    void healthScoreUsesPerFilePenaltiesAndCapsJavadocs() {
        RuleEngine engine = new RuleEngine();
        List<CodeIssue> issues = List.of(
                new CodeIssue("A.java", 1, "Missing Javadoc", CodeIssue.Severity.LOW, "", ""),
                new CodeIssue("A.java", 2, "Missing Javadoc", CodeIssue.Severity.LOW, "", ""),
                new CodeIssue("A.java", 3, "Cyclomatic Complexity", CodeIssue.Severity.HIGH, "", ""),
                new CodeIssue("A.java", 4, "Long Method", CodeIssue.Severity.MEDIUM, "", ""),
                new CodeIssue("A.java", 5, "Unused Import", CodeIssue.Severity.LOW, "", ""));

        assertEquals(93, engine.healthScore(issues, 2));
    }

    @Test
    void sourceTreeSelfScanMaintainsGradeA() throws Exception {
        ProjectReport report = new AstAnalyzerService().analyze(Path.of("src/main/java"));

        assertTrue(report.getHealthScore() >= 90,
                "Self-scan issues: " + report.getAllIssues());
        assertEquals("A", report.getGrade());
    }
}
