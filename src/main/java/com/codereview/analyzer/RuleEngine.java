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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.stmt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies code quality rules to a parsed Java compilation unit. */
public class RuleEngine {
    /** Returns all detected issues for one parsed source file. */
    public List<CodeIssue> analyze(CompilationUnit cu, String fileName) {
        List<CodeIssue> out = new ArrayList<>();
        cu.findAll(CallableDeclaration.class).forEach(raw -> {
            CallableDeclaration<?> callable = (CallableDeclaration<?>) raw;
            int line = callable.getBegin().map(p -> p.line).orElse(1);
            int complexity = ComplexityVisitor.calculate(callable);
            if (complexity > 10) {
                add(out, fileName, line, "Cyclomatic Complexity", Severity.HIGH,
                        "Cyclomatic complexity is " + complexity + " (limit 10).",
                        "Split the method into smaller, focused methods.");
            }
            if (callable instanceof MethodDeclaration method && method.getBody().isPresent()) {
                BlockStmt body = method.getBody().get();
                int lines = body.getEnd().map(p -> p.line).orElse(line) - line + 1;
                if (lines > 30) {
                    add(out, fileName, line, "Long Method", Severity.MEDIUM,
                            "Method spans " + lines + " lines (limit 30).",
                            "Extract cohesive sections into helper methods.");
                }
                int depth = maxNesting(body, 0);
                if (depth > 3) {
                    add(out, fileName, line, "Nesting Depth", Severity.HIGH,
                            "Maximum nesting depth is " + depth + " (limit 3).",
                            "Use guard clauses or extract nested logic.");
                }
            }
            if (callable instanceof MethodDeclaration method && method.isPublic()
                    && method.getJavadocComment().isEmpty()) {
                add(out, fileName, line, "Missing Javadoc", Severity.LOW,
                        "Public method has no Javadoc.",
                        "Document the method purpose, parameters, and return value.");
            }
        });

        cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(ClassOrInterfaceDeclaration::isPublic)
                .filter(type -> type.getJavadocComment().isEmpty())
                .forEach(type -> add(out, fileName, type.getBegin().map(p -> p.line).orElse(1),
                        "Missing Javadoc", Severity.LOW, "Public class has no Javadoc.",
                        "Add a brief class-level Javadoc comment."));

        Set<String> referencedNames = collectReferencedNames(cu);
        cu.getImports().forEach(importDeclaration -> {
            if (importDeclaration.isAsterisk()) {
                return;
            }
            String fullName = importDeclaration.getNameAsString();
            String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
            if (!referencedNames.contains(simpleName)) {
                add(out, fileName, importDeclaration.getBegin().map(p -> p.line).orElse(1),
                        "Unused Import", Severity.LOW,
                        "Import appears to be unused: " + fullName,
                        "Remove the import or use the imported type/member.");
            }
        });
        return out;
    }

    private Set<String> collectReferencedNames(CompilationUnit cu) {
        Set<String> names = new HashSet<>();
        cu.findAll(ClassOrInterfaceType.class).forEach(type -> names.add(type.getNameAsString()));
        cu.findAll(NameExpr.class).forEach(expression -> names.add(expression.getNameAsString()));
        cu.findAll(MethodCallExpr.class).forEach(call -> names.add(call.getNameAsString()));
        cu.findAll(FieldAccessExpr.class).forEach(field -> names.add(field.getNameAsString()));
        cu.findAll(CallableDeclaration.class).forEach(raw -> {
            CallableDeclaration<?> callable = (CallableDeclaration<?>) raw;
            callable.getThrownExceptions().forEach(type -> {
                String text = type.asString();
                names.add(text.substring(text.lastIndexOf('.') + 1));
            });
        });
        return names;
    }

    private int maxNesting(Node node, int depth) {
        int current = depth;
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof ForEachStmt
                || node instanceof WhileStmt || node instanceof DoStmt || node instanceof SwitchStmt
                || node instanceof TryStmt || node instanceof CatchClause) {
            current++;
        }
        int maximum = current;
        for (Node child : node.getChildNodes()) {
            maximum = Math.max(maximum, maxNesting(child, current));
        }
        return maximum;
    }

    private static void add(List<CodeIssue> out, String fileName, int line, String rule,
                            Severity severity, String message, String suggestion) {
        out.add(new CodeIssue(fileName, line, rule, severity, message, suggestion));
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
        Map<String, List<CodeIssue>> byFile = new HashMap<>();
        for (CodeIssue issue : issues) {
            byFile.computeIfAbsent(issue.getFileName(), ignored -> new ArrayList<>()).add(issue);
        }
        double penalties = 0;
        for (List<CodeIssue> fileIssues : byFile.values()) {
            long high = fileIssues.stream().filter(i -> i.getSeverity() == Severity.HIGH).count();
            long medium = fileIssues.stream().filter(i -> i.getSeverity() == Severity.MEDIUM).count();
            long javadocs = fileIssues.stream().filter(i -> i.getSeverity() == Severity.LOW
                    && i.getRuleName().equals("Missing Javadoc")).count();
            long otherLow = fileIssues.stream().filter(i -> i.getSeverity() == Severity.LOW
                    && !i.getRuleName().equals("Missing Javadoc")).count();
            penalties += high * 8.0 + medium * 3.0 + otherLow * 0.5 + Math.min(15.0, javadocs * 0.5);
        }
        return Math.max(0, Math.min(100, (int) Math.floor(100.0 - penalties / totalFiles)));
    }

    /** Returns the letter grade for a numeric health score. */
    public String grade(int score) {
        return score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : score >= 60 ? "D" : "F";
    }
}
