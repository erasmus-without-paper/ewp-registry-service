package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.index.OMobilityLAsIndexValidator;

import org.springframework.beans.factory.annotation.Autowired;

import https.github_com.erasmus_without_paper.ewp_specs_api_omobility_las.blob.stable_v1.endpoints.get_response.LearningAgreement;
import org.junit.Test;

public class OMobilityLAsIndexValidatorTest extends OMobilityLAsValidatorTestBase {
  @Autowired
  protected OMobilityLAsIndexValidator validator;

  @Override
  protected ApiValidator<OMobilityLAsSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilitylasIndexUrl;
  }


  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotReturningAnErrorWhenNoParamsAreProvidedIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoParams(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilityLAsIndexResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without parameters, expect 400.");
  }

  @Test
  public void testNotReturningAnErrorWhenNoReceivingHeiIdIsUsedIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void errorNoSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        requestData.sendingHeiId = learningAgreements.get(0).getSendingHei().getHeiId();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without sending_hei_id and known receiving_hei_id, expect 400.");
  }

  @Test
  public void testReturningAnErrorResponseWhenKnownReceivingHeiIdWithoutSendingHeiIdIsUsedIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleNoReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilityLAsIndexResponse(new ArrayList<>())
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownSendingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilityLAsIndexResponse(
                Collections.singletonList(learningAgreements.get(0).getOmobilityId()))
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with unknown sending_hei_id, expect 400.");
  }

  @Test
  public void testReturningEmptyResponseWhenKnownAndUnknownSendingHeiIdsAreUsedIsDetected() {
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected void handleUnknownReceivingHeiId(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createOMobilityLAsIndexResponse(new ArrayList<>())
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<LearningAgreement> filterLAsByModifiedSince(
          List<LearningAgreement> selectedOMobilities, RequestData requestData) {
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<LearningAgreement> filterLAsByModifiedSince(
          List<LearningAgreement> selectedOMobilities, RequestData requestData) {
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean isCalledPermittedToSeeReceivingHeiIdsData(
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected boolean isCalledPermittedToSeeReceivingHeiIdsData(
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<LearningAgreement> filterLAsByReceivingAcademicYearId(
          List<LearningAgreement> selectedOMobilities, RequestData requestData) {
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
    OMobilityLAsServiceV1Valid service = new OMobilityLAsServiceV1Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0)) {
      @Override
      protected List<LearningAgreement> filterLAsByModifiedSince(
          List<LearningAgreement> selectedOMobilities, RequestData requestData) {
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

