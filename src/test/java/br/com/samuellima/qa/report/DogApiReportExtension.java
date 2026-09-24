package br.com.samuellima.qa.report;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.TestWatcher;
import br.com.samuellima.qa.report.model.ExecutionStatus;
import br.com.samuellima.qa.report.model.TestExecutionRecord;

public class DogApiReportExtension implements BeforeTestExecutionCallback, TestWatcher {

  private static final Namespace NAMESPACE =
      Namespace.create(DogApiReportExtension.class);
  private static final String PUBLISHER_KEY = "dogapi-report-publisher";

  private static final Map<String, Instant> START_TIMES = new ConcurrentHashMap<>();
  private static final ExecutionReportPublisher PUBLISHER = new ExecutionReportPublisher();

  static {
    Runtime.getRuntime().addShutdownHook(
        new Thread(PUBLISHER::publishOnce, "dogapi-report-shutdown"));
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    registerPublisher(context);
    START_TIMES.put(context.getUniqueId(), Instant.now());
  }

  @Override
  public void testSuccessful(ExtensionContext context) {
    record(context, ExecutionStatus.PASSED, null);
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    record(context, ExecutionStatus.FAILED, cause);
  }

  @Override
  public void testAborted(ExtensionContext context, Throwable cause) {
    record(context, ExecutionStatus.ABORTED, cause);
  }

  @Override
  public void testDisabled(ExtensionContext context, Optional<String> reason) {
    record(context, ExecutionStatus.SKIPPED, null);
  }

  private void registerPublisher(ExtensionContext context) {
    context.getRoot()
        .getStore(NAMESPACE)
        .getOrComputeIfAbsent(PUBLISHER_KEY,
            key -> (CloseableResource) PUBLISHER::publishOnce,
            CloseableResource.class);
  }

  private void record(ExtensionContext context, ExecutionStatus status, Throwable cause) {
    Instant startedAt = START_TIMES.remove(context.getUniqueId());
    Duration duration = startedAt != null
        ? Duration.between(startedAt, Instant.now())
        : Duration.ZERO;

    Class<?> testClass = context.getTestClass().orElse(null);

    ExecutionRegistry.add(new TestExecutionRecord(
        resolveSuite(testClass),
        testClass != null ? testClass.getSimpleName() : "?",
        resolveParentClass(context),
        context.getDisplayName(),
        context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("?"),
        status,
        startedAt,
        duration,
        firstLineOf(cause),
        shortStackTrace(cause),
        ExecutionRegistry.drainExtras()));
  }

  /** Suíte = classe de teste top-level (agrupa @Nested sob a classe externa). */
  private String resolveSuite(Class<?> testClass) {
    if (testClass == null) {
      return "dog-api";
    }
    Class<?> outer = testClass;
    while (outer.getEnclosingClass() != null) {
      outer = outer.getEnclosingClass();
    }
    return outer.getSimpleName();
  }

  private String resolveParentClass(ExtensionContext context) {
    return context.getParent()
        .flatMap(ExtensionContext::getTestClass)
        .map(Class::getSimpleName)
        .filter(name -> !name.equals(context.getTestClass().map(Class::getSimpleName).orElse("")))
        .orElse(null);
  }

  private String firstLineOf(Throwable cause) {
    if (cause == null) {
      return null;
    }
    return cause.getMessage() != null
        ? cause.getMessage().lines().findFirst().orElse(cause.getClass().getSimpleName())
        : cause.getClass().getSimpleName();
  }

  private String shortStackTrace(Throwable cause) {
    if (cause == null) {
      return null;
    }
    StringBuilder stack = new StringBuilder(cause.getClass().getName());
    if (cause.getMessage() != null) {
      stack.append(": ").append(cause.getMessage().lines().findFirst().orElse(""));
    }
    stack.append("\n");

    int printed = 0;
    for (StackTraceElement element : cause.getStackTrace()) {
      if (element.getClassName().startsWith("br.com.samuellima.qa")) {
        stack.append("  at ").append(element).append("\n");
        if (++printed >= 5) {
          break;
        }
      }
    }
    return stack.toString().trim();
  }
}
