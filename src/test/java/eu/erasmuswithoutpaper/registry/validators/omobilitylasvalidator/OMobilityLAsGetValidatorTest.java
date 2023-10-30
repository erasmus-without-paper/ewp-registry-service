package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator;


import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.get.OMobilityLAsGetValidator;
import eu.erasmuswithoutpaper.registry.validators.types.LearningAgreement;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;

public class OMobilityLAsGetValidatorTest extends OMobilityLAsValidatorTestBase {
  @Autowired
  protected OMobilityLAsGetValidator validator;

  @Override
  protected ApiValidator<OMobilityLAsSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilitylasGetUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingIMobilityTorsIdListIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoParams(RequestData requestData)
          throws ErrorResponseException {
        throw new ErrorResponseException(createOMobilityLAsGetResponse(new ArrayList<>()));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without sending_hei_id and omobility_ids, expect 400.");
  }

  @Test
  public void testNotReportingMissingHeiIdParameterAsAnErrorIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoSendingHeiId(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilityLAsGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without sending_hei_id, expect 400.");
  }

  @Test
  public void testIgnoringAdditionalHeiIdsIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoSendingHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.sendingHeiId = this.learningAgreements.get(0).getSendingHei().getHeiId();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without sending_hei_id, expect 400.");
  }

  @Test
  public void testNotReportingErrorWhenUnknownHeiIdIsPassedIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        requestData.sendingHeiId = this.learningAgreements.get(0).getSendingHei().getHeiId();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request for one of known omobility_ids with unknown sending_hei_id, expect 400."
    );
  }

  @Test
  public void testReturningWrongIMobilityTorIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean filterLAsForGet(LearningAgreement la, RequestData requestData) {
        return requestData.sendingHeiId.equals(la.getSendingHei().getHeiId())
            && !requestData.omobilityIds.contains(la.getOmobilityId());
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one known and one unknown omobility_id, expect 200 and only "
            + "one omobility in response.");
  }

  @Test
  public void testReturningDataForUnknownOMobilityIdIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean filterLAsForGet(LearningAgreement la, RequestData requestData) {
        return requestData.sendingHeiId.equals(la.getSendingHei().getHeiId());
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one unknown omobility_id, expect 200 and empty response.");
  }

  @Test
  public void testTooLargeMaxIMobilityTorsIdsInManifestIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(omobilitylasIndexUrl,
        omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
