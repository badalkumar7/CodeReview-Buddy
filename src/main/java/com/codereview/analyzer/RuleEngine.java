package com.codereview.analyzer;

import com.codereview.model.CodeIssue;
import com.codereview.model.CodeIssue.Severity;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies code quality rules to a parsed Java compilation unit. */
public class RuleEngine {
    /** Returns all detected issues for one parsed source file. */
    public List<CodeIssue> analyze(CompilationUnit compilationUnit, String fileName) {
        List<CodeIssue> issues = new ArrayList<>();

        compilationUnit.findAll(CallableDeclaration.class).forEach(rawCallable -> {
            CallableDeclaration<?> callable = (CallableDeclaration<?>) rawCallable;
            int line = callable.getBegin().map(position -> position.line).orElse(1);
            int complexity = ComplexityVisitor.calculate(callable);

            if (complexity > 10) {
                add(issues, fileName, line, "Cyclomatic Complexity", Severity.HIGH,
                        "Cyclomatic complexity is " + complexity + " (limit 10).",
                        "Split the method into smaller, focused methods.");
            }

            if (callable instanceof MethodDeclaration method && method.getBody().isPresent()) {
                BlockStmt body = method.getBody().get();
                int lines = body.getEnd().map(position -> position.line).orElse(line) - line + 1;
                if (lines > 30) {
                    add(issues, fileName, line, "Long Method", Severity.MEDIUM,
                            "Method spans " + lines + " lines (limit 30).",
                            "Extract cohesive sections into helper methods.");
                }

                int nestingDepth = maxNesting(body, 0);
                if (nestingDepth > 3) {
                    add(issues, fileName, line, "Nesting Depth", Severity.HIGH,
                            "Maximum nesting depth is " + nestingDepth + " (limit 3).",
                            "Use guard clauses or extract nested logic.");
                }
            }

            if (callable instanceof MethodDeclaration method && method.isPublic()
                    && method.getJavadocComment().isEmpty()) {
                add(issues, fileName, line, "Missing Javadoc", Severity.LOW,
                        "Public method has no Javadoc.",
                        "Document the method purpose, parameters, and return value.");
            }
        });

        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(ClassOrInterfaceDeclaration::isPublic)
                .filter(type -> type.getJavadocComment().isEmpty())
                .forEach(type -> add(issues, fileName,
                        type.getBegin().map(position -> position.line).orElse(1),
                        "Missing Javadoc", Severity.LOW, "Public class has no Javadoc.",
                        "Add a brief class-level Javadoc comment."));

        Set<String> referencedNames = collectReferencedNames(compilationUnit);
        compilationUnit.getImports().forEach(importDeclaration -> {
            if (importDeclaration.isAsterisk()) {
                return;
            }

            String fullName = importDeclaration.getNameAsString();
            String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
            if (!referencedNames.contains(simpleName)) {
                add(issues, fileName,
                        importDeclaration.getBegin().map(position -> position.line).orElse(1),
                        "Unused Import", Severity.LOW,
                        "Import appears to be unused: " + fullName,
                        "Remove the import or use the imported type/member.");
            }
        });

        return issues;
    }

    private Set<String> collectReferencedNames(CompilationUnit compilationUnit) {
        Set<String> names = new HashSet<>();
        compilationUnit.findAll(ClassOrInterfaceType.class)
                .forEach(type -> names.add(type.getNameAsString()));
        compilationUnit.findAll(NameExpr.class)
                .forEach(expression -> names.add(expression.getNameAsString()));
        compilationUnit.findAll(MethodCallExpr.class)
                .forEach(call -> names.add(call.getNameAsString()));
        compilationUnit.findAll(FieldAccessExpr.class)
                .forEach(field -> names.add(field.getNameAsString()));
        compilationUnit.findAll(CallableDeclaration.class).forEach(rawCallable -> {
            CallableDeclaration<?> callable = (CallableDeclaration<?>) rawCallable;
            callable.getThrownExceptions().forEach(type -> {
                String exceptionName = type.asString();
                names.add(exceptionName.substring(exceptionName.lastIndexOf('.') + 1));
            });
        });
        return names;
    }

    private int maxNesting(Node node, int depth) {
        int currentDepth = depth;
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof ForEachStmt
                || node instanceof WhileStmt || node instanceof DoStmt || node instanceof SwitchStmt
                || node instanceof TryStmt || node instanceof CatchClause) {
            currentDepth++;
        }

        int maximumDepth = currentDepth;
        for (Node child : node.getChildNodes()) {
            maximumDepth = Math.max(maximumDepth, maxNesting(child, currentDepth));
        }
        return maximumDepth;
    }

    private static void add(List<CodeIssue> issues, String fileName, int line, String rule,
                            Severity severity, String message, String suggestion) {
        issues.add(new CodeIssue(fileName, line, rule, severity, message, suggestion));
    }

    /** Calculates a project score by scoring each file independently, then averaging. */
    public int healthScore(List<CodeIssue> issues) {
        int fileCount = (int) issues.stream().map(CodeIssue::getFileName).distinct().count();
        return healthScore(issues, fileCount);
    }

    /** Calculates the average file health, counting clean files as 100. */
    public int healthScore(List<CodeIssue> issues, int totalFiles) {
        if (totalFiles <= 0) {
            return 100;
        }

        Map<String, List<CodeIssue>> issuesByFile = new HashMap<>();
        for (CodeIssue issue : issues) {
            issuesByFile.computeIfAbsent(issue.getFileName(), ignored -> new ArrayList<>())
                    .add(issue);
        }

        double penalties = 0;
        for (List<CodeIssue> fileIssues : issuesByFile.values()) {
            long highSeverity = fileIssues.stream()
                    .filter(issue -> issue.getSeverity() == Severity.HIGH)
                    .count();
            long mediumSeverity = fileIssues.stream()
                    .filter(issue -> issue.getSeverity() == Severity.MEDIUM)
                    .count();
            long javadocIssues = fileIssues.stream()
                    .filter(issue -> issue.getSeverity() == Severity.LOW
                            && issue.getRuleName().equals("Missing Javadoc"))
                    .count();
            long otherLowSeverity = fileIssues.stream()
                    .filter(issue -> issue.getSeverity() == Severity.LOW
                            && !issue.getRuleName().equals("Missing Javadoc"))
                    .count();

            penalties += highSeverity * 8.0 + mediumSeverity * 3.0 + otherLowSeverity * 0.5
                    + Math.min(15.0, javadocIssues * 0.5);
        }

        return Math.max(0, Math.min(100,
                (int) Math.floor(100.0 - penalties / totalFiles)));
    }

    /** Returns the letter grade for a numeric health score. */
    public String grade(int score) {
        return score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C"
                : score >= 60 ? "D" : "F";
    }
}
