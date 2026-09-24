package br.com.samuellima.qa.report;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import br.com.samuellima.qa.report.model.ExecutionSummary;
import br.com.samuellima.qa.report.model.TestExecutionRecord;
import br.com.samuellima.qa.report.writer.ExecutionReportWriter;
import br.com.samuellima.qa.report.writer.MarkdownExecutionReportWriter;
import br.com.samuellima.qa.report.writer.PdfExecutionReportWriter;

public final class ExecutionReportPublisher {

  private static final AtomicBoolean PUBLISHED = new AtomicBoolean(false);
  private static final long TIMEOUT_SECONDS = 60;

  private final List<ExecutionReportWriter> writers;

  public ExecutionReportPublisher() {
    this(List.of(new MarkdownExecutionReportWriter(), new PdfExecutionReportWriter()));
  }

  public ExecutionReportPublisher(List<ExecutionReportWriter> writers) {
    this.writers = List.copyOf(writers);
  }

  public void publishOnce() {
    if (PUBLISHED.compareAndSet(false, true)) {
      publish();
    }
  }

  public void publish() {
    List<TestExecutionRecord> records = ExecutionRegistry.snapshot();
    if (records.isEmpty()) {
      return;
    }

    ExecutionSummary summary = ExecutionSummary.from(records);
    ExecutorService executor = Executors.newFixedThreadPool(writers.size(), threadFactory());

    try {
      CompletableFuture<?>[] tasks = writers.stream()
          .map(writer -> CompletableFuture.runAsync(() -> runSafely(writer, records, summary),
              executor))
          .toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(tasks).join();
    } catch (Exception e) {
      System.err.println("⚠️  Falha ao publicar os relatórios Dog API: " + e.getMessage());
    } finally {
      shutdown(executor);
    }
  }

  private void runSafely(ExecutionReportWriter writer, List<TestExecutionRecord> records,
      ExecutionSummary summary) {
    try {
      writer.write(records, summary);
    } catch (Exception e) {
      System.err.println("⚠️  Não foi possível gerar o relatório " + writer.name() + ": "
          + e.getMessage());
    }
  }

  private void shutdown(ExecutorService executor) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private ThreadFactory threadFactory() {
    return runnable -> {
      Thread thread = new Thread(runnable, "dogapi-report-writer");
      thread.setDaemon(false);
      return thread;
    };
  }
}
