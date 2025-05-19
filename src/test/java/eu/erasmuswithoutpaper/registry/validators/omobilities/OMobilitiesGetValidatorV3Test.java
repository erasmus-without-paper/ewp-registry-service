package eu.erasmuswithoutpaper.registry.validators.omobilities;


import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.get.OMobilitiesGetValidator;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;

public class OMobilitiesGetValidatorV3Test extends OMobilitiesValidatorTestBase {
  @Override
  protected String getManifestFilename() {
    return "omobilities/manifest-v3.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(3, 0, 0);
  }

  @Autowired
  protected OMobilitiesGetValidator validator;

  @Override
  protected ApiValidator<OMobilitiesSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilitiesGetUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingOMobilityIdsExceededIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorMaxOMobilityIdsExceeded(RequestData requestData)
          throws ErrorResponseException {
        //Do nothing
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-omobility-ids> known omobility_ids, expect 400.");
  }

  @Test
  public void testNotReportingMissingRequiredParametersAsAnErrorIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoParams(RequestData requestData)
          throws ErrorResponseException {
        throw new ErrorResponseException(createOMobilitiesGetResponse(new ArrayList<>()));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without parameters, expect 400.");
  }

  @Test
  public void testReturningWrongOMobilityIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean filterOMobilitiesForGet(OMobilityEntry mobility, RequestData requestData) {
        return !requestData.omobilityIds.contains(mobility.mobility.getOmobilityId());
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one known and one unknown omobility_id, expect 200 and only "
            + "one omobility in response.");
  }

  @Test
  public void testReturningDataForUnknownOMobilityIdIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean filterOMobilitiesForGet(OMobilityEntry mobility, RequestData requestData) {
        return true;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one unknown omobility_id, expect 200 and empty response.");
  }

  @Test
  public void testTooLargeMaxOMobilityIdsInManifestIsDetected() {
    OMobilitiesServiceV3Valid service = new OMobilitiesServiceV3Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected int getMaxOmobilityIds() {
        return super.getMaxOmobilityIds() - 1;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request exactly <max-omobility-ids> known omobility_ids, "
            + "expect 200 and non-empty response.");
  }
}
