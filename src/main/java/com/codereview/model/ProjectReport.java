package com.codereview.model;
import java.util.List;
/** Aggregates analysis metrics and findings for an entire project. */
public class ProjectReport {
    private final String projectPath; private final int totalFiles,totalLines,healthScore; private final double averageComplexity; private final String grade; private final List<FileReport> fileReports;
    public ProjectReport(String projectPath,int totalFiles,int totalLines,double averageComplexity,int healthScore,String grade,List<FileReport> fileReports){this.projectPath=projectPath;this.totalFiles=totalFiles;this.totalLines=totalLines;this.averageComplexity=averageComplexity;this.healthScore=Math.max(0,Math.min(100,healthScore));this.grade=grade;this.fileReports=List.copyOf(fileReports);}
    /** Returns the normalized project root path. */
    public String getProjectPath(){return projectPath;}
    /** Returns the number of Java files scanned. */
    public int getTotalFiles(){return totalFiles;}
    /** Returns the total number of source lines. */
    public int getTotalLines(){return totalLines;}
    /** Returns the project-wide average method complexity. */
    public double getAverageComplexity(){return averageComplexity;}
    /** Returns the project health score from zero through one hundred. */
    public int getHealthScore(){return healthScore;}
    /** Returns the letter grade corresponding to the health score. */
    public String getGrade(){return grade;}
    /** Returns reports for each scanned Java file. */
    public List<FileReport> getFileReports(){return fileReports;}
    /** Returns every issue across the project files. */
    public List<CodeIssue> getAllIssues(){return fileReports.stream().flatMap(f->f.getIssues().stream()).toList();}
}
