package com.codereview.report;
import com.codereview.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.*;
import java.nio.file.Path;

/** Writes project analysis reports as styled PDF documents. */
public class PdfReportExporter {
    /** Exports the supplied report to {@code destination}. */
    public void export(ProjectReport report,Path destination) throws IOException {
        try(OutputStream out=java.nio.file.Files.newOutputStream(destination)){
            Document doc=new Document(PageSize.A4,38,38,42,38);PdfWriter.getInstance(doc,out);doc.open();
            Font title=FontFactory.getFont(FontFactory.HELVETICA_BOLD,22,new Color(31,55,90));Font heading=FontFactory.getFont(FontFactory.HELVETICA_BOLD,14,new Color(31,55,90));Font body=FontFactory.getFont(FontFactory.HELVETICA,9,Color.DARK_GRAY);
            Paragraph h=new Paragraph("CodeReview Buddy",title);h.setSpacingAfter(4);doc.add(h);doc.add(new Paragraph("Project: "+report.getProjectPath(),body));doc.add(new Paragraph("Generated: "+java.time.LocalDateTime.now().toString(),body));doc.add(Chunk.NEWLINE);
            PdfPTable kpi=new PdfPTable(4);kpi.setWidthPercentage(100);String[] vals={report.getHealthScore()+" / 100  ("+report.getGrade()+")",String.valueOf(report.getTotalFiles()),String.format("%.2f",report.getAverageComplexity()),String.valueOf(report.getAllIssues().size())};String[] labels={"Health score","Files scanned","Average complexity","Issues"};for(int i=0;i<4;i++){PdfPCell c=new PdfPCell(new Phrase(labels[i]+"\n"+vals[i],body));c.setPadding(9);c.setBackgroundColor(new Color(235,242,250));kpi.addCell(c);}doc.add(kpi);doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("File complexity breakdown",heading));PdfPTable files=new PdfPTable(new float[]{5,1,1,1});files.setWidthPercentage(100);for(String s:new String[]{"File","Lines","Methods","Avg complexity"})header(files,s);for(FileReport f:report.getFileReports()){files.addCell(new Phrase(f.getFilePath(),body));files.addCell(new Phrase(String.valueOf(f.getTotalLines()),body));files.addCell(new Phrase(String.valueOf(f.getMethodCount()),body));files.addCell(new Phrase(String.format("%.2f",f.getAverageComplexity()),body));}doc.add(files);doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Detected issues",heading));if(report.getAllIssues().isEmpty())doc.add(new Paragraph("No issues found.",body));else for(CodeIssue i:report.getAllIssues())doc.add(new Paragraph(i.getSeverity()+" | "+i.getRuleName()+" | "+i.getFileName()+":"+i.getLineNumber()+" — "+i.getMessage()+" Suggestion: "+i.getSuggestion(),body));doc.close();
        }catch(DocumentException e){throw new IOException("Could not create PDF report",e);}
    }
    private void header(PdfPTable t,String text){PdfPCell c=new PdfPCell(new Phrase(text,FontFactory.getFont(FontFactory.HELVETICA_BOLD,9,Color.WHITE)));c.setBackgroundColor(new Color(45,78,120));c.setPadding(6);t.addCell(c);}
}
