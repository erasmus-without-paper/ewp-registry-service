package eu.erasmuswithoutpaper.registry.validators.omobilities;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index.OMobilitiesIndexValidator;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;

public class OMobilitiesIndexValidatorV3Test extends OMobilitiesValidatorTestBase {
  @Override
  protected String getManifestFilename() {
    return "omobilities/manifest-v3.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(3, 0, 0);
  }

  @Autowired
  protected OMobilitiesIndexValidator validator;

  @Override
  protected ApiValidator<OMobilitiesSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilitiesIndexUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testIgnoringMultipleModifiedSinceIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorMultipleModifiedSince(RequestData requestData)
          throws ErrorResponseException {
        // Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with multiple modified_since parameters, expect 400.");
  }

  @Test
  public void testNotUsingModifiedSinceIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<OMobilityEntry> filterOMobilitiesByModifiedSince(
          List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
        return selectedOMobilities;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Request with modified_since in the future, expect 200 OK and empty response");
  }

  @Test
  public void testReturnsEmptyResponseWhenModifiedSinceIsUsed() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<OMobilityEntry> filterOMobilitiesByModifiedSince(
          List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
        if (requestData.modifiedSince != null) {
          return new ArrayList<>();
        }
        return selectedOMobilities;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with modified_since far in the past, expect 200 OK and non-empty response.");
  }

  @Test
  public void testIncorrectFilteringWithReceivingAcademicYearIdIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<OMobilityEntry> filterOMobilitiesByReceivingAcademicYearId(
          List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
        if (requestData.receivingAcademicYearId == null) {
          return selectedOMobilities;
        }
        return new ArrayList<>();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with known receiving_academic_year_id parameter, expect"
            + " 200 OK and non-empty response.");
  }

  @Test
  public void testIncorrectFilteringWithModifiedSinceIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<OMobilityEntry> filterOMobilitiesByModifiedSince(
          List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
        if (requestData.modifiedSince == null) {
          return selectedOMobilities;
        }
        return new ArrayList<>();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with modified_since far in the past, expect 200 OK and non-empty response.");
  }
}

