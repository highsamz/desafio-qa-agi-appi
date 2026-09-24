package br.com.samuellima.qa.report.model;

public enum ExecutionStatus {

  PASSED("PASSOU", "✅"),
  FAILED("FALHOU", "❌"),
  ABORTED("ABORTADO", "⚠️"),
  SKIPPED("IGNORADO", "⏭️");

  private final String label;
  private final String icon;

  ExecutionStatus(String label, String icon) {
    this.label = label;
    this.icon = icon;
  }

  public String label() {
    return label;
  }

  public String icon() {
    return icon;
  }

  public boolean isFailure() {
    return this == FAILED || this == ABORTED;
  }
}
