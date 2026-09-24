package br.com.samuellima.qa.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import br.com.samuellima.qa.report.model.TestExecutionRecord;

public final class ExecutionRegistry {

  private static final List<TestExecutionRecord> RECORDS =
      Collections.synchronizedList(new ArrayList<>());

  private static final ThreadLocal<Map<String, String>> EXTRAS =
      ThreadLocal.withInitial(LinkedHashMap::new);

  private ExecutionRegistry() {
  }

  public static void registerInfo(String key, String value) {
    if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
      EXTRAS.get().put(key, value);
    }
  }

  public static Map<String, String> drainExtras() {
    Map<String, String> extras = new LinkedHashMap<>(EXTRAS.get());
    EXTRAS.get().clear();
    return extras;
  }

  public static void add(TestExecutionRecord record) {
    if (record != null) {
      RECORDS.add(record);
    }
  }

  public static List<TestExecutionRecord> snapshot() {
    synchronized (RECORDS) {
      return List.copyOf(RECORDS);
    }
  }

  public static Map<String, List<TestExecutionRecord>> snapshotBySuite() {
    Map<String, List<TestExecutionRecord>> bySuite = new LinkedHashMap<>();
    for (TestExecutionRecord record : snapshot()) {
      bySuite.computeIfAbsent(record.suite(), k -> new ArrayList<>()).add(record);
    }
    return bySuite;
  }

  public static boolean isEmpty() {
    return RECORDS.isEmpty();
  }

  public static void clear() {
    RECORDS.clear();
    EXTRAS.set(new HashMap<>());
  }
}
