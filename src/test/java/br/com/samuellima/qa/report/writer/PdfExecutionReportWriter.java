package br.com.samuellima.qa.report.writer;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import br.com.samuellima.qa.report.ReportPaths;
import br.com.samuellima.qa.report.model.ExecutionStatus;
import br.com.samuellima.qa.report.model.ExecutionSummary;
import br.com.samuellima.qa.report.model.TestExecutionRecord;

public class PdfExecutionReportWriter implements ExecutionReportWriter {

  private static final Color HEADER_BACKGROUND = new DeviceRgb(38, 50, 66);
  private static final Color ROW_BACKGROUND = new DeviceRgb(240, 242, 245);
  private static final Color SUCCESS = new DeviceRgb(22, 128, 72);
  private static final Color FAILURE = new DeviceRgb(178, 34, 34);
  private static final Color WARNING = new DeviceRgb(196, 132, 16);

  private static final float TITLE_SIZE = 16f;
  private static final float SECTION_SIZE = 12f;
  private static final float BODY_SIZE = 8.5f;

  @Override
  public String name() {
    return "PDF";
  }

  @Override
  public void write(List<TestExecutionRecord> records, ExecutionSummary summary)
          throws IOException {
    ReportPaths.ensureDirectories();
    Path file = ReportPaths.pdfFile();

    PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
    PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

    try (PdfDocument pdf = new PdfDocument(new PdfWriter(file.toFile()));
         Document document = new Document(pdf, PageSize.A4.rotate())) {

      document.setMargins(24, 24, 24, 24);
      document.setFont(regular).setFontSize(BODY_SIZE);

      addHeader(document, summary, bold, regular);
      addSummaryTable(document, summary, bold);
      addFailures(document, records, bold);
      addSuiteTables(document, records, bold);
    }

    Files.copy(file, ReportPaths.timestampedPdfFile(summary.finishedAt()),
            StandardCopyOption.REPLACE_EXISTING);

    System.out.println("📄 Relatório Dog API (PDF): " + file.toAbsolutePath());
  }

  private void addHeader(Document document, ExecutionSummary summary, PdfFont bold,
                         PdfFont regular) {
    document.add(new Paragraph("Dog API - Relatorio de Execucao")
            .setFont(bold)
            .setFontSize(TITLE_SIZE)
            .setFontColor(HEADER_BACKGROUND));

    document.add(new Paragraph("Gerado em " + ReportPaths.format(summary.finishedAt()))
            .setFont(regular)
            .setFontSize(BODY_SIZE)
            .setFontColor(ColorConstants.DARK_GRAY)
            .setMarginBottom(12));
  }

  private void addSummaryTable(Document document, ExecutionSummary summary, PdfFont bold) {
    Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1, 1, 1}))
            .useAllAvailableWidth()
            .setMarginBottom(14);

    addHeaderCells(table, bold, "Total", "Passaram", "Falharam", "Abortados", "Ignorados",
            "Sucesso", "Tempo total");

    table.addCell(valueCell(String.valueOf(summary.total()), ColorConstants.BLACK, bold));
    table.addCell(valueCell(String.valueOf(summary.passed()), SUCCESS, bold));
    table.addCell(valueCell(String.valueOf(summary.failed()), FAILURE, bold));
    table.addCell(valueCell(String.valueOf(summary.aborted()), WARNING, bold));
    table.addCell(valueCell(String.valueOf(summary.skipped()), ColorConstants.DARK_GRAY, bold));
    table.addCell(valueCell(summary.formattedSuccessRate(),
            summary.hasFailures() ? FAILURE : SUCCESS, bold));
    table.addCell(valueCell(summary.formattedTotalDuration(), ColorConstants.BLACK, bold));

    document.add(table);
  }

  private void addFailures(Document document, List<TestExecutionRecord> records, PdfFont bold) {
    List<TestExecutionRecord> failures = records.stream()
            .filter(r -> r.status().isFailure())
            .toList();

    if (failures.isEmpty()) {
      document.add(sectionTitle("Todos os testes passaram", bold, SUCCESS));
      return;
    }

    document.add(sectionTitle("Falhas (" + failures.size() + ")", bold, FAILURE));

    Table table = new Table(UnitValue.createPercentArray(new float[]{2, 4, 3, 1, 6}))
            .useAllAvailableWidth()
            .setMarginBottom(14);

    addHeaderCells(table, bold, "Suite", "Teste", "Classe", "Duracao", "Erro");

    boolean stripe = false;
    for (TestExecutionRecord record : failures) {
      Color background = stripe ? ROW_BACKGROUND : ColorConstants.WHITE;
      table.addCell(bodyCell(record.suite(), background));
      table.addCell(bodyCell(record.displayName(), background));
      table.addCell(bodyCell(record.context(), background));
      table.addCell(bodyCell(record.formattedDuration(), background));
      table.addCell(bodyCell(record.hasError() ? record.errorHeadline() : "-", background)
              .setFontColor(FAILURE));
      stripe = !stripe;
    }

    document.add(table);
    addErrorDetails(document, failures, bold);
  }

  private void addErrorDetails(Document document, List<TestExecutionRecord> failures,
                               PdfFont bold) {
    List<TestExecutionRecord> detailed = failures.stream()
            .filter(TestExecutionRecord::hasMultilineError)
            .toList();

    if (detailed.isEmpty()) {
      return;
    }

    document.add(sectionTitle("Detalhe das falhas", bold, FAILURE));
    for (TestExecutionRecord record : detailed) {
      document.add(new Paragraph(sanitize(record.displayName()))
              .setFont(bold)
              .setFontSize(BODY_SIZE)
              .setMarginBottom(2));
      record.errorMessage().lines().forEach(line ->
              document.add(new Paragraph(sanitizeLine(line))
                      .setFontSize(BODY_SIZE)
                      .setFontColor(FAILURE)
                      .setMarginBottom(0)));
      document.add(new Paragraph(" ").setFontSize(4));
    }
  }

  private void addSuiteTables(Document document, List<TestExecutionRecord> records, PdfFont bold) {
    Map<String, List<TestExecutionRecord>> bySuite = new LinkedHashMap<>();
    records.forEach(r -> bySuite.computeIfAbsent(r.suite(), k -> new ArrayList<>()).add(r));

    bySuite.forEach((suite, suiteRecords) -> {
      ExecutionSummary suiteSummary = ExecutionSummary.from(suiteRecords);
      String title = String.format("%s - %d/%d (%s)", suite, suiteSummary.passed(),
              suiteSummary.total(), suiteSummary.formattedSuccessRate());

      document.add(sectionTitle(title, bold,
              suiteSummary.hasFailures() ? FAILURE : HEADER_BACKGROUND));

      Table table = new Table(UnitValue.createPercentArray(new float[]{1.4f, 6, 3.2f, 1, 4.4f}))
              .useAllAvailableWidth()
              .setMarginBottom(14);

      addHeaderCells(table, bold, "Status", "Teste", "Classe", "Duracao", "Erro");

      boolean stripe = false;
      for (TestExecutionRecord record : suiteRecords) {
        Color background = stripe ? ROW_BACKGROUND : ColorConstants.WHITE;
        table.addCell(bodyCell(record.status().label(), background)
                .setFontColor(statusColor(record.status())));
        table.addCell(bodyCell(record.displayName(), background));
        table.addCell(bodyCell(record.context(), background));
        table.addCell(bodyCell(record.formattedDuration(), background));
        table.addCell(bodyCell(record.hasError() ? record.errorHeadline() : "-", background));
        stripe = !stripe;
      }

      document.add(table);
    });
  }

  private Paragraph sectionTitle(String text, PdfFont bold, Color color) {
    return new Paragraph(sanitize(text))
            .setFont(bold)
            .setFontSize(SECTION_SIZE)
            .setFontColor(color)
            .setMarginTop(6)
            .setMarginBottom(6);
  }

  private void addHeaderCells(Table table, PdfFont bold, String... titles) {
    for (String title : titles) {
      table.addHeaderCell(new Cell()
              .add(new Paragraph(sanitize(title)).setFont(bold).setFontSize(BODY_SIZE))
              .setBackgroundColor(HEADER_BACKGROUND)
              .setFontColor(ColorConstants.WHITE)
              .setTextAlignment(TextAlignment.CENTER)
              .setPadding(4));
    }
  }

  private Cell valueCell(String value, Color color, PdfFont bold) {
    return new Cell()
            .add(new Paragraph(sanitize(value)).setFont(bold).setFontSize(11))
            .setFontColor(color)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5);
  }

  private Cell bodyCell(String value, Color background) {
    return new Cell()
            .add(new Paragraph(sanitize(value)).setFontSize(BODY_SIZE))
            .setBackgroundColor(background)
            .setTextAlignment(TextAlignment.LEFT)
            .setPadding(3);
  }

  private Color statusColor(ExecutionStatus status) {
    return switch (status) {
      case PASSED -> SUCCESS;
      case FAILED -> FAILURE;
      case ABORTED -> WARNING;
      case SKIPPED -> ColorConstants.DARK_GRAY;
    };
  }

  private String sanitizeLine(String value) {
    if (value == null || value.isBlank()) {
      return " ";
    }
    StringBuilder sanitized = new StringBuilder(value.length());
    value.codePoints().forEach(codePoint -> {
      if (codePoint <= 0xFF) {
        sanitized.appendCodePoint(codePoint);
      } else {
        sanitized.append(' ');
      }
    });
    String result = sanitized.toString();
    return result.isBlank() ? " " : result;
  }

  private String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return "-";
    }
    String singleLine = value.lines().findFirst().orElse("-");
    StringBuilder sanitized = new StringBuilder(singleLine.length());
    singleLine.codePoints().forEach(codePoint -> {
      if (codePoint <= 0xFF) {
        sanitized.appendCodePoint(codePoint);
      } else {
        sanitized.append(' ');
      }
    });
    String result = sanitized.toString().trim();
    return result.isEmpty() ? "-" : result;
  }
}