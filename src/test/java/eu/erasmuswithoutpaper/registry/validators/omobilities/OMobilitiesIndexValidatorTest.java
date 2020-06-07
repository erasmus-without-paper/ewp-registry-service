package eu.erasmuswithoutpaper.registry.validators.omobilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index.OMobilitiesIndexValidator;

import org.springframework.beans.factory.annotation.Autowired;
import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;
import org.junit.Test;

public class OMobilitiesIndexValidatorTest extends OMobilitiesValidatorTestBase {
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
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotReturningAnErrorWhenNoParamsAreProvidedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoParams(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilitiesIndexResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without parameters, expect 400.");
  }

  @Test
  public void testNotReturningAnErrorWhenNoReceivingHeiIdIsUsedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        requestData.sendingHeiId = mobilities.get(0).sending_hei_id;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without sending_hei_id and known receiving_hei_id, expect 400.");
  }

  @Test
  public void testReturningAnErrorResponseWhenKnownReceivingHeiIdWithoutSendingHeiIdIsUsedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleNoReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "No receiving_hei_id")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known sending_hei_id, expect 200 OK."
    );
  }

  @Test
  public void testReturningEmptyResponseWhenKnownReceivingHeiIdWithoutSendingHeiIdIsUsedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleNoReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilitiesIndexResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known sending_hei_id, expect 200 OK."
    );
  }

  @Test
  public void testIgnoringAdditionalReceivingHeiIdIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorMultipleSendingHeiIds(
          RequestData requestData) throws ErrorResponseException {
        //ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known sending_hei_id and unknown sending_hei_id, expect 400.");
  }

  @Test
  public void testNotReturningAnErrorWhenUnknownReceivingHeiIdIsUsedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        // ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with unknown sending_hei_id, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenUnknownReceivingHeiIdIsUsedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilitiesIndexResponse(
                Collections.singletonList(mobilities.get(0).mobility.getOmobilityId()))
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with unknown sending_hei_id, expect 400.");
  }

  @Test
  public void testReturningEmptyResponseWhenKnownAndUnknownSendingHeiIdsAreUsedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilitiesIndexResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with known sending_hei_id and known and unknown " +
            "receiving_hei_id, expect 200 and non-empty response.");
  }

  @Test
  public void testReturningAnErrorResponseWhenUnknownSendingHeiIdsAreUsedIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
        .containsFailure("Request with known sending_hei_id and unknown receiving_hei_id, " +
            "expect 200 and empty response.");
  }


  @Test
  public void testIgnoringMultipleModifiedSinceIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<OMobilityEntry> filterOMobilitiesByModifiedSince(
          List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
        return selectedOMobilities;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Request with known sending_hei_id and modified_since in the future, expect 200 OK "
            + "and empty response");
  }

  @Test
  public void testReturnsEmptyResponseWhenModifiedSinceIsUsed() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
        "Request with known sending_hei_id and modified_since far in the past, expect 200 OK "
            + "and non-empty response.");
  }

  @Test
  public void testNotAcceptingMultipleSendingHeiIdsIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleMultipleReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400,
                "More that one receiving_hei_id provided.")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known sending_hei_id and two unknown receiving_hei_id, expect 200 OK "
            + "and empty response.");
  }

  @Test
  public void testSendingDataThatCallerHasNoAccessToIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean isCallerPermittedToSeeSendingHeiId(
          RequestData requestData, String receivingHeiId) {
        return true;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Request with known sending_hei_id and receiving_hei_id valid but not covered by"
            + " the validator, expect empty response.");

  }

  @Test
  public void testSendingDataThatCallerHasNoAccessToIsDetected2() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean isCallerPermittedToSeeSendingHeiId(
          RequestData requestData, String receivingHeiId) {
        return true;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known sending_hei_id as other EWP participant, "
            + "expect 200 OK and empty response.");

  }

  @Test
  public void testIncorrectFilteringWithReceivingAcademicYearIdIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
    assertThat(report).containsFailure(
        "Request with known sending_hei_id and known receiving_academic_year_id parameter, expect"
            + " 200 OK and non-empty response."
    );
  }

  @Test
  public void testIncorrectFilteringWithModifiedSinceIsDetected() {
    OMobilitiesServiceV1Valid service = new OMobilitiesServiceV1Valid(
        omobilitiesIndexUrl, omobilitiesGetUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
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
        "Request with known sending_hei_id and modified_since far in the past, "
            + "expect 200 OK and non-empty response."
        );
  }
}

