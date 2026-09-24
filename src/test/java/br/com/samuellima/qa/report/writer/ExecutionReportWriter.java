package br.com.samuellima.qa.report.writer;

import java.util.List;
import br.com.samuellima.qa.report.model.ExecutionSummary;
import br.com.samuellima.qa.report.model.TestExecutionRecord;

public interface ExecutionReportWriter {

  String name();

  void write(List<TestExecutionRecord> records, ExecutionSummary summary) throws Exception;
}
