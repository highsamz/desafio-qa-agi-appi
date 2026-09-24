package br.com.samuellima.qa.report.writer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import br.com.samuellima.qa.report.ReportPaths;
import br.com.samuellima.qa.report.model.ExecutionSummary;
import br.com.samuellima.qa.report.model.TestExecutionRecord;

public class MarkdownExecutionReportWriter implements ExecutionReportWriter {

  @Override
  public String name() {
    return "Markdown";
  }

  @Override
  public void write(List<TestExecutionRecord> records, ExecutionSummary summary)
      throws IOException {
    ReportPaths.ensureDirectories();
    Path file = ReportPaths.markdownFile();
    Files.writeString(file, build(records, summary), StandardCharsets.UTF_8);
    System.out.println("📋 Relatório Dog API (MD): " + file.toAbsolutePath());
  }

  private String build(List<TestExecutionRecord> records, ExecutionSummary summary) {
    StringBuilder md = new StringBuilder();

    appendHeader(md, summary);
    appendSummary(md, summary);
    appendFailures(md, records);
    appendSuites(md, records);

    return md.toString();
  }

  private void appendHeader(StringBuilder md, ExecutionSummary summary) {
    String icon = summary.hasFailures() ? "🔴" : "🟢";
    md.append("# ").append(icon).append(" Relatório Dog API — ")
        .append(ReportPaths.format(summary.finishedAt())).append("\n\n");
    md.append("> Gerado automaticamente ao final da execução dos testes da Dog API.\n\n");
    md.append("---\n\n");
  }

  private void appendSummary(StringBuilder md, ExecutionSummary summary) {
    md.append("## 📊 Resumo\n\n");
    md.append("| Métrica | Valor |\n|---|---|\n");
    md.append("| 🧪 Total | **").append(summary.total()).append("** |\n");
    md.append("| ✅ Passaram | **").append(summary.passed()).append("** |\n");
    md.append("| ❌ Falharam | **").append(summary.failed()).append("** |\n");
    if (summary.aborted() > 0) {
      md.append("| ⚠️ Abortados | **").append(summary.aborted()).append("** |\n");
    }
    if (summary.skipped() > 0) {
      md.append("| ⏭️ Ignorados | **").append(summary.skipped()).append("** |\n");
    }
    md.append("| 🎯 Sucesso | **").append(summary.formattedSuccessRate()).append("** |\n");
    md.append("| ⏱️ Tempo total | **").append(summary.formattedTotalDuration()).append("** |\n");
    md.append("| 🕐 Início | ").append(ReportPaths.format(summary.startedAt())).append(" |\n");
    md.append("| 🕐 Fim | ").append(ReportPaths.format(summary.finishedAt())).append(" |\n\n");
  }

  private void appendFailures(StringBuilder md, List<TestExecutionRecord> records) {
    List<TestExecutionRecord> failures = records.stream()
        .filter(r -> r.status().isFailure())
        .toList();

    if (failures.isEmpty()) {
      md.append("## ✅ Todos os testes passaram!\n\n---\n\n");
      return;
    }

    md.append("## ❌ Falhas (").append(failures.size()).append(")\n\n");
    for (TestExecutionRecord record : failures) {
      md.append("### ").append(record.status().icon()).append(" ").append(record.displayName())
          .append("\n\n");
      md.append("- **Suíte:** `").append(record.suite()).append("`\n");
      md.append("- **Classe:** `").append(record.context()).append("`\n");
      md.append("- **Método:** `").append(record.methodName()).append("`\n");
      md.append("- **Duração:** ").append(record.formattedDuration()).append("\n");
      record.extras().forEach((key, value) ->
          md.append("- **").append(key).append(":** `").append(value).append("`\n"));
      if (record.hasError()) {
        md.append("- **Erro:** `").append(escape(record.errorMessage())).append("`\n");
      }
      if (record.stackTraceExcerpt() != null && !record.stackTraceExcerpt().isBlank()) {
        md.append("\n```\n").append(record.stackTraceExcerpt()).append("\n```\n");
      }
      md.append("\n");
    }
    md.append("---\n\n");
  }

  private void appendSuites(StringBuilder md, List<TestExecutionRecord> records) {
    Map<String, List<TestExecutionRecord>> bySuite = new LinkedHashMap<>();
    records.forEach(r -> bySuite.computeIfAbsent(r.suite(), k -> new java.util.ArrayList<>()).add(r));

    md.append("## 🧾 Execuções por suíte\n\n");
    bySuite.forEach((suite, suiteRecords) -> {
      ExecutionSummary suiteSummary = ExecutionSummary.from(suiteRecords);
      md.append("### ").append(suite).append(" — ").append(suiteSummary.passed()).append("/")
          .append(suiteSummary.total()).append(" (").append(suiteSummary.formattedSuccessRate())
          .append(")\n\n");
      md.append("| Status | Teste | Classe | Duração | Erro |\n|---|---|---|---|---|\n");
      for (TestExecutionRecord record : suiteRecords) {
        md.append("| ").append(record.status().icon()).append(" ")
            .append(record.status().label()).append(" | ")
            .append(escape(record.displayName())).append(" | `")
            .append(record.context()).append("` | ")
            .append(record.formattedDuration()).append(" | ")
            .append(record.hasError() ? escape(record.errorMessage()) : "—").append(" |\n");
      }
      md.append("\n");
    });
  }

  private String escape(String value) {
    if (value == null) {
      return "—";
    }
    return value.replace("`", "'").replace("|", "\\|").lines().findFirst().orElse("—");
  }
}
