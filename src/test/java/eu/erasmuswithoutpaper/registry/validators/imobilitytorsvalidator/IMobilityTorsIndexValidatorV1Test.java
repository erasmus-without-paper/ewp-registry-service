package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.index.IMobilityTorsIndexValidator;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;

public class IMobilityTorsIndexValidatorV1Test extends IMobilityTorsValidatorTestBaseV1 {
  @Autowired
  protected IMobilityTorsIndexValidator validator;

  @Override
  protected ApiValidator<IMobilityTorsSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilityTorsIndexUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotReturningAnErrorWhenNoParamsAreProvidedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorNoParams(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIMobilityTorsIndexResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without parameters, expect 400.");
  }

  @Test
  public void testNotReturningAnErrorWhenNoReceivingHeiIdIsUsedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorNoReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        requestData.receivingHeiId = "test";
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without receiving_hei_id and known sending_hei_id, expect 400.");
  }

  @Test
  public void testReturningAnErrorResponseWhenKnownReceivingHeiIdWithoutSendingHeiIdIsUsedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleNoSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "No sending_hei_id")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsNotice(
        "Request one known receiving_hei_id, expect 200 OK."
    );
  }

  @Test
  public void testReturningEmptyResponseWhenKnownReceivingHeiIdWithoutSendingHeiIdIsUsedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleNoSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIMobilityTorsIndexResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsNotice(
        "Request one known receiving_hei_id, expect 200 OK."
    );
  }

  @Test
  public void testIgnoringAdditionalReceivingHeiIdIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorMultipleReceivingHeiIds(
          RequestData requestData) throws ErrorResponseException {
        //ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known receiving_hei_id and unknown receiving_hei_id, expect 400.");
  }

  @Test
  public void testReturningAnErrorWhenUnknownReceivingHeiIdIsUsedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleUnknownReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown receiving_hei_id")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with unknown receiving_hei_id, expect 200 and empty response.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenUnknownReceivingHeiIdIsUsedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleUnknownReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIMobilityTorsIndexResponse(
                Collections.singletonList(tors.get(0).tor.getOmobilityId()))
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with unknown receiving_hei_id, expect 200 and empty response.");
  }

  @Test
  public void testReturningEmptyResponseWhenKnownAndUnknownSendingHeiIdsAreUsedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIMobilityTorsIndexResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with known receiving_hei_id and known and unknown " +
            "sending_hei_id, expect 200 and non-empty response.");
  }

  @Test
  public void testReturningAnErrorResponseWhenUnknownSendingHeiIdsAreUsedIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown receiving_hei_id")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with known receiving_hei_id and unknown sending_hei_id, " +
            "expect 200 and empty response.");
  }


  @Test
  public void testIgnoringMultipleModifiedSinceIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void errorMultipleModifiedSince(
          RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with multiple modified_since parameters, expect 400.");
  }

  @Test
  public void testNotUsingModifiedSinceIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected List<IMobilityTorEntry> filterIMobilityTorsByModifiedSince(
          List<IMobilityTorEntry> selectedIMobilityTors, RequestData requestData) {
        return selectedIMobilityTors;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Request with known receiving_hei_id and modified_since in the future, expect 200 OK "
            + "and empty response");
  }

  @Test
  public void testReturnsEmptyResponseWhenModifiedSinceIsUsed() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected List<IMobilityTorEntry> filterIMobilityTorsByModifiedSince(
          List<IMobilityTorEntry> selectedIMobilityTors, RequestData requestData) {
        if (requestData.modifiedSince != null) {
          return new ArrayList<>();
        }
        return selectedIMobilityTors;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known receiving_hei_id and modified_since far in the past, expect 200 OK "
            + "and non-empty response.");
  }

  @Test
  public void testNotAcceptingMultipleSendingHeiIdsIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected void handleMultipleSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "More that one sending_hei_id provided.")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known receiving_hei_id and two unknown sending_hei_id, expect 200 OK "
            + "and empty response.");
  }

  @Test
  public void testSendingDataThatCallerHasNoAccessToIsDetected() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected boolean isCallerPermittedToSeeSendingHeiId(
          RequestData requestData, String sendingHeiId) {
        return true;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Request with known receiving_hei_id and sending_hei_id valid but not covered by"
            + " the validator, expect empty response.");

  }

  @Test
  public void testSendingDataThatCallerHasNoAccessToIsDetected2() {
    IMobilityTorsServiceV1Valid service = new IMobilityTorsServiceV1Valid(
        omobilityTorsIndexUrl, omobilityTorsGetUrl, this.client) {
      @Override
      protected boolean isCallerPermittedToSeeSendingHeiId(
          RequestData requestData, String sendingHeiId) {
        return true;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known receiving_hei_id as other EWP participant, "
            + "expect 200 OK and empty response.");

  }
}

