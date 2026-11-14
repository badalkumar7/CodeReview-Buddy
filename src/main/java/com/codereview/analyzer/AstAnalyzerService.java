package com.codereview.analyzer;

import com.codereview.model.CodeIssue;
import com.codereview.model.FileReport;
import com.codereview.model.ProjectReport;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Recursively parses and analyzes Java source files under a project directory. */
public class AstAnalyzerService {
    private final RuleEngine rules = new RuleEngine();

    /** Scans all Java files below {@code root} and returns their combined report. */
    public ProjectReport analyze(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Project path is not a directory: " + root);
        }

        List<FileReport> reports = new ArrayList<>();
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        JavaParser parser = new JavaParser(configuration);

        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                try {
                    ParseResult<CompilationUnit> result = parser.parse(file);
                    if (result.getResult().isEmpty() || !result.getProblems().isEmpty()) {
                        CodeIssue issue = new CodeIssue(
                                file.toString(),
                                1,
                                "Parse Error",
                                CodeIssue.Severity.LOW,
                                result.getProblems().toString(),
                                "Fix syntax errors so the file can be analyzed.");
                        reports.add(new FileReport(
                                file.toString(), lineCount(file), 0, 0, List.of(issue)));
                        continue;
                    }

                    CompilationUnit compilationUnit = result.getResult().get();
                    List<CodeIssue> issues = rules.analyze(compilationUnit, file.toString());
                    int methodCount = compilationUnit.findAll(CallableDeclaration.class).size();
                    double averageComplexity = compilationUnit
                            .findAll(CallableDeclaration.class)
                            .stream()
                            .mapToInt(ComplexityVisitor::calculate)
                            .average()
                            .orElse(0);

                    reports.add(new FileReport(
                            file.toString(),
                            lineCount(file),
                            methodCount,
                            averageComplexity,
                            issues));
                } catch (Exception exception) {
                    CodeIssue issue = new CodeIssue(
                            file.toString(),
                            1,
                            "Parse Error",
                            CodeIssue.Severity.LOW,
                            Objects.toString(exception.getMessage(), "Could not parse file"),
                            "Review file encoding and syntax.");
                    reports.add(new FileReport(
                            file.toString(), lineCount(file), 0, 0, List.of(issue)));
                }
            }
        }

        int totalLines = reports.stream().mapToInt(FileReport::getTotalLines).sum();
        int totalMethods = reports.stream().mapToInt(FileReport::getMethodCount).sum();
        double weightedComplexity = reports.stream()
                .mapToDouble(report -> report.getAverageComplexity() * report.getMethodCount())
                .sum() / (totalMethods == 0 ? 1 : totalMethods);
        List<CodeIssue> issues = reports.stream()
                .flatMap(report -> report.getIssues().stream())
                .toList();
        int healthScore = rules.healthScore(issues, reports.size());

        return new ProjectReport(
                root.toAbsolutePath().normalize().toString(),
                reports.size(),
                totalLines,
                weightedComplexity,
                healthScore,
                rules.grade(healthScore),
                reports);
    }

    private int lineCount(Path path) {
        try (Stream<String> lines = Files.lines(path)) {
            return (int) lines.count();
        } catch (IOException exception) {
            return 0;
        }
    }
}
