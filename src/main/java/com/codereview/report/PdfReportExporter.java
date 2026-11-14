package com.codereview.report;

import com.codereview.model.CodeIssue;
import com.codereview.model.FileReport;
import com.codereview.model.ProjectReport;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes project analysis reports as styled PDF documents. */
public class PdfReportExporter {
    /** Exports the supplied report to {@code destination}. */
    public void export(ProjectReport report, Path destination) throws IOException {
        try (OutputStream output = Files.newOutputStream(destination)) {
            Document document = new Document(PageSize.A4, 38, 38, 42, 38);
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 22, new Color(31, 55, 90));
            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 14, new Color(31, 55, 90));
            Font bodyFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 9, Color.DARK_GRAY);

            Paragraph title = new Paragraph("CodeReview Buddy", titleFont);
            title.setSpacingAfter(4);
            document.add(title);
            document.add(new Paragraph("Project: " + report.getProjectPath(), bodyFont));
            document.add(new Paragraph(
                    "Generated: " + java.time.LocalDateTime.now().toString(), bodyFont));
            document.add(Chunk.NEWLINE);

            addSummaryTable(document, report, bodyFont);
            addFileTable(document, report, bodyFont, headingFont);
            addIssues(document, report, bodyFont, headingFont);
            document.close();
        } catch (DocumentException exception) {
            throw new IOException("Could not create PDF report", exception);
        }
    }

    private void addSummaryTable(Document document, ProjectReport report, Font bodyFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        String[] values = {
                report.getHealthScore() + " / 100  (" + report.getGrade() + ")",
                String.valueOf(report.getTotalFiles()),
                String.format("%.2f", report.getAverageComplexity()),
                String.valueOf(report.getAllIssues().size())
        };
        String[] labels = {"Health score", "Files scanned", "Average complexity", "Issues"};

        for (int i = 0; i < values.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(labels[i] + "\n" + values[i], bodyFont));
            cell.setPadding(9);
            cell.setBackgroundColor(new Color(235, 242, 250));
            table.addCell(cell);
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addFileTable(Document document, ProjectReport report, Font bodyFont,
                              Font headingFont) throws DocumentException {
        document.add(new Paragraph("File complexity breakdown", headingFont));

        PdfPTable table = new PdfPTable(new float[]{5, 1, 1, 1});
        table.setWidthPercentage(100);
        for (String heading : new String[]{"File", "Lines", "Methods", "Avg complexity"}) {
            addHeader(table, heading);
        }

        for (FileReport fileReport : report.getFileReports()) {
            table.addCell(new Phrase(fileReport.getFilePath(), bodyFont));
            table.addCell(new Phrase(String.valueOf(fileReport.getTotalLines()), bodyFont));
            table.addCell(new Phrase(String.valueOf(fileReport.getMethodCount()), bodyFont));
            table.addCell(new Phrase(
                    String.format("%.2f", fileReport.getAverageComplexity()), bodyFont));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addIssues(Document document, ProjectReport report, Font bodyFont,
                           Font headingFont) throws DocumentException {
        document.add(new Paragraph("Detected issues", headingFont));

        if (report.getAllIssues().isEmpty()) {
            document.add(new Paragraph("No issues found.", bodyFont));
            return;
        }

        for (CodeIssue issue : report.getAllIssues()) {
            String text = issue.getSeverity() + " | " + issue.getRuleName() + " | "
                    + issue.getFileName() + ":" + issue.getLineNumber() + " — "
                    + issue.getMessage() + " Suggestion: " + issue.getSuggestion();
            document.add(new Paragraph(text, bodyFont));
        }
    }

    private void addHeader(PdfPTable table, String text) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(new Color(45, 78, 120));
        cell.setPadding(6);
        table.addCell(cell);
    }
}
