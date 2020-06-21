package eu.erasmuswithoutpaper.registry.validators;

import org.assertj.core.api.AbstractAssert;

public class TestValidationReportAsset extends AbstractAssert<TestValidationReportAsset, TestValidationReport> {
  public TestValidationReportAsset(TestValidationReport actual) {
    super(actual, TestValidationReportAsset.class);
  }

  public static TestValidationReportAsset assertThat(TestValidationReport actual) {
    return new TestValidationReportAsset(actual);
  }

  public TestValidationReportAsset isCorrect() {
    isNotNull();
    if (actual.toString().isEmpty() || !actual.isCorrect()) {
      failWithMessage("Report is not correct. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset doesntContainNotice() {
    isNotNull();
    if (actual.containsNotice()) {
      failWithMessage("Report contains a NOTICE. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsNotice() {
    isNotNull();
    if (!actual.containsNotice()) {
      failWithMessage("Report doesn't contain a NOTICE. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsNotice(String partOfExpectedName) {
    isNotNull();
    if (!actual.containsNotice(partOfExpectedName)) {
      failWithMessage(
          "Report doesn't contain a NOTICE. Expected to contain NOTICE with name: <%s>, actual: <%s>",
          partOfExpectedName, actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset doesntContainWarning() {
    isNotNull();
    if (actual.containsWarning()) {
      failWithMessage("Report contains a WARNING. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsWarning() {
    isNotNull();
    if (!actual.containsWarning()) {
      failWithMessage("Report doesn't contain a WARNING. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsWarning(String partOfExpectedName) {
    isNotNull();
    if (!actual.containsWarning(partOfExpectedName)) {
      failWithMessage(
          "Report doesn't contain a WARNING. Expected to contain WARNING with name: <%s>, actual: <%s>",
          partOfExpectedName, actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset doesntContainFailure() {
    isNotNull();
    if (actual.containsFailure()) {
      failWithMessage("Report contains a FAILURE. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsFailure() {
    isNotNull();
    if (!actual.containsFailure()) {
      failWithMessage("Report doesn't contain a FAILURE. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsFailure(String partOfExpectedName) {
    isNotNull();
    if (!actual.containsFailure(partOfExpectedName)) {
      failWithMessage(
          "Report doesn't contain a FAILURE. Expected to contain FAILURE with name: <%s>, actual: <%s>",
          partOfExpectedName, actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset doesntContainError() {
    isNotNull();
    if (actual.containsError()) {
      failWithMessage("Report contains an ERROR. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsError() {
    isNotNull();
    if (!actual.containsError()) {
      failWithMessage("Report doesn't contain an ERROR. Actual: <%s>", actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsError(String partOfExpectedName) {
    isNotNull();
    if (!actual.containsError(partOfExpectedName)) {
      failWithMessage(
          "Report doesn't contain an ERROR. Expected to contain ERROR with name: <%s>, actual: <%s>",
          partOfExpectedName, actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsSkipped(String partOfExpectedName) {
    isNotNull();
    if (!actual.containsSkipped(partOfExpectedName)) {
      failWithMessage(
          "Report doesn't contain an SKIPPED. Expected to contain SKIPPED with"
              + " name: <%s>, actual: <%s>",
          partOfExpectedName, actual.toString());
    }
    return this;
  }

  public TestValidationReportAsset containsText(String partOfExpectedName) {
    isNotNull();
    if (!actual.containsText(partOfExpectedName)) {
      failWithMessage(
          "Report doesn't contain expected text. Expected: <%s>, actual: <%s>",
          partOfExpectedName, actual.toString());
    }
    return this;
  }
}
