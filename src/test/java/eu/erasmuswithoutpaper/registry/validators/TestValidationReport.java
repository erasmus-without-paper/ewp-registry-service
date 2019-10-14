package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class TestValidationReport {
  private static final String ANY_MESSAGE = "";
  private final List<ValidationStepWithStatus> steps;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (ValidationStepWithStatus result : this.steps) {
      if (!result.getStatus().equals(ValidationStepWithStatus.Status.SUCCESS)) {
        sb.append('\n');
      }
      addHeader(sb, result);
      addErrorMessage(sb, result);
    }
    return sb.toString();
  }

  private void addErrorMessage(StringBuilder sb, ValidationStepWithStatus result) {
    if (!result.getStatus().equals(ValidationStepWithStatus.Status.SUCCESS)) {
      sb.append('\n');
      sb.append(result.getMessage()).append('\n');
      if (result.getServerDeveloperErrorMessage().isPresent()) {
        sb.append(result.getServerDeveloperErrorMessage().get()).append('\n');
      }
      sb.append("\n\n");
    }
  }

  private void addHeader(StringBuilder sb, ValidationStepWithStatus result) {
    sb.append("### ")
        .append(result.getStatus())
        .append(": ")
        .append(result.getName())
        .append('\n');
  }


  private boolean isFreshCredentialsNotice(ValidationStepWithStatus step) {
    return matchStep(step, ValidationStepWithStatus.Status.NOTICE,
        "Check if our client credentials have been served long enough.");
  }

  public TestValidationReport(List<ValidationStepWithStatus> steps) {
    this(steps, true);
  }

  public TestValidationReport(List<ValidationStepWithStatus> steps,
      boolean removeFreshCredentialsNotice) {
    this.steps = steps;
    // NOTICE about fresh credentials, it's always included in reports from tests because
    // credentials are generated when test starts. It obscures the tests a bit therefore we
    // remove it.
    if (removeFreshCredentialsNotice) {
      this.steps.removeIf(this::isFreshCredentialsNotice);
    }
  }

  public boolean isCorrect() {
    return !containsError() && !containsFailure() && !containsWarning() && !containsNotice();
  }

  public boolean containsNotice() {
    return containsNotice(ANY_MESSAGE);
  }

  public boolean containsNotice(String partOfExpectedName) {
    return contains(ValidationStepWithStatus.Status.NOTICE, partOfExpectedName);
  }

  public boolean containsWarning() {
    return containsWarning(ANY_MESSAGE);
  }

  public boolean containsWarning(String partOfExpectedName) {
    return contains(ValidationStepWithStatus.Status.WARNING, partOfExpectedName);
  }

  public boolean containsFailure() {
    return containsFailure(ANY_MESSAGE);
  }

  public boolean containsFailure(String partOfExpectedName) {
    return contains(ValidationStepWithStatus.Status.FAILURE, partOfExpectedName);
  }

  public boolean containsError(String partOfExpectedName) {
    return contains(ValidationStepWithStatus.Status.ERROR, partOfExpectedName);
  }

  public boolean containsError() {
    return containsError(ANY_MESSAGE);
  }

  public boolean containsText(String partOfExpectedName) {
    return steps.stream().anyMatch(step -> step.getMessage().contains(partOfExpectedName));
  }

  private boolean contains(ValidationStepWithStatus.Status status, String partOfExpectedName) {
    return steps.stream().anyMatch(step -> this.matchStep(step, status, partOfExpectedName));
  }

  private boolean matchStep(ValidationStepWithStatus step, ValidationStepWithStatus.Status status,
      String partOfExpectedName) {
    return step.getStatus() == status && step.getName().contains(partOfExpectedName);
  }
}



