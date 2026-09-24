package br.com.samuellima.qa.report.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record TestExecutionRecord(
    String suite,
    String className,
    String parentClassName,
    String displayName,
    String methodName,
    ExecutionStatus status,
    Instant startedAt,
    Duration duration,
    String errorMessage,
    String stackTraceExcerpt,
    Map<String, String> extras
) {

  public TestExecutionRecord {
    suite = Objects.requireNonNullElse(suite, "unknown");
    className = Objects.requireNonNullElse(className, "?");
    displayName = Objects.requireNonNullElse(displayName, methodName);
    status = Objects.requireNonNull(status, "status é obrigatório");
    startedAt = Objects.requireNonNullElseGet(startedAt, Instant::now);
    duration = Objects.requireNonNullElse(duration, Duration.ZERO);
    extras = extras == null || extras.isEmpty()
        ? Map.of()
        : Collections.unmodifiableMap(new LinkedHashMap<>(extras));
  }

  public String context() {
    return parentClassName != null ? parentClassName + " › " + className : className;
  }

  public String formattedDuration() {
    return String.format("%.1fs", duration.toMillis() / 1000.0);
  }

  public boolean hasError() {
    return errorMessage != null && !errorMessage.isBlank();
  }
}
