package br.com.samuellima.qa.report.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record ExecutionSummary(
    long total,
    long passed,
    long failed,
    long aborted,
    long skipped,
    Duration totalDuration,
    Instant startedAt,
    Instant finishedAt
) {

  public static ExecutionSummary from(List<TestExecutionRecord> records) {
    if (records == null || records.isEmpty()) {
      Instant now = Instant.now();
      return new ExecutionSummary(0, 0, 0, 0, 0, Duration.ZERO, now, now);
    }

    long passed = count(records, ExecutionStatus.PASSED);
    long failed = count(records, ExecutionStatus.FAILED);
    long aborted = count(records, ExecutionStatus.ABORTED);
    long skipped = count(records, ExecutionStatus.SKIPPED);

    Duration total = records.stream()
        .map(TestExecutionRecord::duration)
        .reduce(Duration.ZERO, Duration::plus);

    Instant startedAt = records.stream()
        .map(TestExecutionRecord::startedAt)
        .min(Instant::compareTo)
        .orElseGet(Instant::now);

    Instant finishedAt = records.stream()
        .map(r -> r.startedAt().plus(r.duration()))
        .max(Instant::compareTo)
        .orElseGet(Instant::now);

    return new ExecutionSummary(records.size(), passed, failed, aborted, skipped, total, startedAt,
        finishedAt);
  }

  private static long count(List<TestExecutionRecord> records, ExecutionStatus status) {
    return records.stream().filter(r -> r.status() == status).count();
  }

  public double successRate() {
    return total > 0 ? (passed * 100.0 / total) : 0.0;
  }

  public String formattedSuccessRate() {
    return String.format("%.1f%%", successRate());
  }

  public String formattedTotalDuration() {
    long seconds = totalDuration.toSeconds();
    return seconds >= 60
        ? String.format("%dm %02ds", seconds / 60, seconds % 60)
        : String.format("%.1fs", totalDuration.toMillis() / 1000.0);
  }

  public boolean hasFailures() {
    return failed > 0 || aborted > 0;
  }
}
