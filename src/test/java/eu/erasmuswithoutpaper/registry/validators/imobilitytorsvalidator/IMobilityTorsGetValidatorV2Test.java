package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;


import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get.IMobilityTorsGetValidator;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;

public class IMobilityTorsGetValidatorV2Test extends IMobilityTorsValidatorTestBaseV2 {
  @Autowired
  protected IMobilityTorsGetValidator validator;

  @Override
  protected ApiValidator<IMobilityTorsSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilityTorsGetUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingIMobilityTorsIdListIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
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
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorNoParams(RequestData requestData)
          throws ErrorResponseException {
        throw new ErrorResponseException(createIMobilityTorsGetResponse(new ArrayList<>()));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without receiving_hei_id and omobility_ids, expect 400.");
  }

  @Test
  public void testNotReportingMissingHeiIdParameterAsAnErrorIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorNoReceivingHeiId(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIMobilityTorsGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without receiving_hei_id, expect 400.");
  }

  @Test
  public void testIgnoringAdditionalHeiIdsIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorMultipleReceivingHeiIds(
          RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct receiving_hei_id twice, expect 400.");
  }

  @Test
  public void testReturningCorrectDataWhenNoHeiIdIsPassedIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorNoReceivingHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.receivingHeiId = this.tors.get(0).receiving_hei_id;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without receiving_hei_id, expect 400.");
  }

  @Test
  public void testReportingErrorWhenUnknownHeiIdIsPassedIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleUnknownReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown receiving_hei_id")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request for one of known omobility_ids with unknown receiving_hei_id, expect 200 "
            + "and empty response.");
  }

  @Test
  public void testNonEmptyResponseWhenUnknownReceivingHeiIdIsPassedIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleUnknownReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        requestData.receivingHeiId = this.tors.get(0).receiving_hei_id;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request for one of known omobility_ids with unknown receiving_hei_id, expect 200 "
            + "and empty response.");
  }

  @Test
  public void testReturningWrongIMobilityTorIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected boolean filterIMoblityTorForGet(IMobilityTorEntry tor, RequestData requestData) {
        return requestData.receivingHeiId.equals(tor.receiving_hei_id)
            && !requestData.omobilityIds.contains(tor.tor.getOmobilityId());
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one known and one unknown omobility_id, expect 200 and only "
            + "one omobility in response.");
  }

  @Test
  public void testReturningDataForUnknownOMobilityIdIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
      @Override
      protected boolean filterIMoblityTorForGet(IMobilityTorEntry tor, RequestData requestData) {
        return requestData.receivingHeiId.equals(tor.receiving_hei_id);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one unknown omobility_id, expect 200 and empty response.");
  }

  @Test
  public void testTooLargeMaxIMobilityTorsIdsInManifestIsDetected() {
    IMobilityTorsServiceV2Valid service = new IMobilityTorsServiceV2Valid(omobilityTorsIndexUrl,
        omobilityTorsGetUrl, this.client) {
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
