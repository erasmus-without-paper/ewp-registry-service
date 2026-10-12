package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameters;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaIndexValidator;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Regression tests for user-supplied receiving_academic_year_id validation parameter.
 * Malformed values (e.g. "2025-2026") must not be blamed on the tested implementation:
 * the validator reports a failure about the parameter itself, and skips the tests which
 * depend on it.
 */
public class IiaIndexValidatorV7AcademicYearParameterTest extends IiaValidatorTestBase {

  private static final String KNOWN_YEAR_STEP_NAME = "known receiving_academic_year_id parameter";

  @Autowired
  protected IiaIndexValidator validator;

  @Override
  protected ApiValidator<IiaSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return iiaIndexUrl;
  }

  @Override
  protected String getManifestFilename() {
    return "iiasvalidator/manifest-v7.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(7, 0, 0);
  }

  private ValidationParameters paramsWithReceivingAcademicYear(String academicYearId) {
    ValidationParameters params = new ValidationParameters();
    params.put("receiving_academic_year_id", academicYearId);
    return params;
  }

  private List<ValidationStepWithStatus> runValidation(FakeInternetService service,
      ValidationParameters params) {
    this.internet.addFakeInternetService(service);
    List<ValidationStepWithStatus> steps =
        getValidationReportSteps(getUrl(), getVersion(), getSecurity(), params);
    this.internet.removeFakeInternetService(service);
    this.internet.clearAll();
    return steps;
  }

  @Test
  public void testMalformedReceivingAcademicYearParameterDoesNotFailImplementation() {
    IiasServiceValidV7 service = new IiasServiceValidV7(iiaIndexUrl, iiaGetUrl, this.client);
    List<ValidationStepWithStatus> steps = runValidation(service,
        paramsWithReceivingAcademicYear("2025-2026"));
    TestValidationReport report = new TestValidationReport(steps);

    assertThat(report)
        .containsFailure("Checking validation parameter receiving_academic_year_id.")
        .containsText("is not in YYYY/YYYY format")
        .containsSkipped(KNOWN_YEAR_STEP_NAME);

    boolean failedOnKnownYearTest = steps.stream().anyMatch(step ->
        step.getStatus() == Status.FAILURE && step.getName().contains(KNOWN_YEAR_STEP_NAME));
    assertFalse(failedOnKnownYearTest,
        "Known receiving_academic_year_id step must not FAIL when the validator parameter is"
            + " malformed; it should be skipped instead. Steps:\n" + report);
  }

  @Test
  public void testValidReceivingAcademicYearParameterStillRunsTest() {
    IiasServiceValidV7 service = new IiasServiceValidV7(iiaIndexUrl, iiaGetUrl, this.client);
    List<ValidationStepWithStatus> steps = runValidation(service,
        paramsWithReceivingAcademicYear("2018/2019"));
    TestValidationReport report = new TestValidationReport(steps);

    assertThat(report)
        .isCorrect()
        .doesntContainFailure();
    assertFalse(steps.stream().anyMatch(step ->
            step.getStatus() == Status.SKIPPED && step.getName().contains(KNOWN_YEAR_STEP_NAME)),
        "Known receiving_academic_year_id step must run when the parameter is well-formed."
            + " Steps:\n" + report);
  }
}
