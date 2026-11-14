package com.codereview.analyzer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/** Computes method cyclomatic complexity from JavaParser syntax nodes. */
public class ComplexityVisitor extends VoidVisitorAdapter<Void> {
    private int complexity = 1;

    /** Calculates complexity for a method or constructor declaration. */
    public static int calculate(CallableDeclaration<?> callable) {
        ComplexityVisitor visitor = new ComplexityVisitor();
        if (callable instanceof MethodDeclaration method) {
            method.getBody().ifPresent(body -> body.accept(visitor, null));
        } else if (callable instanceof ConstructorDeclaration constructor) {
            constructor.getBody().accept(visitor, null);
        }
        return visitor.complexity;
    }

    /** Calculates complexity for an arbitrary method body node. */
    public static int calculate(Node body) {
        ComplexityVisitor visitor = new ComplexityVisitor();
        body.accept(visitor, null);
        return visitor.complexity;
    }

    @Override
    public void visit(IfStmt statement, Void argument) {
        complexity++;
        super.visit(statement, argument);
    }

    @Override
    public void visit(ForStmt statement, Void argument) {
        complexity++;
        super.visit(statement, argument);
    }

    @Override
    public void visit(ForEachStmt statement, Void argument) {
        complexity++;
        super.visit(statement, argument);
    }

    @Override
    public void visit(WhileStmt statement, Void argument) {
        complexity++;
        super.visit(statement, argument);
    }

    @Override
    public void visit(DoStmt statement, Void argument) {
        complexity++;
        super.visit(statement, argument);
    }

    @Override
    public void visit(SwitchEntry entry, Void argument) {
        complexity++;
        super.visit(entry, argument);
    }

    @Override
    public void visit(CatchClause clause, Void argument) {
        complexity++;
        super.visit(clause, argument);
    }

    @Override
    public void visit(BinaryExpr expression, Void argument) {
        if (expression.getOperator() == BinaryExpr.Operator.AND
                || expression.getOperator() == BinaryExpr.Operator.OR) {
            complexity++;
        }
        super.visit(expression, argument);
    }

    @Override
    public void visit(ConditionalExpr expression, Void argument) {
        complexity++;
        super.visit(expression, argument);
    }
}
