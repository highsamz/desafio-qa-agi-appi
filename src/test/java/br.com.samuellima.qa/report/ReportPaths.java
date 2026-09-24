package br.com.samuellima.qa.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ReportPaths {

  private static final Path PROJECT_ROOT = resolveProjectRoot();

  public static final Path REPORT_DIR = PROJECT_ROOT.resolve(Paths.get("target",
      "dog-api-reports"));

  private static final String MARKDOWN_FILE = "LAST_EXECUTION_REPORT.md";
  private static final String PDF_FILE = "LAST_EXECUTION_REPORT.pdf";
  private static final String TIMESTAMPED_PDF_PREFIX = "DOG_API_EXECUTION_REPORT_";

  private static final DateTimeFormatter FILE_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

  public static final DateTimeFormatter DISPLAY_TIMESTAMP =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

  private ReportPaths() {
  }

  public static Path markdownFile() {
    return REPORT_DIR.resolve(MARKDOWN_FILE);
  }

  public static Path pdfFile() {
    return REPORT_DIR.resolve(PDF_FILE);
  }

  public static Path timestampedPdfFile(Instant instant) {
    return REPORT_DIR.resolve(TIMESTAMPED_PDF_PREFIX + FILE_TIMESTAMP.format(instant) + ".pdf");
  }

  public static void ensureDirectories() throws IOException {
    Files.createDirectories(REPORT_DIR);
  }

  public static String format(Instant instant) {
    return DISPLAY_TIMESTAMP.format(instant);
  }

  private static Path resolveProjectRoot() {
    Path current = Paths.get("").toAbsolutePath();
    while (current != null) {
      if (Files.exists(current.resolve("pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    return Paths.get("").toAbsolutePath();
  }
}
