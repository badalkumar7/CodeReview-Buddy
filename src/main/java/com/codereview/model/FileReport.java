package com.codereview.model;
import java.util.List;
/** Stores source metrics and detected issues for one Java file. */
public class FileReport {
    private final String filePath; private final int totalLines, methodCount; private final double averageComplexity; private final List<CodeIssue> issues;
    public FileReport(String filePath,int totalLines,int methodCount,double averageComplexity,List<CodeIssue> issues){this.filePath=filePath;this.totalLines=totalLines;this.methodCount=methodCount;this.averageComplexity=averageComplexity;this.issues=List.copyOf(issues);}
    /** Returns the path of the analyzed file. */
    public String getFilePath(){return filePath;}
    /** Returns the number of source lines. */
    public int getTotalLines(){return totalLines;}
    /** Returns the number of methods and constructors found. */
    public int getMethodCount(){return methodCount;}
    /** Returns the average cyclomatic complexity. */
    public double getAverageComplexity(){return averageComplexity;}
    /** Returns an immutable list of issues for this file. */
    public List<CodeIssue> getIssues(){return issues;}
}
