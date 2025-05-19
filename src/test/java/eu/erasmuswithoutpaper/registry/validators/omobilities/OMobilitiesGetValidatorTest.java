package eu.erasmuswithoutpaper.registry.validators.omobilities;


import java.util.ArrayList;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.get.OMobilitiesGetValidator;

import org.springframework.beans.factory.annotation.Autowired;
import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;
import org.junit.jupiter.api.Test;

public class OMobilitiesGetValidatorTest extends OMobilitiesValidatorTestBase {
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
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingOMobilityIdsExceededIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
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
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoParams(RequestData requestData)
          throws ErrorResponseException {
        throw new ErrorResponseException(createOMobilitiesGetResponse(new ArrayList<>()));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without sending_hei_id and omobility_ids, expect 400.");
  }

  @Test
  public void testNotReportingMissingHeiIdParameterAsAnErrorIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoSendingHeiId(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilitiesGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without sending_hei_id, expect 400.");
  }

  @Test
  public void testIgnoringAdditionalHeiIdsIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorMultipleSendingHeiIds(
          RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct sending_hei_id twice, expect 400.");
  }

  @Test
  public void testReturningCorrectDataWhenNoHeiIdIsPassedIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoSendingHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.sendingHeiId = this.mobilities.get(0).sending_hei_id;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without sending_hei_id, expect 400.");
  }

  @Test
  public void testNotReportingErrorWhenUnknownHeiIdIsPassedIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        // ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request for one of known omobility_ids with unknown sending_hei_id, expect 400."
    );
  }

  @Test
  public void testCorrectResponseWhenUnknownReceivingHeiIdIsPassedIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        requestData.sendingHeiId = this.mobilities.get(0).sending_hei_id;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request for one of known omobility_ids with unknown sending_hei_id, expect 400."
    );
  }

  @Test
  public void testReturningWrongOMobilityIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean filterOMobilitiesForGet(OMobilityEntry mobility, RequestData requestData) {
        return requestData.sendingHeiId.equals(mobility.sending_hei_id)
            && !requestData.omobilityIds.contains(mobility.mobility.getOmobilityId());
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one known and one unknown omobility_id, expect 200 and only "
            + "one omobility in response.");
  }

  @Test
  public void testReturningDataForUnknownOMobilityIdIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
        omobilitiesGetUrl, this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean filterOMobilitiesForGet(OMobilityEntry mobility, RequestData requestData) {
        return requestData.sendingHeiId.equals(mobility.sending_hei_id);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one unknown omobility_id, expect 200 and empty response.");
  }

  @Test
  public void testTooLargeMaxOMobilityIdsInManifestIsDetected() {
    OMobilitiesServiceV2Valid service = new OMobilitiesServiceV2Valid(omobilitiesIndexUrl,
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
