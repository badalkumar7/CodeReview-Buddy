package com.codereview.analyzer;
import com.codereview.model.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Recursively parses and analyzes Java source files under a project directory. */
public class AstAnalyzerService {
    private final RuleEngine rules=new RuleEngine();
    /** Scans all Java files below {@code root} and returns their combined report. */
    public ProjectReport analyze(Path root) throws IOException {
        if(!Files.isDirectory(root))throw new IllegalArgumentException("Project path is not a directory: "+root);
        List<FileReport> reports=new ArrayList<>(); JavaParser parser=new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        try(Stream<Path> paths=Files.walk(root)){for(Path file:paths.filter(Files::isRegularFile).filter(p->p.toString().endsWith(".java")).toList()){
            try {ParseResult<CompilationUnit> result=parser.parse(file); if(result.getResult().isEmpty()||!result.getProblems().isEmpty()){reports.add(new FileReport(file.toString(),lineCount(file),0,0,List.of(new CodeIssue(file.toString(),1,"Parse Error",CodeIssue.Severity.LOW,result.getProblems().toString(),"Fix syntax errors so the file can be analyzed."))));continue;}
                CompilationUnit cu=result.getResult().get();List<CodeIssue> issues=rules.analyze(cu,file.toString());
                int methods=cu.findAll(CallableDeclaration.class).size();double complexity=cu.findAll(CallableDeclaration.class).stream().mapToInt(ComplexityVisitor::calculate).average().orElse(0);reports.add(new FileReport(file.toString(),lineCount(file),methods,complexity,issues));
            } catch(Exception ex){reports.add(new FileReport(file.toString(),lineCount(file),0,0,List.of(new CodeIssue(file.toString(),1,"Parse Error",CodeIssue.Severity.LOW,Objects.toString(ex.getMessage(),"Could not parse file"),"Review file encoding and syntax."))));}
        }}
        int lines=reports.stream().mapToInt(FileReport::getTotalLines).sum();int methods=reports.stream().mapToInt(FileReport::getMethodCount).sum();double complexity=reports.stream().mapToDouble(f->f.getAverageComplexity()*f.getMethodCount()).sum()/(methods==0?1:methods);List<CodeIssue> issues=reports.stream().flatMap(f->f.getIssues().stream()).toList();int score=rules.healthScore(issues,reports.size());
        return new ProjectReport(root.toAbsolutePath().normalize().toString(),reports.size(),lines,complexity,score,rules.grade(score),reports);
    }
    private int lineCount(Path p){try(Stream<String> s=Files.lines(p)){return (int)s.count();}catch(IOException e){return 0;}}
}
