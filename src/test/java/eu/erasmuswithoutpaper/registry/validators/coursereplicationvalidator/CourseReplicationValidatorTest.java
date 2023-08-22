package eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator.CourseReplicationSuiteState;
import eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator.CourseReplicationValidator;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;

public class CourseReplicationValidatorTest extends AbstractApiTest<CourseReplicationSuiteState> {
  private static String courseReplicationUrlHTTT =
      "https://university.example.com/creplication/HTTT/";
  @Autowired
  private CourseReplicationValidator validator;
  private SemanticVersion version100 = new SemanticVersion(1, 0, 0);

  @Override
  protected SemanticVersion getVersion() {
    return version100;
  }

  @Override
  protected String getManifestFilename() {
    return "coursesvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return courseReplicationUrlHTTT;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        getUrl(), this.client, this.validatorKeyStoreSet.getMainKeyStore());
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotReportingErrorWhenNoParametersAreProvidedIsDetected() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        courseReplicationUrlHTTT, this.client, this.validatorKeyStoreSet.getMainKeyStore()) {
      @Override
      protected void errorNoParameters(RequestData requestData,
          Map<String, List<String>> params)
          throws ErrorResponseException {
        throw new ErrorResponseException(
            createCourseReplicationResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without any parameter, expect 400.");
  }

  @Test
  public void testReportingAnErrorWhenUnknownParametersArePassedIsDetected() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        courseReplicationUrlHTTT, this.client, this.validatorKeyStoreSet.getMainKeyStore()) {
      @Override
      protected void errorAdditionalParameters(RequestData requestData,
          Map<String, List<String>> params)
          throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown parameter")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with known hei_id and invalid parameter, expect 200.");
  }

  @Test
  public void testIgnoringAdditionalHeiIdsIsDetected() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        courseReplicationUrlHTTT, this.client, this.validatorKeyStoreSet.getMainKeyStore()) {
      @Override
      protected void errorMultipleHeiIds(RequestData requestData)
          throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with correct hei_id twice, expect 400.");
  }

  @Test
  public void testNotReportingUnknownHeiIdAsAnErrorIsDetected() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        courseReplicationUrlHTTT, this.client, this.validatorKeyStoreSet.getMainKeyStore()) {
      @Override
      protected List<String> processNotCoveredHei(RequestData requestData)
          throws ErrorResponseException {
        return new ArrayList<>();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with unknown hei_id parameter, expect 400.");
  }

  @Test
  public void testReturningCorrectResponseWhenUnknownHeiIdIsPassedIsDetected() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        courseReplicationUrlHTTT, this.client, this.validatorKeyStoreSet.getMainKeyStore()) {
      @Override
      protected List<String> processNotCoveredHei(RequestData requestData)
          throws ErrorResponseException {
        requestData.requestedHeiId = this.coveredHeiIds.get(0);
        return processCoveredHei(requestData);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with unknown hei_id parameter, expect 400.");
  }

  @Test
  public void testReturningInvalidLosIdsIsDetected() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        courseReplicationUrlHTTT, this.client, this.validatorKeyStoreSet.getMainKeyStore()) {
      @Override
      protected List<String> processCoveredHei(RequestData requestData)
          throws ErrorResponseException {
        return Arrays.asList("invalid-id", "invalid-invalid-id");
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with known hei_id, expect 200.");
  }

  @Test
  public void testAcceptingInvalidDatesIsDetected() {
    CourseReplicationServiceV1Valid service = new CourseReplicationServiceV1Valid(
        courseReplicationUrlHTTT, this.client, this.validatorKeyStoreSet.getMainKeyStore()) {
      protected void errorInvalidModifiedSince(RequestData requestData)
          throws ErrorResponseException {
        requestData.requestedModifiedSinceDate = null;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with invalid value of modified_since, expect 400.");
  }

  @Override
  protected ApiValidator<CourseReplicationSuiteState> getValidator() {
    return validator;
  }
}

