package com.codereview.analyzer;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
/** Computes method cyclomatic complexity from JavaParser syntax nodes. */
public class ComplexityVisitor extends VoidVisitorAdapter<Void> {
    private int complexity=1;
    /** Calculates complexity for a method or constructor declaration. */
    public static int calculate(CallableDeclaration<?> method){ComplexityVisitor v=new ComplexityVisitor();if(method instanceof MethodDeclaration m)m.getBody().ifPresent(b->b.accept(v,null));else if(method instanceof ConstructorDeclaration c)c.getBody().accept(v,null);return v.complexity;}
    /** Calculates complexity for an arbitrary method body node. */
    public static int calculate(Node body){ComplexityVisitor v=new ComplexityVisitor();body.accept(v,null);return v.complexity;}
    /** Counts an {@code if} branch and visits its nested expressions and statements. */
    @Override public void visit(IfStmt n,Void a){complexity++;super.visit(n,a);}
    /** Counts a {@code for} loop. */
    @Override public void visit(ForStmt n,Void a){complexity++;super.visit(n,a);}
    /** Counts an enhanced {@code for} loop. */
    @Override public void visit(ForEachStmt n,Void a){complexity++;super.visit(n,a);}
    /** Counts a {@code while} loop. */
    @Override public void visit(WhileStmt n,Void a){complexity++;super.visit(n,a);}
    /** Counts a {@code do-while} loop. */
    @Override public void visit(DoStmt n,Void a){complexity++;super.visit(n,a);}
    /** Counts a switch entry. */
    @Override public void visit(SwitchEntry n,Void a){complexity++;super.visit(n,a);}
    /** Counts an exception handler. */
    @Override public void visit(CatchClause n,Void a){complexity++;super.visit(n,a);}
    /** Counts each short-circuit boolean operator. */
    @Override public void visit(BinaryExpr n,Void a){if(n.getOperator()==BinaryExpr.Operator.AND||n.getOperator()==BinaryExpr.Operator.OR)complexity++;super.visit(n,a);}
    /** Counts a ternary conditional expression. */
    @Override public void visit(com.github.javaparser.ast.expr.ConditionalExpr n,Void a){complexity++;super.visit(n,a);}
}
